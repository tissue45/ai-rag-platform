package com.astra.backend.document;

import com.astra.backend.user.UserEntity;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private DocumentSourceType sourceType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingest_status", nullable = false, length = 32)
    private DocumentIngestStatus ingestStatus;

    @Column(name = "ingest_error", columnDefinition = "TEXT")
    private String ingestError;

    @Column(name = "ingested_at")
    private Instant ingestedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UserEntity getOwner() {
        return owner;
    }

    public void setOwner(UserEntity owner) {
        this.owner = owner;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DocumentSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DocumentSourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public DocumentIngestStatus getIngestStatus() {
        return ingestStatus;
    }

    public void setIngestStatus(DocumentIngestStatus ingestStatus) {
        this.ingestStatus = ingestStatus;
    }

    public String getIngestError() {
        return ingestError;
    }

    public void setIngestError(String ingestError) {
        this.ingestError = ingestError;
    }

    public Instant getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(Instant ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}

