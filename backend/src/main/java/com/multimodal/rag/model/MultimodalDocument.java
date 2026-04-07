package com.multimodal.rag.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "multimodal_documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultimodalDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType; // pdf, image, audio, video, code, etc.

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String filePath;
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String extractedContent;

    private LocalDateTime uploadTime;
    private String status; // UPLOADED, PROCESSING, COMPLETED, FAILED
    
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 500)
    private String tags; // comma-separated, e.g. "技术笔记,AI,RAG"

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "is_public")
    @Builder.Default
    private boolean shared = false; // System-wide knowledge or seed data

    @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
    public boolean isShared() {
        return shared;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("isPublic")
    public void setShared(boolean shared) {
        this.shared = shared;
    }

    @PrePersist
    protected void onCreate() {
        uploadTime = LocalDateTime.now();
    }

    @com.fasterxml.jackson.annotation.JsonProperty("userId")
    public Long getOwnerId() {
        try {
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
