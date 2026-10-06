package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

public final class ClanInvite {

    public enum Status { PENDING, ACCEPTED, DENIED, EXPIRED }

    private final UUID id;
    private final UUID clanId;
    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final long createdAt;
    private final long expiresAt;
    private Status status;

    public ClanInvite(UUID id, UUID clanId, UUID senderUuid, UUID receiverUuid,
                       long createdAt, long expiresAt, Status status) {
        this.id = id;
        this.clanId = clanId;
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
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

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public UUID getReceiverUuid() {
        return receiverUuid;
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
