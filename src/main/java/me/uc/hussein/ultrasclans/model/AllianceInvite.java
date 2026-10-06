package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

public final class AllianceInvite {

    public enum Status { PENDING, ACCEPTED, DENIED, EXPIRED }

    private final UUID id;
    private final UUID senderClanId;
    private final UUID targetClanId;
    private final UUID senderPlayer;
    private final long createdAt;
    private final long expiresAt;
    private Status status;

    public AllianceInvite(UUID id, UUID senderClanId, UUID targetClanId, UUID senderPlayer,
                           long createdAt, long expiresAt, Status status) {
        this.id = id;
        this.senderClanId = senderClanId;
        this.targetClanId = targetClanId;
        this.senderPlayer = senderPlayer;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getSenderClanId() { return senderClanId; }
    public UUID getTargetClanId() { return targetClanId; }
    public UUID getSenderPlayer() { return senderPlayer; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
