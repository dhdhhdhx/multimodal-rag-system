package com.multimodal.rag.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class DocumentParserService {

    private final Tika tika = new Tika();

    /**
     * Parses the content of a multipart file.
     * Uses Apache Tika for most document types (PDF, Word, etc.).
     * Special handling is planned for images (OCR) and audio/video (Whisper).
     *
     * @param file The file to be parsed.
     * @return Extracted plain text content.
     * @throws IOException If parsing fails.
     */
    public String parseDocument(Path filePath, String originalFilename, String contentType) throws IOException {
        log.info("Parsing file: {}, Content-Type: {}", originalFilename, contentType);

        if (!Files.exists(filePath) || Files.size(filePath) == 0) {
            log.warn("File is empty or does not exist: {}", originalFilename);
            return "";
        }

        // Check if the file is a media type that requires special processing
        if (contentType != null && (contentType.startsWith("image/") || contentType.startsWith("audio/") || contentType.startsWith("video/"))) {
            return parseNonTextMedia(originalFilename, contentType);
        }

        // Use Tika for text-based or standard office documents
        try (InputStream stream = Files.newInputStream(filePath)) {
            return tika.parseToString(stream);
        } catch (Exception e) {
            log.error("Error parsing document with Tika: {}", originalFilename, e);
            throw new IOException("Failed to parse document content using Tika", e);
        }
    }

    /**
     * Handles non-textual media types.
     * Currently provides placeholders for OCR and Whisper integration.
     *
     * @param file The media file.
     * @return Transcribed or extracted text.
     */
    private String parseNonTextMedia(String fileName, String contentType) {
        if (contentType == null) return "Unknown content";

        if (contentType.startsWith("image/")) {
            log.info("Image file detected: {}. Ready for OCR integration.", fileName);
            // TODO: In a production environment, call an OCR service like Google Vision or Tesseract.
            return "[OCR extracted text placeholder for " + fileName + "]";
        } else if (contentType.startsWith("audio/") || contentType.startsWith("video/")) {
            log.info("Media file detected: {}. Ready for Whisper transcription integration.", fileName);
            // TODO: In a production environment, call OpenAI Whisper API or a local whisper.cpp instance.
            return "[Transcription placeholder for " + fileName + "]";
        }

        return "Unsupported media type: " + contentType;
    }
}
