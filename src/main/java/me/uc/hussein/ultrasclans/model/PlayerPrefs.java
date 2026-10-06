package me.uc.hussein.ultrasclans.model;

/**
 * تفضيلات لاعب واحد (قسم الإعدادات الشخصية). كلها تخص صاحبها فقط ولا تُطبَّق على
 * بقية الأعضاء. القيمة الافتراضية: كل شيء مفعّل واللغة = لغة السيرفر (language فارغة).
 */
public record PlayerPrefs(String language, boolean messages, boolean sounds, boolean hud,
                           boolean invites, boolean tpa, boolean alliance,
                           boolean joinRequests, boolean challenges) {

    public static final PlayerPrefs DEFAULT = new PlayerPrefs("", true, true, true, true, true, true, true, true);

    public enum Setting {
        MESSAGES, SOUNDS, HUD, INVITES, TPA, ALLIANCE, JOIN_REQUESTS, CHALLENGES;

        public boolean read(PlayerPrefs p) {
            return switch (this) {
                case MESSAGES -> p.messages();
                case SOUNDS -> p.sounds();
                case HUD -> p.hud();
                case INVITES -> p.invites();
                case TPA -> p.tpa();
                case ALLIANCE -> p.alliance();
                case JOIN_REQUESTS -> p.joinRequests();
                case CHALLENGES -> p.challenges();
            };
        }
    }

    public PlayerPrefs with(Setting setting, boolean value) {
        return switch (setting) {
            case MESSAGES -> new PlayerPrefs(language, value, sounds, hud, invites, tpa, alliance, joinRequests, challenges);
            case SOUNDS -> new PlayerPrefs(language, messages, value, hud, invites, tpa, alliance, joinRequests, challenges);
            case HUD -> new PlayerPrefs(language, messages, sounds, value, invites, tpa, alliance, joinRequests, challenges);
            case INVITES -> new PlayerPrefs(language, messages, sounds, hud, value, tpa, alliance, joinRequests, challenges);
            case TPA -> new PlayerPrefs(language, messages, sounds, hud, invites, value, alliance, joinRequests, challenges);
            case ALLIANCE -> new PlayerPrefs(language, messages, sounds, hud, invites, tpa, value, joinRequests, challenges);
            case JOIN_REQUESTS -> new PlayerPrefs(language, messages, sounds, hud, invites, tpa, alliance, value, challenges);
            case CHALLENGES -> new PlayerPrefs(language, messages, sounds, hud, invites, tpa, alliance, joinRequests, value);
        };
    }

    public PlayerPrefs withLanguage(String newLanguage) {
        return new PlayerPrefs(newLanguage, messages, sounds, hud, invites, tpa, alliance, joinRequests, challenges);
    }
}
