package me.uc.hussein.ultrasclans.model;

import java.util.UUID;

/**
 * تحالف بين كلانين. العلاقة غير موجهة، لكن clanA/clanB مخزّنان بترتيب
 * ثابت (الأصغر UUID أولًا) لتفادي تكرار نفس التحالف بصفين مختلفين.
 */
public final class ClanAlliance {

    private final UUID id;
    private final UUID clanA;
    private final UUID clanB;
    private final long createdAt;

    public ClanAlliance(UUID id, UUID clanA, UUID clanB, long createdAt) {
        this.id = id;
        this.clanA = clanA;
        this.clanB = clanB;
        this.createdAt = createdAt;
    }

    public boolean involves(UUID clanId) {
        return clanA.equals(clanId) || clanB.equals(clanId);
    }

    public UUID otherClan(UUID clanId) {
        return clanA.equals(clanId) ? clanB : clanA;
    }

    public UUID getId() { return id; }
    public UUID getClanA() { return clanA; }
    public UUID getClanB() { return clanB; }
    public long getCreatedAt() { return createdAt; }
}
