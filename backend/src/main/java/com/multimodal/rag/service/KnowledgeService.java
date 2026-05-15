package com.multimodal.rag.service;

import com.multimodal.rag.client.PythonEmbeddingClient;
import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeService {

    private static final String UPLOAD_DIR = "uploads";

    private final MultimodalDocumentRepository repository;
    private final DocumentParserService parserService;
    private final VectorStoreService vectorStoreService;
    private final StatisticsService statisticsService;
    private final MultimodalEmbeddingService multimodalService;
    private final TextChunkingService chunkingService;
    private final OssStorageService ossStorageService;

    public record ReindexFailure(Long documentId, String fileName, String error) {
    }

    public record ReindexSummary(int total, int succeeded, int failed, List<ReindexFailure> failures) {
    }

    public MultimodalDocument uploadDocument(MultipartFile file, User user) throws IOException {
        Path root = ensureUploadRoot();
        String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename());
        String storedFileName = System.currentTimeMillis() + "_" + originalFilename;
        Path targetPath = root.resolve(storedFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String storagePath = storeFile(targetPath, user, storedFileName, file.getSize());
        MultimodalDocument doc = MultimodalDocument.builder()
                .fileName(originalFilename)
                .fileType(getFileExtension(originalFilename))
                .filePath(storagePath)
                .fileSize(file.getSize())
                .status("PROCESSING")
                .user(user)
                .build();

        doc = repository.save(doc);
        return processAndPersistDocument(doc, user, targetPath, originalFilename, file.getContentType());
    }

    public MultimodalDocument reindexDocument(Long id, User user) throws IOException {
        MultimodalDocument doc = loadDocumentForMutation(id, user);
        return reindexExistingDocument(doc, user);
    }

    public ReindexSummary reindexOwnedDocuments(User user) {
        List<MultimodalDocument> documents = repository.findByUser_Id(user.getId());
        List<ReindexFailure> failures = new ArrayList<>();
        int succeeded = 0;

        for (MultimodalDocument doc : documents) {
            try {
                reindexExistingDocument(doc, user);
                succeeded++;
            } catch (Exception e) {
                String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                failures.add(new ReindexFailure(doc.getId(), doc.getFileName(), message));
                log.error("Failed to reindex document {} ({})", doc.getId(), doc.getFileName(), e);
            }
        }

        return new ReindexSummary(documents.size(), succeeded, failures.size(), failures);
    }

    @Transactional
    public MultimodalDocument togglePublicStatus(Long id, User user) {
        MultimodalDocument doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        Long ownerId = doc.getUser() != null ? doc.getUser().getId() : null;
        if (!Objects.equals(ownerId, user.getId()) && !isAdmin(user)) {
            throw new RuntimeException("Unauthorized to toggle public status for this document");
        }

        doc.setShared(!doc.isShared());
        return repository.save(doc);
    }

    public List<MultimodalDocument> getAllDocuments(User user) {
        if (isAdmin(user)) {
            return repository.findAll();
        }
        return repository.findByUser_IdOrSharedTrue(user.getId());
    }

    public void deleteDocument(Long id, User user) throws IOException {
        MultimodalDocument doc = loadDocumentForMutation(id, user);

        try {
            statisticsService.logDocumentAccess(id, null,
                    com.multimodal.rag.model.DocumentAccessLog.AccessType.DELETE);
        } catch (Exception e) {
            log.warn("Failed to log document access", e);
        }

        deleteStoredFile(doc);

        try {
            vectorStoreService.deleteByDocumentId(id);
        } catch (Exception e) {
            log.warn("Vector deletion failed for document {}, continuing with MySQL deletion", id);
        }

        repository.delete(doc);
        log.info("Deleted document with ID: {}", id);
    }

    public Path getFilePath(Long id) {
        try {
            statisticsService.logDocumentAccess(id, null,
                    com.multimodal.rag.model.DocumentAccessLog.AccessType.VIEW);
        } catch (Exception e) {
            log.warn("Failed to log document access", e);
        }

        MultimodalDocument doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return Paths.get(doc.getFilePath());
    }

    public String getStoragePath(Long id) {
        try {
            statisticsService.logDocumentAccess(id, null,
                    com.multimodal.rag.model.DocumentAccessLog.AccessType.VIEW);
        } catch (Exception e) {
            log.warn("Failed to log document access", e);
        }

        MultimodalDocument doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return doc.getFilePath();
    }

    private MultimodalDocument reindexExistingDocument(MultimodalDocument doc, User actingUser) throws IOException {
        User metadataOwner = doc.getUser() != null ? doc.getUser() : actingUser;
        log.info("Reindexing document {} ({})", doc.getId(), doc.getFileName());

        try (MaterializedDocumentSource source = materializeDocumentSource(doc)) {
            try {
                vectorStoreService.deleteByDocumentId(doc.getId());
            } catch (Exception e) {
                log.warn("Vector deletion failed before reindex for document {}: {}", doc.getId(), e.getMessage());
            }

            doc.setStatus("PROCESSING");
            doc.setExtractedContent(null);
            repository.save(doc);

            return processAndPersistDocument(doc, metadataOwner, source.path(), doc.getFileName(), null);
        }
    }

    private MultimodalDocument processAndPersistDocument(
            MultimodalDocument doc,
            User metadataOwner,
            Path sourcePath,
            String originalFilename,
            String contentType
    ) throws IOException {
        try {
            processDocument(doc, metadataOwner, sourcePath, originalFilename, contentType);
            doc.setStatus("COMPLETED");
            return repository.save(doc);
        } catch (Exception e) {
            log.error("Failed to process document: {}", originalFilename, e);
            doc.setStatus("FAILED");
            repository.save(doc);
            throw e;
        }
    }

    private void processDocument(
            MultimodalDocument doc,
            User metadataOwner,
            Path sourcePath,
            String originalFilename,
            String contentType
    ) throws IOException {
        String modality = multimodalService.detectModality(originalFilename);
        log.info("Processing document {} with modality: {}", originalFilename, modality);

        if (!"TEXT".equals(modality) && !multimodalService.isPythonServiceAvailable()) {
            log.warn("Python service not available, falling back to text extraction");
            modality = "TEXT";
        }

        switch (modality) {
            case "AUDIO" -> processAudioDocument(doc, metadataOwner, sourcePath, originalFilename);
            case "IMAGE", "VIDEO" -> processVisualDocument(doc, metadataOwner, sourcePath, originalFilename, modality);
            case "TEXT", "UNKNOWN" -> processTextDocument(doc, metadataOwner, sourcePath, originalFilename, contentType);
            default -> processTextDocument(doc, metadataOwner, sourcePath, originalFilename, contentType);
        }
    }

    private void processTextDocument(
            MultimodalDocument doc,
            User user,
            Path sourcePath,
            String originalFilename,
            String contentType
    ) throws IOException {
        String content = parserService.parseDocument(sourcePath, originalFilename, contentType);
        doc.setExtractedContent(content);
        addChunkedText(doc, user, "TEXT", content, "document_text");
    }

    private void processAudioDocument(
            MultimodalDocument doc,
            User user,
            Path sourcePath,
            String originalFilename
    ) throws IOException {
        byte[] fileBytes = Files.readAllBytes(sourcePath);
        String transcript = parserService.normalizeText(multimodalService.transcribeAudio(fileBytes, originalFilename));
        String extractedContent = transcript.isBlank()
                ? "No transcript extracted."
                : "Audio transcript\n\n" + transcript;
        doc.setExtractedContent(extractedContent);
        addChunkedText(doc, user, "AUDIO", transcript, "audio_transcript");
    }

    private void processVisualDocument(
            MultimodalDocument doc,
            User user,
            Path sourcePath,
            String originalFilename,
            String modality
    ) throws IOException {
        byte[] fileBytes = Files.readAllBytes(sourcePath);
        PythonEmbeddingClient.EmbeddingResponse response =
                multimodalService.embedByModality(fileBytes, originalFilename, modality);

        String transcript = parserService.normalizeText(response.getText());
        List<String> tags = sanitizeTags(response.getTags());
        String summary = buildMediaSummary(modality, doc.getFileName(), transcript, tags);
        doc.setExtractedContent(summary);

        addStandaloneChunk(doc, user, modality, "media_summary", "", summary);

        if (!transcript.isBlank()) {
            addChunkedText(doc, user, modality, transcript, "VIDEO".equals(modality) ? "video_transcript" : "image_ocr");
        }
    }

    private void addChunkedText(
            MultimodalDocument doc,
            User user,
            String modality,
            String content,
            String contentType
    ) {
        String normalized = parserService.normalizeText(content);
        if (normalized.isBlank()) {
            log.warn("No extracted content available for document {}", doc.getId());
            return;
        }

        List<TextChunkingService.TextChunk> chunks = chunkingService.chunkDocument(normalized);
        if (chunks.isEmpty()) {
            addStandaloneChunk(doc, user, modality, contentType, "", normalized);
            return;
        }

        for (int i = 0; i < chunks.size(); i++) {
            TextChunkingService.TextChunk chunk = chunks.get(i);
            Map<String, Object> metadata = buildBaseMetadata(doc, user, modality);
            metadata.put("contentType", contentType);
            metadata.put("chunkIndex", i);
            metadata.put("totalChunks", chunks.size());
            if (!chunk.headingPath().isBlank()) {
                metadata.put("headingPath", chunk.headingPath());
            }

            String searchableText = buildSearchableChunk(doc, modality, contentType, chunk.headingPath(), chunk.content());
            vectorStoreService.addDocument(searchableText, metadata);
        }
    }

    private void addStandaloneChunk(
            MultimodalDocument doc,
            User user,
            String modality,
            String contentType,
            String headingPath,
            String content
    ) {
        String normalized = parserService.normalizeText(content);
        if (normalized.isBlank()) {
            return;
        }

        Map<String, Object> metadata = buildBaseMetadata(doc, user, modality);
        metadata.put("contentType", contentType);
        if (!headingPath.isBlank()) {
            metadata.put("headingPath", headingPath);
        }

        String searchableText = buildSearchableChunk(doc, modality, contentType, headingPath, normalized);
        vectorStoreService.addDocument(searchableText, metadata);
    }

    private Map<String, Object> buildBaseMetadata(MultimodalDocument doc, User user, String modality) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("documentId", doc.getId());
        metadata.put("userId", user.getId());
        metadata.put("fileName", doc.getFileName());
        metadata.put("fileType", doc.getFileType());
        metadata.put("modality", modality);
        return metadata;
    }

    private String buildSearchableChunk(
            MultimodalDocument doc,
            String modality,
            String contentType,
            String headingPath,
            String content
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("File name: ").append(doc.getFileName()).append('\n');
        builder.append("File type: ").append(doc.getFileType()).append('\n');
        builder.append("Modality: ").append(modality).append('\n');
        builder.append("Content type: ").append(contentType).append('\n');
        if (headingPath != null && !headingPath.isBlank()) {
            builder.append("Section: ").append(headingPath).append('\n');
        }
        builder.append("Content:\n").append(content.trim());
        return builder.toString();
    }

    private String buildMediaSummary(String modality, String fileName, String transcript, List<String> tags) {
        List<String> parts = new ArrayList<>();
        parts.add("File name: " + fileName);
        parts.add("Modality: " + modality);

        if (!tags.isEmpty()) {
            parts.add("Tags: " + String.join(", ", tags));
        }
        if (!transcript.isBlank()) {
            parts.add("Transcript:\n" + transcript);
        }
        if (parts.size() == 2) {
            parts.add("No structured summary could be extracted.");
        }
        return String.join("\n\n", parts);
    }

    private List<String> sanitizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String tag : tags) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values.stream().limit(8).toList();
    }

    private MultimodalDocument loadDocumentForMutation(Long id, User user) {
        if (isAdmin(user)) {
            return repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Document not found"));
        }
        return repository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .map(role -> role.getName())
                .filter(Objects::nonNull)
                .anyMatch(name -> "ADMIN".equalsIgnoreCase(name) || "ROLE_ADMIN".equalsIgnoreCase(name));
    }

    private Path ensureUploadRoot() throws IOException {
        Path root = Paths.get(UPLOAD_DIR);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        return root;
    }

    private String storeFile(Path targetPath, User user, String storedFileName, long fileSize) {
        if (!ossStorageService.isAvailable()) {
            String storagePath = targetPath.toString();
            log.info("OSS not available, using local storage: {}", storagePath);
            return storagePath;
        }

        String ossKey = "documents/" + user.getId() + "/" + storedFileName;
        try (InputStream inputStream = Files.newInputStream(targetPath)) {
            ossStorageService.uploadFile(ossKey, inputStream, fileSize);
            log.info("File uploaded to OSS: {}", ossKey);
            return "oss://" + ossKey;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document to OSS", e);
        }
    }

    private void deleteStoredFile(MultimodalDocument doc) throws IOException {
        String pathStr = doc.getFilePath();
        if (pathStr == null || pathStr.isBlank()) {
            return;
        }

        try {
            if (pathStr.startsWith("oss://")) {
                String ossKey = pathStr.substring(6);
                ossStorageService.deleteFile(ossKey);
            } else {
                Path filePath = Paths.get(pathStr);
                Files.deleteIfExists(filePath);
            }
        } catch (Exception e) {
            log.warn("Could not delete file for document {}: {}", doc.getId(), e.getMessage());
        }
    }

    private MaterializedDocumentSource materializeDocumentSource(MultimodalDocument doc) throws IOException {
        String storagePath = doc.getFilePath();
        if (storagePath == null || storagePath.isBlank()) {
            throw new IOException("Document has no stored file path: " + doc.getId());
        }

        if (!storagePath.startsWith("oss://")) {
            Path localPath = Paths.get(storagePath);
            if (!Files.exists(localPath)) {
                throw new IOException("Stored file not found: " + localPath);
            }
            return new MaterializedDocumentSource(localPath, false);
        }

        Path root = ensureUploadRoot();
        Path tempPath = Files.createTempFile(root, "reindex-", buildTempSuffix(doc.getFileName()));
        String ossKey = storagePath.substring(6);
        try (InputStream inputStream = ossStorageService.getFileStream(ossKey)) {
            Files.copy(inputStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            Files.deleteIfExists(tempPath);
            throw new IOException("Failed to download file from OSS for reindex: " + doc.getFileName(), e);
        }
        return new MaterializedDocumentSource(tempPath, true);
    }

    private String sanitizeOriginalFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-file";
        }
        return Paths.get(originalFilename).getFileName().toString();
    }

    private String buildTempSuffix(String fileName) {
        String extension = getFileExtension(fileName);
        if ("unknown".equals(extension)) {
            return ".tmp";
        }
        return "." + extension;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private record MaterializedDocumentSource(Path path, boolean deleteOnClose) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            if (deleteOnClose) {
                Files.deleteIfExists(path);
            }
        }
    }
}
