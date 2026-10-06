package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.model.Clan;

/**
 * نتيجة محاولة إنشاء كلان. failCode عند الفشل يكون أحد:
 * ALREADY_IN_CLAN, COOLDOWN, NAME_INVALID:&lt;reason&gt;, NAME_TAKEN,
 * NOT_ENOUGH_MONEY, DB_ERROR
 */
public final class CreateClanResult {

    private final boolean success;
    private final Clan clan;
    private final String failCode;

    private CreateClanResult(boolean success, Clan clan, String failCode) {
        this.success = success;
        this.clan = clan;
        this.failCode = failCode;
    }

    public static CreateClanResult success(Clan clan) {
        return new CreateClanResult(true, clan, null);
    }

    public static CreateClanResult fail(String failCode) {
        return new CreateClanResult(false, null, failCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public Clan getClan() {
        return clan;
    }

    public String getFailCode() {
        return failCode;
    }
}
