package com.microchip.lambda_core.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_storage")
public class UserStorage {

    @Id
    private UUID userId;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(name = "quota_bytes", nullable = false)
    private long quotaBytes;

    @Column(name = "used_bytes", nullable = false)
    private long usedBytes;

    @Column(name = "reserved_bytes", nullable = false)
    private long reservedBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UserStorage() {}

    public UserStorage(UUID userId, String username, long quotaBytes) {
        this.userId = userId;
        this.username = username;
        this.quotaBytes = quotaBytes;
    }

    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public long getQuotaBytes() { return quotaBytes; }
    public long getUsedBytes() { return usedBytes; }
    public long getReservedBytes() { return reservedBytes; }
    public Instant getCreatedAt() { return createdAt; }

    public void setQuotaBytes(long quotaBytes) { this.quotaBytes = quotaBytes; }
    public void setUsedBytes(long usedBytes) { this.usedBytes = usedBytes; }
    public void setReservedBytes(long reservedBytes) { this.reservedBytes = reservedBytes; }
}
