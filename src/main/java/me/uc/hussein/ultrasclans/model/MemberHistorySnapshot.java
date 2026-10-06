package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

/**
 * لقطة (snapshot) من بيانات عضو عند مغادرته الكلان، تُحفظ لمدة محددة
 * (افتراضيًا 3 أيام، راجع config.yml -> membership.history-retention-days)
 * حتى يمكن استرجاعها تلقائيًا إذا عاد اللاعب لنفس الكلان.
 */
public final class MemberHistorySnapshot {

    private final UUID uuid;
    private final UUID clanId;
    private final String clanName;
    private final String rankKey;
    private final int memberLevel;
    private final int memberXp;
    private final int kills;
    private final int deaths;
    private final int contribution;
    private final long leftAt;
    private final long expiresAt;

    public MemberHistorySnapshot(UUID uuid, UUID clanId, String clanName, String rankKey, int memberLevel,
                                  int memberXp, int kills, int deaths, int contribution,
                                  long leftAt, long expiresAt) {
        this.uuid = uuid;
        this.clanId = clanId;
        this.clanName = clanName;
        this.rankKey = rankKey;
        this.memberLevel = memberLevel;
        this.memberXp = memberXp;
        this.kills = kills;
        this.deaths = deaths;
        this.contribution = contribution;
        this.leftAt = leftAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getClanId() {
        return clanId;
    }

    public String getClanName() {
        return clanName;
    }

    public String getRankKey() {
        return rankKey;
    }

    public int getMemberLevel() {
        return memberLevel;
    }

    public int getMemberXp() {
        return memberXp;
    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getContribution() {
        return contribution;
    }

    public long getLeftAt() {
        return leftAt;
    }

    public long getExpiresAt() {
        return expiresAt;
    }
}
