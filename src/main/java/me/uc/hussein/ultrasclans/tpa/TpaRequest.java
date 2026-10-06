package me.uc.hussein.ultrasclans.tpa;

import java.util.UUID;

/**
 * طلب TPA مؤقت (لا يُحفظ في قاعدة البيانات - ذو صلاحية قصيرة وذو معنى
 * فقط طالما الطرفان متصلان، على عكس دعوات الكلان طويلة الأمد).
 */
public final class TpaRequest {

    public enum Mode {
        /** المُرسِل يريد الانتقال إلى المستلِم. */
        STANDARD,
        /** المُرسِل يريد من المستلِم الانتقال إليه. */
        HERE
    }

    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final Mode mode;
    private final long expiresAt;

    public TpaRequest(UUID senderUuid, UUID receiverUuid, Mode mode, long expiresAt) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.mode = mode;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public UUID getSenderUuid() { return senderUuid; }
    public UUID getReceiverUuid() { return receiverUuid; }
    public Mode getMode() { return mode; }
}
