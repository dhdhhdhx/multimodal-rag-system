package com.multimodal.rag.service;

import com.multimodal.rag.client.PythonEmbeddingClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * Service for determining file modality and routing to appropriate embedding method
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalEmbeddingService {

    private final PythonEmbeddingClient pythonClient;

    // Supported file extensions by modality
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList(
        "txt", "pdf", "doc", "docx", "md", "rtf"
    );
    
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "tiff"
    );
    
    private static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
        "mp3", "wav", "m4a", "flac", "ogg", "aac"
    );
    
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
        "mp4", "avi", "mov", "wmv", "flv", "mkv", "webm"
    );

    /**
     * Determine modality from filename
     */
    public String detectModality(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        
        if (TEXT_EXTENSIONS.contains(extension)) {
            return "TEXT";
        } else if (IMAGE_EXTENSIONS.contains(extension)) {
            return "IMAGE";
        } else if (AUDIO_EXTENSIONS.contains(extension)) {
            return "AUDIO";
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return "VIDEO";
        }
        
        return "UNKNOWN";
    }

    /**
     * Embed content based on modality
     */
    public PythonEmbeddingClient.EmbeddingResponse embedByModality(byte[] content, String filename, String modality) {
        log.info("Embedding {} file: {}", modality, filename);
        
        try {
            switch (modality) {
                case "IMAGE":
                    return pythonClient.embedImage(content, filename);
                
                case "AUDIO":
                    return pythonClient.embedAudio(content, filename);
                
                case "VIDEO":
                    return pythonClient.embedVideo(content, filename);
                
                case "TEXT":
                default:
                    // For text files, extract text content first
                    String text = new String(content);
                    return pythonClient.embedText(text);
            }
        } catch (Exception e) {
            log.error("Error embedding {} file {}: {}", modality, filename, e.getMessage());
            throw new RuntimeException("Embedding failed for " + modality, e);
        }
    }

    /**
     * Process multipart file and embed
     */
    public PythonEmbeddingClient.EmbeddingResponse processAndEmbed(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        String modality = detectModality(filename);
        byte[] content = file.getBytes();
        
        log.info("Processing file: {} (modality: {})", filename, modality);
        
        return embedByModality(content, filename, modality);
    }

    /**
     * For audio files, get transcription text
     */
    public String transcribeAudio(byte[] audioBytes, String filename) {
        return pythonClient.transcribeAudio(audioBytes, filename);
    }

    /**
     * Check if Python service is available
     */
    public boolean isPythonServiceAvailable() {
        return pythonClient.isServiceAvailable();
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return (lastDot == -1) ? "" : filename.substring(lastDot + 1);
    }
}
