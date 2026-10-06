package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

public final class ClanSpawn {

    private final UUID clanId;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;

    public ClanSpawn(UUID clanId, String world, double x, double y, double z, float yaw, float pitch) {
        this.clanId = clanId;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public UUID getClanId() { return clanId; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
}
