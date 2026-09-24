package com.microchip.lambda_core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_file")
public class UserFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "received_bytes", nullable = false)
    private long receivedBytes;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FileStatus status = FileStatus.UPLOADING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UserFile() {

    }

    public UserFile(UUID ownerId, String name, long sizeBytes, String contentType) {
        this.ownerId = ownerId;
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public long getSizeBytes() { return sizeBytes; }
    public long getReceivedBytes() { return receivedBytes; }
    public String getContentType() { return contentType; }
    public FileStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setReceivedBytes(long receivedBytes) { this.receivedBytes = receivedBytes; }
    public void setStatus(FileStatus status) { this.status = status; }
}
