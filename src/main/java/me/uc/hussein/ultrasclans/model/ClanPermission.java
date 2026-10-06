package me.uc.hussein.ultrasclans.model;

/**
 * نظام صلاحيات داخلي خاص بالكلان (منفصل تمامًا عن صلاحيات Bukkit).
 * هذه الصلاحيات تُفحص دائمًا عبر PermissionService على جانب الخادم،
 * ولا يُعتمد أبدًا على حالة الـGUI أو الـclient لاتخاذ القرار.
 */
public enum ClanPermission {
    CLAN_VIEW,
    CLAN_INVITE,
    CLAN_KICK,
    CLAN_BAN,
    CLAN_MUTE,
    CLAN_PROMOTE,
    CLAN_DEMOTE,
    CLAN_MANAGE_RANKS,
    CLAN_MANAGE_PERMISSIONS,
    CLAN_ACCEPT_REQUEST,
    CLAN_BANK_DEPOSIT,
    CLAN_BANK_WITHDRAW,
    CLAN_BANK_UPGRADE,
    CLAN_STORAGE_VIEW,
    CLAN_STORAGE_DEPOSIT,
    CLAN_STORAGE_WITHDRAW,
    CLAN_STORAGE_UPGRADE,
    CLAN_WARP_USE,
    CLAN_WARP_CREATE,
    CLAN_WARP_DELETE,
    CLAN_SPAWN_USE,
    CLAN_SPAWN_SET,
    CLAN_ALLIANCE_VIEW,
    CLAN_ALLIANCE_MANAGE,
    CLAN_EVENT_CREATE,
    CLAN_EVENT_ACCEPT,
    CLAN_EVENT_CANCEL,
    CLAN_SETTINGS,
    CLAN_CHAT,
    CLAN_TAG,
    CLAN_UPGRADES,
    CLAN_TASKS,
    CLAN_STATS
}
