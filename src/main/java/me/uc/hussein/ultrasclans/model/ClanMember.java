package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

/**
 * يمثّل عضوية لاعب واحد داخل كلان.
 */
public final class ClanMember {

    private final UUID uuid;
    private final UUID clanId;
    private String rankKey;
    private int memberLevel;
    private int memberXp;
    private int kills;
    private int deaths;
    private int contribution;
    private final long joinedAt;
    private long lastOnline;

    public ClanMember(UUID uuid, UUID clanId, String rankKey, int memberLevel, int memberXp,
                       int kills, int deaths, int contribution, long joinedAt, long lastOnline) {
        this.uuid = uuid;
        this.clanId = clanId;
        this.rankKey = rankKey;
        this.memberLevel = memberLevel;
        this.memberXp = memberXp;
        this.kills = kills;
        this.deaths = deaths;
        this.contribution = contribution;
        this.joinedAt = joinedAt;
        this.lastOnline = lastOnline;
    }

    public UUID getUuid() {
        return uuid;
    }

    public UUID getClanId() {
        return clanId;
    }

    public String getRankKey() {
        return rankKey;
    }

    public void setRankKey(String rankKey) {
        this.rankKey = rankKey;
    }

    public int getMemberLevel() {
        return memberLevel;
    }

    public void setMemberLevel(int memberLevel) {
        this.memberLevel = memberLevel;
    }

    public int getMemberXp() {
        return memberXp;
    }

    public void setMemberXp(int memberXp) {
        this.memberXp = memberXp;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getContribution() {
        return contribution;
    }

    public void setContribution(int contribution) {
        this.contribution = contribution;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long lastOnline) {
        this.lastOnline = lastOnline;
    }
}
