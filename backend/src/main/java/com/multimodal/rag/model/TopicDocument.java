package com.multimodal.rag.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 话题与文档的关联表
 */
@Entity
@Table(name = "topic_documents", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"topic_id", "document_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", insertable = false, updatable = false)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", insertable = false, updatable = false)
    private MultimodalDocument document;

    @PrePersist
    protected void onCreate() {
        addedAt = LocalDateTime.now();
    }
}
