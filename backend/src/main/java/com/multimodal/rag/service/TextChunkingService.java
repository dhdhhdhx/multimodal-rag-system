package com.multimodal.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits large text documents into smaller chunks for vector storage.
 * Uses paragraph/sentence boundaries with overlap for context continuity.
 */
@Service
@Slf4j
public class TextChunkingService {

    private static final int CHUNK_SIZE = 800;      // ~300 tokens for Chinese
    private static final int CHUNK_OVERLAP = 150;    // overlap between chunks

    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        // Small enough to be a single chunk
        if (text.length() <= CHUNK_SIZE) {
            return List.of(text.trim());
        }

        List<String> chunks = new ArrayList<>();
        // First try splitting by double newlines (paragraphs)
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder current = new StringBuilder();
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (current.length() + para.length() + 1 <= CHUNK_SIZE) {
                if (current.length() > 0) current.append("\n\n");
                current.append(para);
            } else {
                // Current buffer is big enough, flush it
                if (current.length() > 0) {
                    chunks.add(current.toString());
                }
                // If single paragraph exceeds chunk size, split by sentences
                if (para.length() > CHUNK_SIZE) {
                    chunks.addAll(splitLongParagraph(para));
                    current = new StringBuilder();
                } else {
                    // Start new chunk with overlap from previous
                    current = new StringBuilder();
                    addOverlap(chunks, current);
                    current.append(para);
                }
            }
        }

        if (current.length() > 0) {
            chunks.add(current.toString());
        }

        log.info("Split text ({} chars) into {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private List<String> splitLongParagraph(String para) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = para.split("(?<=[。！？.!?\\n])\\s*");
        StringBuilder current = new StringBuilder();
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;
            if (current.length() + sentence.length() + 1 <= CHUNK_SIZE) {
                if (current.length() > 0) current.append(" ");
                current.append(sentence);
            } else {
                if (current.length() > 0) chunks.add(current.toString());
                if (sentence.length() > CHUNK_SIZE) {
                    chunks.addAll(forceSplit(sentence));
                    current = new StringBuilder();
                } else {
                    current = new StringBuilder(sentence);
                }
            }
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }

    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end));
            start = end - CHUNK_OVERLAP;
            if (start <= 0 || start >= text.length()) break;
        }
        return chunks;
    }

    private void addOverlap(List<String> chunks, StringBuilder current) {
        if (chunks.isEmpty() || CHUNK_OVERLAP <= 0) return;
        String lastChunk = chunks.get(chunks.size() - 1);
        if (lastChunk.length() > CHUNK_OVERLAP) {
            current.append(lastChunk.substring(lastChunk.length() - CHUNK_OVERLAP));
            current.append("\n\n");
        }
    }
}