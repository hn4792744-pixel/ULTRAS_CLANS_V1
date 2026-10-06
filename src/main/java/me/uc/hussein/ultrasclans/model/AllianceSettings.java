package me.uc.hussein.ultrasclans.model;

/**
 * ما يسمح به كلان واحد لحليفه (اتجاه واحد فقط - كل كلان له سطر مستقل).
 * الافتراضي بالكامل false (لا صلاحيات) عدا منع PvP الذي يكون true
 * افتراضيًا (قسم 27: "Alliance = no access" افتراضيًا، وPrevent PvP
 * منطقي أن يكون مفعّلًا بشكل افتراضي بين حلفاء جدد).
 */
public final class AllianceSettings {

    private boolean allowSpawn;
    private boolean allowWarp;
    private boolean allowStorage;
    private boolean allowBank;
    private boolean allowMemberList;
    private boolean allowClanInfo;
    private boolean preventPvp;

    public AllianceSettings(boolean allowSpawn, boolean allowWarp, boolean allowStorage, boolean allowBank,
                             boolean allowMemberList, boolean allowClanInfo, boolean preventPvp) {
        this.allowSpawn = allowSpawn;
        this.allowWarp = allowWarp;
        this.allowStorage = allowStorage;
        this.allowBank = allowBank;
        this.allowMemberList = allowMemberList;
        this.allowClanInfo = allowClanInfo;
        this.preventPvp = preventPvp;
    }

    public static AllianceSettings defaults() {
        return new AllianceSettings(false, false, false, false, false, false, true);
    }

    public boolean isAllowSpawn() { return allowSpawn; }
    public void setAllowSpawn(boolean v) { this.allowSpawn = v; }
    public boolean isAllowWarp() { return allowWarp; }
    public void setAllowWarp(boolean v) { this.allowWarp = v; }
    public boolean isAllowStorage() { return allowStorage; }
    public void setAllowStorage(boolean v) { this.allowStorage = v; }
    public boolean isAllowBank() { return allowBank; }
    public void setAllowBank(boolean v) { this.allowBank = v; }
    public boolean isAllowMemberList() { return allowMemberList; }
    public void setAllowMemberList(boolean v) { this.allowMemberList = v; }
    public boolean isAllowClanInfo() { return allowClanInfo; }
    public void setAllowClanInfo(boolean v) { this.allowClanInfo = v; }
    public boolean isPreventPvp() { return preventPvp; }
    public void setPreventPvp(boolean v) { this.preventPvp = v; }
}
