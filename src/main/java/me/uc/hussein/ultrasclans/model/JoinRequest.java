package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

public final class JoinRequest {

    public enum Status { PENDING, ACCEPTED, DENIED, EXPIRED }

    private final UUID id;
    private final UUID clanId;
    private final UUID requesterUuid;
    private final long createdAt;
    private final long expiresAt;
    private Status status;

    public JoinRequest(UUID id, UUID clanId, UUID requesterUuid, long createdAt, long expiresAt, Status status) {
        this.id = id;
        this.clanId = clanId;
        this.requesterUuid = requesterUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClanId() {
        return clanId;
    }

    public UUID getRequesterUuid() {
        return requesterUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
