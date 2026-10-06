package me.uc.hussein.ultrasclans.model;

import java.util.Set;
import java.util.UUID;

/**
 * رتبة داخلية قابلة للتخصيص داخل كلان معيّن (وليست تلقائية كـ ClanTier).
 * راجع ranks.yml (member-ranks) للقالب الافتراضي.
 */
public final class ClanRankDefinition {

    private final UUID clanId;
    private final String rankKey;
    private String name;
    private String display;
    private String color;
    private int priority;
    private final boolean protectedRank;
    private boolean defaultRank;
    private Set<String> permissions;

    public ClanRankDefinition(UUID clanId, String rankKey, String name, String display, String color,
                               int priority, boolean protectedRank, boolean defaultRank,
                               Set<String> permissions) {
        this.clanId = clanId;
        this.rankKey = rankKey;
        this.name = name;
        this.display = display;
        this.color = color;
        this.priority = priority;
        this.protectedRank = protectedRank;
        this.defaultRank = defaultRank;
        this.permissions = permissions;
    }

    public boolean hasPermission(String permission) {
        return permissions.contains("*") || permissions.contains(permission);
    }

    public UUID getClanId() {
        return clanId;
    }

    public String getRankKey() {
        return rankKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isProtectedRank() {
        return protectedRank;
    }

    public boolean isDefaultRank() {
        return defaultRank;
    }

    public void setDefaultRank(boolean defaultRank) {
        this.defaultRank = defaultRank;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
