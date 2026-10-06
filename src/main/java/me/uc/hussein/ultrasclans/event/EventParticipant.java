package me.uc.hussein.ultrasclans.event;

import java.util.UUID;

/**
 * الموقع السابق للحدث يُخزَّن كإحداثيات خام (وليس Bukkit Location) حتى لا
 * تحتاج طبقة repository/ لاستدعاء Bukkit API إطلاقًا (قاعدة ثابتة في
 * المشروع بأكمله) - تحويله إلى Location الفعلي يتم فقط في EventService
 * على الـmain thread عبر Bukkit.getWorld(preWorld).
 */
public final class EventParticipant {

    private final UUID eventId;
    private final UUID playerUuid;
    private final UUID clanId;
    private ParticipantState state;
    private final String preWorld;
    private final double preX, preY, preZ;
    private final float preYaw, prePitch;
    private Long disconnectedAt;

    public EventParticipant(UUID eventId, UUID playerUuid, UUID clanId, ParticipantState state,
                             String preWorld, double preX, double preY, double preZ,
                             float preYaw, float prePitch, Long disconnectedAt) {
        this.eventId = eventId;
        this.playerUuid = playerUuid;
        this.clanId = clanId;
        this.state = state;
        this.preWorld = preWorld;
        this.preX = preX;
        this.preY = preY;
        this.preZ = preZ;
        this.preYaw = preYaw;
        this.prePitch = prePitch;
        this.disconnectedAt = disconnectedAt;
    }

    public UUID getEventId() { return eventId; }
    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getClanId() { return clanId; }
    public ParticipantState getState() { return state; }
    public void setState(ParticipantState state) { this.state = state; }
    public String getPreWorld() { return preWorld; }
    public double getPreX() { return preX; }
    public double getPreY() { return preY; }
    public double getPreZ() { return preZ; }
    public float getPreYaw() { return preYaw; }
    public float getPrePitch() { return prePitch; }
    public Long getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(Long disconnectedAt) { this.disconnectedAt = disconnectedAt; }
}
