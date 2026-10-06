package me.uc.hussein.ultrasclans.event;

import org.bukkit.Location;

/**
 * الساحة الإدارية الوحيدة للسيرفر (قسم 34): نقطتا سبون الفريقين،
 * ونقطتا تحديد منطقة الاستعادة (rest1/rest2 كزاويتين متقابلتين لمكعّب).
 */
public final class AdminArena {

    private Location event1;
    private Location event2;
    private Location rest1;
    private Location rest2;

    public boolean isFullyConfigured() {
        return event1 != null && event2 != null && rest1 != null && rest2 != null;
    }

    public Location getEvent1() { return event1; }
    public void setEvent1(Location event1) { this.event1 = event1; }
    public Location getEvent2() { return event2; }
    public void setEvent2(Location event2) { this.event2 = event2; }
    public Location getRest1() { return rest1; }
    public void setRest1(Location rest1) { this.rest1 = rest1; }
    public Location getRest2() { return rest2; }
    public void setRest2(Location rest2) { this.rest2 = rest2; }
}
