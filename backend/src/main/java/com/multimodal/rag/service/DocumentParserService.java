package com.multimodal.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Slf4j
public class DocumentParserService {

    private static final String CHAPTER_HEADING_REGEX =
            "^\\u7b2c[\\u4e00-\\u9fa5\\d]+[\\u7ae0\\u8282\\u90e8\\u5206\\u7bc7]\\s*.*$";

    private static final Set<String> PLAIN_TEXT_EXTENSIONS = Set.of(
            "txt", "md", "markdown", "csv", "json", "yaml", "yml", "xml",
            "html", "htm", "java", "py", "js", "ts", "tsx", "jsx",
            "css", "sql", "sh", "bat", "ps1", "properties", "log"
    );

    private static final List<Charset> TEXT_CHARSETS = List.of(
            StandardCharsets.UTF_8,
            StandardCharsets.UTF_16LE,
            StandardCharsets.UTF_16BE,
            Charset.forName("GB18030"),
            Charset.forName("GBK")
    );

    private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\\n\\t]]");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("[ \\t\\x0B\\f]+");
    private static final Pattern MULTI_NEWLINE_PATTERN = Pattern.compile("\\n{3,}");

    private final Tika tika = new Tika();

    public String parseDocument(Path filePath, String originalFilename, String contentType) throws IOException {
        log.info("Parsing file: {}, Content-Type: {}", originalFilename, contentType);

        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            log.warn("File is empty or does not exist: {}", originalFilename);
            return "";
        }

        if (isMediaType(contentType)) {
            return parseNonTextMedia(originalFilename, contentType);
        }

        String extension = getExtension(originalFilename);
        if (PLAIN_TEXT_EXTENSIONS.contains(extension) || isPlainTextContentType(contentType)) {
            return normalizeText(readTextFile(filePath), extension);
        }

        try (InputStream stream = Files.newInputStream(filePath)) {
            return normalizeText(tika.parseToString(stream), extension);
        } catch (Exception e) {
            log.error("Error parsing document with Tika: {}", originalFilename, e);
            throw new IOException("Failed to parse document content using Tika", e);
        }
    }

    public String normalizeText(String text) {
        return normalizeText(text, "");
    }

    private String readTextFile(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        for (Charset charset : TEXT_CHARSETS) {
            String decoded = new String(bytes, charset);
            if (looksReadable(decoded)) {
                return decoded;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean looksReadable(String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        long replacementCount = text.chars().filter(ch -> ch == '\uFFFD').count();
        long visibleCount = text.chars().filter(ch -> !Character.isWhitespace(ch)).count();
        if (visibleCount == 0) {
            return true;
        }
        return (double) replacementCount / visibleCount < 0.02;
    }

    private String normalizeText(String text, String extension) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .replace("\uFEFF", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        normalized = CONTROL_CHAR_PATTERN.matcher(normalized).replaceAll(" ");
        if ("md".equals(extension) || "markdown".equals(extension)) {
            normalized = normalizeMarkdown(normalized);
        } else {
            normalized = normalizeWrappedText(normalized);
        }

        normalized = MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = MULTI_NEWLINE_PATTERN.matcher(normalized).replaceAll("\n\n");
        return normalized.trim();
    }

    private String normalizeMarkdown(String text) {
        List<String> lines = text.lines().toList();
        List<String> out = new ArrayList<>();
        List<String> paragraph = new ArrayList<>();
        boolean inCodeFence = false;

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                flushParagraph(paragraph, out);
                out.add(trimmed);
                inCodeFence = !inCodeFence;
                continue;
            }

            if (inCodeFence) {
                out.add(line);
                continue;
            }

            if (trimmed.isEmpty()) {
                flushParagraph(paragraph, out);
                continue;
            }

            if (isMarkdownHeading(trimmed) || isListLike(trimmed) || isTableRow(trimmed)) {
                flushParagraph(paragraph, out);
                out.add(trimmed);
                continue;
            }

            paragraph.add(trimmed);
        }

        flushParagraph(paragraph, out);
        return String.join("\n", out);
    }

    private String normalizeWrappedText(String text) {
        List<String> lines = text.lines().toList();
        List<String> out = new ArrayList<>();
        List<String> paragraph = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.stripTrailing();
            String trimmed = line.trim();

            if (trimmed.isEmpty()) {
                flushParagraph(paragraph, out);
                continue;
            }

            if (isHeadingLike(trimmed) || isListLike(trimmed)) {
                flushParagraph(paragraph, out);
                out.add(trimmed);
                continue;
            }

            paragraph.add(trimmed);
        }

        flushParagraph(paragraph, out);
        return String.join("\n\n", out);
    }

    private void flushParagraph(List<String> paragraph, List<String> out) {
        if (paragraph.isEmpty()) {
            return;
        }
        StringBuilder merged = new StringBuilder(paragraph.get(0));
        for (int i = 1; i < paragraph.size(); i++) {
            merged.append(joinToken(merged.toString(), paragraph.get(i)));
        }
        out.add(merged.toString().trim());
        paragraph.clear();
    }

    private String joinToken(String left, String right) {
        if (left.endsWith("-") && startsWithAsciiLetterOrDigit(right)) {
            return right;
        }
        if (endsWithChineseOrCjk(left) && startsWithChineseOrCjk(right)) {
            return right;
        }
        if (startsWithPunctuation(right)) {
            return right;
        }
        return " " + right;
    }

    private boolean isMarkdownHeading(String line) {
        return line.matches("^#{1,6}\\s+.+$");
    }

    private boolean isHeadingLike(String line) {
        return isMarkdownHeading(line)
                || line.matches(CHAPTER_HEADING_REGEX)
                || line.matches("^\\d+(\\.\\d+){0,4}\\s+.+$")
                || line.matches("^[A-Z][A-Z0-9\\s:_/-]{3,80}$");
    }

    private boolean isListLike(String line) {
        return line.matches("^[-*+]\\s+.+$")
                || line.matches("^\\d+[.)\\u3001]\\s+.+$")
                || line.matches("^[\\uFF08(]?[\\u4e00-\\u9fa5\\d]+[)\\uFF09\\u3001.]\\s*.+$");
    }

    private boolean isTableRow(String line) {
        return line.contains("|");
    }

    private boolean startsWithAsciiLetterOrDigit(String value) {
        return !value.isEmpty() && Character.toString(value.charAt(0)).matches("[A-Za-z0-9]");
    }

    private boolean startsWithChineseOrCjk(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char ch = value.charAt(0);
        return ch >= '\u2E80' && ch <= '\u9FFF';
    }

    private boolean endsWithChineseOrCjk(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char ch = value.charAt(value.length() - 1);
        return ch >= '\u2E80' && ch <= '\u9FFF';
    }

    private boolean startsWithPunctuation(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char ch = value.charAt(0);
        return ch == '.'
                || ch == ','
                || ch == '!'
                || ch == '?'
                || ch == ';'
                || ch == ':'
                || ch == ')'
                || ch == ']'
                || ch == '}'
                || ch == '\uFF0C'
                || ch == '\u3002'
                || ch == '\uFF01'
                || ch == '\uFF1F'
                || ch == '\uFF1B'
                || ch == '\uFF1A';
    }

    private boolean isMediaType(String contentType) {
        return contentType != null
                && (contentType.startsWith("image/")
                || contentType.startsWith("audio/")
                || contentType.startsWith("video/"));
    }

    private boolean isPlainTextContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/")
                || lower.contains("json")
                || lower.contains("xml")
                || lower.contains("yaml");
    }

    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String parseNonTextMedia(String fileName, String contentType) {
        if (contentType == null) {
            return "Unknown content";
        }

        if (contentType.startsWith("image/")) {
            log.info("Image file detected: {}. OCR should be handled by the multimodal pipeline.", fileName);
            return "[Image content placeholder for " + fileName + "]";
        }
        if (contentType.startsWith("audio/") || contentType.startsWith("video/")) {
            log.info("Media file detected: {}. Transcription should be handled by the multimodal pipeline.", fileName);
            return "[Transcript placeholder for " + fileName + "]";
        }

        return "Unsupported media type: " + contentType;
    }
}
