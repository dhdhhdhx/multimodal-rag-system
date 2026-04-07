package com.multimodal.rag.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "query_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "query_text", nullable = false, columnDefinition = "TEXT")
    private String queryText;
    
    @Column(name = "response_time_ms")
    private Integer responseTimeMs;
    
    @Column(name = "document_count")
    private Integer documentCount;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
