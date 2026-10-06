package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

public final class ClanWarp {

    private final UUID clanId;
    private final int warpId;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;
    private final UUID createdBy;
    private final long createdAt;

    public ClanWarp(UUID clanId, int warpId, String world, double x, double y, double z,
                     float yaw, float pitch, UUID createdBy, long createdAt) {
        this.clanId = clanId;
        this.warpId = warpId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getClanId() { return clanId; }
    public int getWarpId() { return warpId; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public UUID getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }
}
