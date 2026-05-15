package com.multimodal.rag.service;

import com.multimodal.rag.model.MultimodalDocument;
import com.multimodal.rag.model.User;
import com.multimodal.rag.repository.MultimodalDocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void reindexDocumentShouldRefreshVectorsFromStoredFile() throws Exception {
        MultimodalDocumentRepository repository = mock(MultimodalDocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        StatisticsService statisticsService = mock(StatisticsService.class);
        MultimodalEmbeddingService multimodalService = mock(MultimodalEmbeddingService.class);
        OssStorageService ossStorageService = mock(OssStorageService.class);

        KnowledgeService service = new KnowledgeService(
                repository,
                new DocumentParserService(),
                vectorStoreService,
                statisticsService,
                multimodalService,
                new TextChunkingService(),
                ossStorageService
        );

        Path file = tempDir.resolve("notes.md");
        Files.writeString(file, """
                # Retrieval Notes

                The markdown content should be reindexed with its heading structure preserved.
                """, StandardCharsets.UTF_8);

        User owner = buildUser(7L);
        MultimodalDocument document = MultimodalDocument.builder()
                .id(11L)
                .fileName("notes.md")
                .fileType("md")
                .filePath(file.toString())
                .status("COMPLETED")
                .user(owner)
                .build();

        when(repository.findByIdAndUser_Id(11L, 7L)).thenReturn(Optional.of(document));
        when(repository.save(any(MultimodalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(multimodalService.detectModality("notes.md")).thenReturn("TEXT");

        MultimodalDocument result = service.reindexDocument(11L, owner);

        assertEquals("COMPLETED", result.getStatus());
        assertTrue(result.getExtractedContent().contains("# Retrieval Notes"));
        verify(vectorStoreService).deleteByDocumentId(11L);
        verify(vectorStoreService, atLeastOnce()).addDocument(anyString(), any());
    }

    @Test
    void reindexOwnedDocumentsShouldContinueAfterSingleFailure() throws Exception {
        MultimodalDocumentRepository repository = mock(MultimodalDocumentRepository.class);
        VectorStoreService vectorStoreService = mock(VectorStoreService.class);
        StatisticsService statisticsService = mock(StatisticsService.class);
        MultimodalEmbeddingService multimodalService = mock(MultimodalEmbeddingService.class);
        OssStorageService ossStorageService = mock(OssStorageService.class);

        KnowledgeService service = new KnowledgeService(
                repository,
                new DocumentParserService(),
                vectorStoreService,
                statisticsService,
                multimodalService,
                new TextChunkingService(),
                ossStorageService
        );

        Path goodFile = tempDir.resolve("good.md");
        Files.writeString(goodFile, "# Good\n\nThis file can be reindexed.", StandardCharsets.UTF_8);

        User owner = buildUser(9L);
        MultimodalDocument goodDocument = MultimodalDocument.builder()
                .id(21L)
                .fileName("good.md")
                .fileType("md")
                .filePath(goodFile.toString())
                .status("COMPLETED")
                .user(owner)
                .build();

        MultimodalDocument missingDocument = MultimodalDocument.builder()
                .id(22L)
                .fileName("missing.md")
                .fileType("md")
                .filePath(tempDir.resolve("missing.md").toString())
                .status("COMPLETED")
                .user(owner)
                .build();

        when(repository.findByUser_Id(9L)).thenReturn(List.of(goodDocument, missingDocument));
        when(repository.save(any(MultimodalDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(multimodalService.detectModality(eq("good.md"))).thenReturn("TEXT");

        KnowledgeService.ReindexSummary summary = service.reindexOwnedDocuments(owner);

        assertEquals(2, summary.total());
        assertEquals(1, summary.succeeded());
        assertEquals(1, summary.failed());
        assertEquals(1, summary.failures().size());
        assertEquals("missing.md", summary.failures().get(0).fileName());
        verify(vectorStoreService).deleteByDocumentId(21L);
    }

    private User buildUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setRoles(new HashSet<>());
        return user;
    }
}
