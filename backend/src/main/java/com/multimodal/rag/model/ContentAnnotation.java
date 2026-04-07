package com.multimodal.rag.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "content_annotations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentAnnotation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "document_id", nullable = false)
    private Long documentId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "start_offset", nullable = false)
    private Integer startOffset;
    
    @Column(name = "end_offset", nullable = false)
    private Integer endOffset;
    
    @Column(name = "highlighted_text", columnDefinition = "TEXT")
    private String highlightedText;
    
    @Column(name = "annotation_text", columnDefinition = "TEXT")
    private String annotationText;
    
    @Column(length = 20)
    private String color = "#FFFF00";
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
