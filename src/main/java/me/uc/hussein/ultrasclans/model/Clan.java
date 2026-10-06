package me.uc.hussein.ultrasclans.model;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * يمثّل كلانًا واحدًا في الذاكرة (in-memory cache) فوق ما هو مخزّن
 * في قاعدة البيانات. كل التعديلات على هذا الكائن يجب أن تمر عبر
 * ClanService حتى تُطبّق أيضًا على قاعدة البيانات (لا تُعدّل الحقول
 * هنا مباشرة من الأوامر أو الـGUI).
 */
public final class Clan {

    private final UUID id;
    private String name;
    private UUID ownerUuid;
    private String color;
    private boolean isPublic;
    private int memberCapacity;
    private double bankBalance;
    private double bankCapacity;
    private long cp;
    private int clanLevel;
    private int storageLevel;
    private int warpCapacity;
    private int allianceCapacity;
    private final long createdAt;

    /** قفل بسيط يمنع تنفيذ عمليتين حساستين (بنك مثلاً) في نفس اللحظة على نفس الكلان. */
    private final AtomicBoolean transactionLock = new AtomicBoolean(false);

    public Clan(UUID id, String name, UUID ownerUuid, String color, boolean isPublic,
                int memberCapacity, double bankBalance, double bankCapacity, long cp,
                int clanLevel, int storageLevel, int warpCapacity, int allianceCapacity,
                long createdAt) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.color = color;
        this.isPublic = isPublic;
        this.memberCapacity = memberCapacity;
        this.bankBalance = bankBalance;
        this.bankCapacity = bankCapacity;
        this.cp = cp;
        this.clanLevel = clanLevel;
        this.storageLevel = storageLevel;
        this.warpCapacity = warpCapacity;
        this.allianceCapacity = allianceCapacity;
        this.createdAt = createdAt;
    }

    public boolean tryLock() {
        return transactionLock.compareAndSet(false, true);
    }

    public void unlock() {
        transactionLock.set(false);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public int getMemberCapacity() {
        return memberCapacity;
    }

    public void setMemberCapacity(int memberCapacity) {
        this.memberCapacity = memberCapacity;
    }

    public double getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(double bankBalance) {
        this.bankBalance = bankBalance;
    }

    public double getBankCapacity() {
        return bankCapacity;
    }

    public void setBankCapacity(double bankCapacity) {
        this.bankCapacity = bankCapacity;
    }

    public long getCp() {
        return cp;
    }

    public void setCp(long cp) {
        this.cp = cp;
    }

    public int getClanLevel() {
        return clanLevel;
    }

    public void setClanLevel(int clanLevel) {
        this.clanLevel = clanLevel;
    }

    public int getStorageLevel() {
        return storageLevel;
    }

    public void setStorageLevel(int storageLevel) {
        this.storageLevel = storageLevel;
    }

    public int getWarpCapacity() {
        return warpCapacity;
    }

    public void setWarpCapacity(int warpCapacity) {
        this.warpCapacity = warpCapacity;
    }

    public int getAllianceCapacity() {
        return allianceCapacity;
    }

    public void setAllianceCapacity(int allianceCapacity) {
        this.allianceCapacity = allianceCapacity;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
