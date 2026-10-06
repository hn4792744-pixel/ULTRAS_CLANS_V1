package me.uc.hussein.ultrasclans.upgrades;

public enum UpgradeType {
    MEMBER_CAPACITY("member-capacity"),
    BANK_CAPACITY("bank-capacity"),
    STORAGE_LEVEL("storage-level"),
    WARP_CAPACITY("warp-capacity"),
    ALLIANCE_CAPACITY("alliance-capacity"),
    CLAN_LEVEL("clan-level");

    private final String configKey;

    UpgradeType(String configKey) {
        this.configKey = configKey;
    }

    public String configKey() {
        return configKey;
    }
}
