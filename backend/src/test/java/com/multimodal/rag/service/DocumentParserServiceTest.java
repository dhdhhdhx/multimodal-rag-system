package com.multimodal.rag.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentParserServiceTest {

    private final DocumentParserService parserService = new DocumentParserService();

    @TempDir
    Path tempDir;

    @Test
    void parseDocumentShouldPreserveMarkdownStructureAndMergeWrappedLines() throws Exception {
        Path markdownFile = tempDir.resolve("notes.md");
        String content = """
                # Backend Notes

                This line belongs to the same paragraph
                and should be merged for retrieval.

                - first item
                - second item
                """;
        Files.writeString(markdownFile, content, StandardCharsets.UTF_8);

        String parsed = parserService.parseDocument(markdownFile, "notes.md", "text/markdown");

        assertTrue(parsed.contains("# Backend Notes"));
        assertTrue(parsed.contains("This line belongs to the same paragraph and should be merged for retrieval."));
        assertTrue(parsed.contains("- first item"));
        assertTrue(parsed.contains("- second item"));
    }
}
