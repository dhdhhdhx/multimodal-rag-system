package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.client.PythonEmbeddingClient;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.multimodal.rag.model.Role;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeService {

    private final MultimodalDocumentRepository repository;
    private final DocumentParserService parserService;
    private final VectorStoreService vectorStoreService;
    private final StatisticsService statisticsService;
    private final MultimodalEmbeddingService multimodalService;
    private final TextChunkingService chunkingService;
    private final OssStorageService ossStorageService;

    private static final String UPLOAD_DIR = "uploads";

    private String buildMediaSummary(String modality, String fileName, String summary, String transcript, List<String> tags) {
        List<String> sections = new ArrayList<>();
        sections.add("[" + ("IMAGE".equals(modality) ? "图片内容" : "VIDEO".equals(modality) ? "视频内容" : "多媒体内容") + "]");
        sections.add("文件名：" + fileName);

        if (summary != null && !summary.isBlank()) {
            sections.add(summary);
        } else if (tags != null && !tags.isEmpty()) {
            sections.add(("IMAGE".equals(modality) ? "图片要点：" : "视频画面要点：") + String.join("、", tags));
        }

        if (transcript != null && !transcript.isBlank()) {
            sections.add("语音转写：\n" + transcript);
        }

        return String.join("\n\n", sections);
    }

    public MultimodalDocument uploadDocument(MultipartFile file, User user) throws IOException {
        // Step 1: Save file locally for processing (temp)
        Path root = Paths.get(UPLOAD_DIR);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path targetPath = root.resolve(fileName);
        Files.copy(file.getInputStream(), targetPath);

        // Step 1b: Upload to OSS if available
        String storagePath;
        if (ossStorageService.isAvailable()) {
            String ossKey = "documents/" + user.getId() + "/" + fileName;
            try (var fis = Files.newInputStream(targetPath)) {
                ossStorageService.uploadFile(ossKey, fis, file.getSize());
            }
            storagePath = "oss://" + ossKey;
            log.info("File uploaded to OSS: {}", ossKey);
        } else {
            storagePath = targetPath.toString();
            log.info("OSS not available, using local storage: {}", storagePath);
        }

        // Step 2: Persist metadata
        MultimodalDocument doc = new MultimodalDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(getFileExtension(file.getOriginalFilename()));
        doc.setFilePath(storagePath);
        doc.setFileSize(file.getSize());
        doc.setStatus("PROCESSING");
        doc.setUser(user);

        doc = repository.save(doc);

        // Step 3: Detect modality and process accordingly
        try {
            String modality = multimodalService.detectModality(file.getOriginalFilename());
            log.info("Processing document {} with modality: {}", file.getOriginalFilename(), modality);
            
            // Check if Python service is available for non-text files
            if (!"TEXT".equals(modality) && !multimodalService.isPythonServiceAvailable()) {
                log.warn("Python service not available, falling back to text extraction");
                modality = "TEXT";
            }
            
            String content;
            
            if ("TEXT".equals(modality)) {
                // Traditional text processing with chunking
                log.info("Starting TEXT processing for file: {}, content-type: {}", file.getOriginalFilename(), file.getContentType());
                content = parserService.parseDocument(targetPath, file.getOriginalFilename(), file.getContentType());
                int contentLen = content != null ? content.length() : 0;
                log.info("Extracted content length: {} chars for file: {}", contentLen, file.getOriginalFilename());

                if (contentLen == 0) {
                    log.error("No content extracted from file: {}, setting status to FAILED", file.getOriginalFilename());
                    doc.setStatus("FAILED");
                    doc.setExtractedContent("无法提取文件内容，请检查文件格式是否正确");
                    repository.save(doc);
                    throw new RuntimeException("Failed to extract content from file: " + file.getOriginalFilename());
                }

                doc.setExtractedContent(content.length() > 5000 ? content.substring(0, 5000) + "..." : content);

                // Split into chunks for better retrieval
                List<String> chunks = chunkingService.chunkText(content);
                log.info("Document split into {} chunks", chunks.size());

                Map<String, Object> baseMeta = Map.of(
                    "documentId", doc.getId(),
                    "userId", user.getId(),
                    "fileName", doc.getFileName(),
                    "fileType", doc.getFileType(),
                    "modality", modality
                );

                for (int i = 0; i < chunks.size(); i++) {
                    String chunk = chunks.get(i);
                    Map<String, Object> chunkMeta = new java.util.HashMap<>(baseMeta);
                    chunkMeta.put("chunkIndex", i);
                    chunkMeta.put("totalChunks", chunks.size());

                    if (multimodalService.isPythonServiceAvailable()) {
                        PythonEmbeddingClient.EmbeddingResponse response = multimodalService.embedByModality(chunk.getBytes(), file.getOriginalFilename(), "TEXT");
                        vectorStoreService.addDocumentWithEmbedding(response.getEmbedding(), chunk, chunkMeta);
                    } else {
                        vectorStoreService.addDocument(chunk, chunkMeta);
                    }
                }
            } else {
                // Multimodal processing (IMAGE, AUDIO, VIDEO)
                // Read from saved file on disk instead of loading all bytes into memory
                byte[] fileBytes;
                long fileSize = Files.size(targetPath);

                // For large files (>50MB), use file-path-based processing
                if (fileSize > 50 * 1024 * 1024) {
                    log.info("Large file detected ({} MB), reading from disk: {}", fileSize / (1024 * 1024), targetPath);
                    fileBytes = Files.readAllBytes(targetPath);
                } else {
                    fileBytes = Files.readAllBytes(targetPath);
                }
                
                // For audio, also get transcription
                if ("AUDIO".equals(modality)) {
                    content = multimodalService.transcribeAudio(fileBytes, file.getOriginalFilename());
                    String audioContent = "[音频转写]\n" + content;
                    doc.setExtractedContent(audioContent.length() > 5000 ? audioContent.substring(0, 5000) + "..." : audioContent);

                    // Chunk the transcription for better retrieval
                    List<String> audioChunks = chunkingService.chunkText(content);
                    Map<String, Object> baseMeta = Map.of(
                        "documentId", doc.getId(),
                        "userId", user.getId(),
                        "fileName", doc.getFileName(),
                        "fileType", doc.getFileType(),
                        "modality", modality
                    );
                    for (int i = 0; i < audioChunks.size(); i++) {
                        String chunk = audioChunks.get(i);
                        Map<String, Object> chunkMeta = new java.util.HashMap<>(baseMeta);
                        chunkMeta.put("chunkIndex", i);
                        chunkMeta.put("totalChunks", audioChunks.size());
                        vectorStoreService.addDocument(chunk, chunkMeta);
                    }
                } else {
                    // IMAGE or VIDEO processing
                    PythonEmbeddingClient.EmbeddingResponse response = multimodalService.embedByModality(fileBytes, file.getOriginalFilename(), modality);
                    List<Double> embedding = response.getEmbedding();
                    String transcript = response.getText();
                    content = buildMediaSummary(modality, file.getOriginalFilename(), response.getSummary(), transcript, response.getTags());
                    doc.setExtractedContent(content.length() > 5000 ? content.substring(0, 5000) + "..." : content);

                    Map<String, Object> baseMeta = Map.of(
                        "documentId", doc.getId(),
                        "userId", user.getId(),
                        "fileName", doc.getFileName(),
                        "fileType", doc.getFileType(),
                        "modality", modality
                    );

                    vectorStoreService.addDocumentWithEmbedding(embedding, content, baseMeta);

                    if ("VIDEO".equals(modality) && transcript != null && !transcript.isBlank()) {
                        List<String> videoChunks = chunkingService.chunkText(transcript);
                        for (int i = 0; i < videoChunks.size(); i++) {
                            String chunk = videoChunks.get(i);
                            Map<String, Object> chunkMeta = new java.util.HashMap<>(baseMeta);
                            chunkMeta.put("chunkIndex", i);
                            chunkMeta.put("totalChunks", videoChunks.size());
                            chunkMeta.put("contentType", "video_transcript");
                            vectorStoreService.addDocument(chunk, chunkMeta);
                        }
                    }
                }
            }
            
            doc.setStatus("COMPLETED");
            repository.save(doc);
        } catch (Exception e) {
            log.error("Failed to process document: {}", file.getOriginalFilename(), e);
            doc.setStatus("FAILED");
            repository.save(doc);
            throw e;
        }

        return doc;
    }

    @Transactional
    public MultimodalDocument togglePublicStatus(Long id, User user) {
        MultimodalDocument doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found: " + id));

        // Permission check: only owner or admin can toggle
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase("ROLE_ADMIN") || role.getName().equalsIgnoreCase("ADMIN"));
        
        Long ownerId = (doc.getUser() != null) ? doc.getUser().getId() : null;
        if (!Objects.equals(ownerId, user.getId()) && !isAdmin) {
             throw new RuntimeException("Unauthorized to toggle public status for this document");
        }

        doc.setShared(!doc.isShared());
        return repository.save(doc);
    }

    public List<MultimodalDocument> getAllDocuments(User user) {
        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()))) {
            return repository.findAll();
        }
        return repository.findByUser_IdOrSharedTrue(user.getId());
    }

    public void deleteDocument(Long id, User user) throws IOException {
        MultimodalDocument doc;
        if (user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()))) {
            doc = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        } else {
            doc = repository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Document not found or access denied"));
        }
        
        // Log document access
        try {
            statisticsService.logDocumentAccess(id, null, 
                com.multimodal.rag.model.DocumentAccessLog.AccessType.DELETE);
        } catch (Exception e) {
            log.warn("Failed to log document access", e);
        }
        
        // Gracefully delete file (OSS or local)
        String pathStr = doc.getFilePath();
        if (pathStr != null && !pathStr.isEmpty()) {
            try {
                if (pathStr.startsWith("oss://")) {
                    String ossKey = pathStr.substring(6);
                    ossStorageService.deleteFile(ossKey);
                } else {
                    Path filePath = Paths.get(pathStr);
                    Files.deleteIfExists(filePath);
                }
            } catch (Exception e) {
                log.warn("Could not delete file for document {}: {}", id, e.getMessage());
            }
        }
        
        // Delete vector data from PGVector
        try {
            vectorStoreService.deleteByDocumentId(id);
        } catch (Exception e) {
            log.warn("Vector deletion failed for document {}, continuing with MySQL deletion", id);
        }

        // Ensure database record is removed even if file system operation fails
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

    /**
     * Get the storage path string (may be oss:// or local path)
     */
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "unknown";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
