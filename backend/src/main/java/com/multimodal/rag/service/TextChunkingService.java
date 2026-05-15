package com.multimodal.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Splits documents into retrieval-friendly chunks while preserving heading context.
 */
@Service
@Slf4j
public class TextChunkingService {

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 160;
    private static final String CHAPTER_HEADING_REGEX =
            "^\\u7b2c[\\u4e00-\\u9fa5\\d]+[\\u7ae0\\u8282\\u90e8\\u5206\\u7bc7]\\s*.*$";

    public record TextChunk(String headingPath, String content) {}

    public List<String> chunkText(String text) {
        return chunkDocument(text).stream()
                .map(TextChunk::content)
                .toList();
    }

    public List<TextChunk> chunkDocument(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<Section> sections = splitIntoSections(text.trim());
        List<TextChunk> chunks = new ArrayList<>();
        for (Section section : sections) {
            chunks.addAll(chunkSection(section));
        }

        if (chunks.isEmpty()) {
            chunks.add(new TextChunk("", text.trim()));
        }

        log.info("Split text ({} chars) into {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private List<Section> splitIntoSections(String text) {
        List<Section> sections = new ArrayList<>();
        Deque<String> headingStack = new ArrayDeque<>();
        StringBuilder currentBody = new StringBuilder();
        String currentHeadingPath = "";

        for (String rawLine : text.split("\\n")) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (isHeading(trimmed)) {
                flushSection(sections, currentHeadingPath, currentBody);

                int level = headingLevel(trimmed);
                String headingText = cleanHeading(trimmed);
                while (headingStack.size() >= level) {
                    headingStack.removeLast();
                }
                headingStack.addLast(headingText);
                currentHeadingPath = String.join(" > ", headingStack);
                continue;
            }

            if (currentBody.length() > 0) {
                currentBody.append('\n');
            }
            currentBody.append(line);
        }

        flushSection(sections, currentHeadingPath, currentBody);
        return sections;
    }

    private void flushSection(List<Section> sections, String headingPath, StringBuilder currentBody) {
        String body = currentBody.toString().trim();
        if (!body.isEmpty()) {
            sections.add(new Section(headingPath, body));
        }
        currentBody.setLength(0);
    }

    private List<TextChunk> chunkSection(Section section) {
        List<TextChunk> chunks = new ArrayList<>();
        List<String> blocks = splitIntoBlocks(section.body());
        StringBuilder current = new StringBuilder();

        for (String block : blocks) {
            if (block.length() > CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(new TextChunk(section.headingPath(), current.toString().trim()));
                    current = overlapBuilder(current.toString());
                }
                for (String fragment : splitLongBlock(block)) {
                    if (!fragment.isBlank()) {
                        chunks.add(new TextChunk(section.headingPath(), fragment.trim()));
                    }
                }
                continue;
            }

            int required = current.length() == 0 ? block.length() : current.length() + 2 + block.length();
            if (required <= CHUNK_SIZE) {
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(block);
            } else {
                if (current.length() > 0) {
                    chunks.add(new TextChunk(section.headingPath(), current.toString().trim()));
                }
                current = overlapBuilder(current.toString());
                if (current.length() > 0) {
                    current.append("\n\n");
                }
                current.append(block);
            }
        }

        if (current.length() > 0) {
            chunks.add(new TextChunk(section.headingPath(), current.toString().trim()));
        }
        return chunks;
    }

    private List<String> splitIntoBlocks(String text) {
        String[] rawBlocks = text.split("\\n\\s*\\n");
        List<String> blocks = new ArrayList<>();
        for (String block : rawBlocks) {
            String trimmed = block.trim();
            if (!trimmed.isEmpty()) {
                blocks.add(trimmed);
            }
        }
        return blocks;
    }

    private List<String> splitLongBlock(String block) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = block.split("(?<=[\\u3002\\uff01\\uff1f!?;:\\n.])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > CHUNK_SIZE) {
                if (current.length() > 0) {
                    chunks.add(current.toString().trim());
                    current = new StringBuilder();
                }
                chunks.addAll(forceSplit(trimmed));
                continue;
            }

            int required = current.length() == 0 ? trimmed.length() : current.length() + 1 + trimmed.length();
            if (required <= CHUNK_SIZE) {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(trimmed);
            } else {
                chunks.add(current.toString().trim());
                current = overlapBuilder(current.toString());
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(trimmed);
            }
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }
        return chunks;
    }

    private List<String> forceSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            chunks.add(text.substring(start, end).trim());
            if (end >= text.length()) {
                break;
            }
            start = Math.max(end - CHUNK_OVERLAP, start + 1);
        }
        return chunks;
    }

    private StringBuilder overlapBuilder(String previousChunk) {
        String cleaned = previousChunk == null ? "" : previousChunk.trim();
        if (cleaned.isEmpty()) {
            return new StringBuilder();
        }
        if (cleaned.length() <= CHUNK_OVERLAP) {
            return new StringBuilder(cleaned);
        }
        return new StringBuilder(cleaned.substring(cleaned.length() - CHUNK_OVERLAP));
    }

    private boolean isHeading(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        return line.matches("^#{1,6}\\s+.+$")
                || line.matches(CHAPTER_HEADING_REGEX)
                || line.matches("^\\d+(\\.\\d+){0,4}\\s+.+$");
    }

    private int headingLevel(String line) {
        if (line.startsWith("#")) {
            int level = 0;
            while (level < line.length() && line.charAt(level) == '#') {
                level++;
            }
            return Math.min(level, 6);
        }
        if (line.matches("^\\d+(\\.\\d+){0,4}\\s+.+$")) {
            String prefix = line.split("\\s+", 2)[0];
            return prefix.split("\\.").length;
        }
        return 1;
    }

    private String cleanHeading(String line) {
        if (line.startsWith("#")) {
            return line.replaceFirst("^#{1,6}\\s+", "").trim();
        }
        return line.trim();
    }

    private record Section(String headingPath, String body) {
    }
}
