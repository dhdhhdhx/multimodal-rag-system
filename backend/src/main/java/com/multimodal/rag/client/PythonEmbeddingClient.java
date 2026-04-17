package com.multimodal.rag.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Client for Python Embedding Service
 * Handles communication with FastAPI service for multimodal embeddings
 */
@Slf4j
@Component
public class PythonEmbeddingClient {

    private final RestTemplate restTemplate;
    
    @Value("${multimodal.python-service.base-url:http://localhost:8000}")
    private String baseUrl;
    
    @Value("${multimodal.python-service.timeout:30000}")
    private int timeout;

    public PythonEmbeddingClient() {
        this.restTemplate = new RestTemplate();
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.restTemplate.setRequestFactory(factory);
        log.info("PythonEmbeddingClient initialized with timeout: {}ms", timeout);
    }

    /**
     * Embed text using sentence-transformers
     */
    public EmbeddingResponse embedText(String text) {
        return embedText(text, true);
    }

    public EmbeddingResponse embedText(String text, boolean align) {
        try {
            String url = baseUrl + "/embed/text?align=" + align;
            
            Map<String, String> request = Map.of("text", text);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                url, entity, EmbeddingResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Text embedded successfully, dimension: {}", response.getBody().getDimension());
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to embed text");
        } catch (Exception e) {
            log.error("Error calling Python embedding service for text: {}", e.getMessage());
            throw new RuntimeException("Text embedding failed", e);
        }
    }

    /**
     * Embed image using CLIP
     */
    public EmbeddingResponse embedImage(byte[] imageBytes, String filename) {
        return embedImage(imageBytes, filename, true);
    }

    public EmbeddingResponse embedImage(byte[] imageBytes, String filename, boolean align) {
        try {
            String url = baseUrl + "/embed/image?align=" + align;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                url, entity, EmbeddingResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Image embedded successfully, dimension: {}", response.getBody().getDimension());
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to embed image");
        } catch (Exception e) {
            log.error("Error calling Python embedding service for image: {}", e.getMessage());
            throw new RuntimeException("Image embedding failed", e);
        }
    }

    /**
     * Transcribe audio using Whisper
     */
    public String transcribeAudio(byte[] audioBytes, String filename) {
        try {
            String url = baseUrl + "/transcribe/audio";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<TranscriptionResponse> response = restTemplate.postForEntity(
                url, entity, TranscriptionResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Audio transcribed successfully");
                return response.getBody().getText();
            }
            
            throw new RuntimeException("Failed to transcribe audio");
        } catch (Exception e) {
            log.error("Error calling Python embedding service for audio: {}", e.getMessage());
            throw new RuntimeException("Audio transcription failed", e);
        }
    }

    /**
     * Embed audio (transcribe then embed text)
     */
    public EmbeddingResponse embedAudio(byte[] audioBytes, String filename) {
        try {
            String url = baseUrl + "/embed/audio?align=true";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                url, entity, EmbeddingResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Audio embedded successfully, dimension: {}", response.getBody().getDimension());
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to embed audio");
        } catch (Exception e) {
            log.error("Error calling Python embedding service for audio: {}", e.getMessage());
            throw new RuntimeException("Audio embedding failed", e);
        }
    }

    /**
     * Embed video using keyframe extraction + CLIP
     */
    public EmbeddingResponse embedVideo(byte[] videoBytes, String filename) {
        return embedVideo(videoBytes, filename, true);
    }

    public EmbeddingResponse embedVideo(byte[] videoBytes, String filename, boolean align) {
        try {
            String url = baseUrl + "/embed/video?align=" + align;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(videoBytes) {
                @Override
                public String getFilename() {
                    return filename;
                }
            });
            
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
            
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                url, entity, EmbeddingResponse.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("Video embedded successfully, dimension: {}", response.getBody().getDimension());
                return response.getBody();
            }
            
            throw new RuntimeException("Failed to embed video");
        } catch (Exception e) {
            log.error("Error calling Python embedding service for video: {}", e.getMessage());
            throw new RuntimeException("Video embedding failed", e);
        }
    }

    /**
     * Check if Python service is available
     */
    public boolean isServiceAvailable() {
        try {
            String url = baseUrl + "/";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Python embedding service not available: {}", e.getMessage());
            return false;
        }
    }

    // Response DTOs
    @lombok.Data
    public static class EmbeddingResponse {
        private List<Double> embedding;
        private int dimension;
        private boolean aligned;
        private List<String> tags;
        private String text;
        private String summary;
    }

    @lombok.Data
    public static class TranscriptionResponse {
        private String text;
        private String language;
    }
}
