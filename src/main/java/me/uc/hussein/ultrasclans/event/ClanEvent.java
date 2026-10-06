package me.uc.hussein.ultrasclans.event;

import java.util.UUID;

/**
 * يمثّل تحديًا/حربًا بين كلانين من الاقتراح حتى الانتهاء (أقسام 32-43).
 * كل الحقول القابلة للتغيير أثناء الحدث (الجولة الحالية، النتيجة...)
 * يجب تعديلها فقط عبر EventService (synchronized على الكائن نفسه).
 */
public final class ClanEvent {

    private final UUID id;
    private final UUID clanA;
    private final UUID clanB;
    private EventStatus status;
    private final LocationType locationType;
    private final int roundDurationSeconds;
    private final int totalRounds;
    private int currentRound;
    private int scoreA;
    private int scoreB;
    private final double moneyWager;
    private final long cpWager;
    private UUID winnerClanId;
    private final UUID createdBy;
    private final long createdAt;
    private Long startedAt;
    private Long endedAt;

    public ClanEvent(UUID id, UUID clanA, UUID clanB, EventStatus status, LocationType locationType,
                      int roundDurationSeconds, int totalRounds, int currentRound, int scoreA, int scoreB,
                      double moneyWager, long cpWager, UUID winnerClanId, UUID createdBy,
                      long createdAt, Long startedAt, Long endedAt) {
        this.id = id;
        this.clanA = clanA;
        this.clanB = clanB;
        this.status = status;
        this.locationType = locationType;
        this.roundDurationSeconds = roundDurationSeconds;
        this.totalRounds = totalRounds;
        this.currentRound = currentRound;
        this.scoreA = scoreA;
        this.scoreB = scoreB;
        this.moneyWager = moneyWager;
        this.cpWager = cpWager;
        this.winnerClanId = winnerClanId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public boolean involves(UUID clanId) {
        return clanA.equals(clanId) || clanB.equals(clanId);
    }

    public UUID otherClan(UUID clanId) {
        return clanA.equals(clanId) ? clanB : clanA;
    }

    /** يُستدعى فقط من داخل synchronized(this) من EventService. */
    public int scoreFor(UUID clanId) {
        return clanA.equals(clanId) ? scoreA : scoreB;
    }

    public UUID getId() { return id; }
    public UUID getClanA() { return clanA; }
    public UUID getClanB() { return clanB; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public LocationType getLocationType() { return locationType; }
    public int getRoundDurationSeconds() { return roundDurationSeconds; }
    public int getTotalRounds() { return totalRounds; }
    public int getCurrentRound() { return currentRound; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
    public int getScoreA() { return scoreA; }
    public void setScoreA(int scoreA) { this.scoreA = scoreA; }
    public int getScoreB() { return scoreB; }
    public void setScoreB(int scoreB) { this.scoreB = scoreB; }
    public double getMoneyWager() { return moneyWager; }
    public long getCpWager() { return cpWager; }
    public UUID getWinnerClanId() { return winnerClanId; }
    public void setWinnerClanId(UUID winnerClanId) { this.winnerClanId = winnerClanId; }
    public UUID getCreatedBy() { return createdBy; }
    public long getCreatedAt() { return createdAt; }
    public Long getStartedAt() { return startedAt; }
    public void setStartedAt(Long startedAt) { this.startedAt = startedAt; }
    public Long getEndedAt() { return endedAt; }
    public void setEndedAt(Long endedAt) { this.endedAt = endedAt; }
}
