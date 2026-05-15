package com.multimodal.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkingServiceTest {

    private final TextChunkingService chunkingService = new TextChunkingService();

    @Test
    void chunkDocumentShouldKeepHeadingPath() {
        String document = """
                # Course Notes

                ## Backend

                Spring Boot handles the API layer.
                PGVector stores embeddings for retrieval.
                """;

        List<TextChunkingService.TextChunk> chunks = chunkingService.chunkDocument(document);

        assertEquals(1, chunks.size());
        assertEquals("Course Notes > Backend", chunks.get(0).headingPath());
        assertTrue(chunks.get(0).content().contains("Spring Boot handles the API layer."));
    }

    @Test
    void chunkDocumentShouldSplitLongSectionsIntoMultipleChunks() {
        String repeatedSentence = "Retrieval quality depends on parsing, chunking, ranking, and clean context. ";
        StringBuilder builder = new StringBuilder("# Retrieval\n\n");
        for (int i = 0; i < 80; i++) {
            builder.append(repeatedSentence);
        }

        List<TextChunkingService.TextChunk> chunks = chunkingService.chunkDocument(builder.toString());

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> "Retrieval".equals(chunk.headingPath())));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.content().length() <= 1000));
    }
}
