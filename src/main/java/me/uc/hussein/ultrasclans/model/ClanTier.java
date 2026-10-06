package me.uc.hussein.ultrasclans.model;

/**
 * مستوى تلقائي للكلان محسوب من الـCP الحالي (Bronze/Silver/Gold...).
 * يُحمّل من ranks.yml (قسم tiers) وغير قابل للتعديل يدويًا لكلان معيّن -
 * هو نتيجة حساب فقط. راجع TierService في المرحلة الثانية.
 */
public final class ClanTier {

    private final String id;
    private final String name;
    private final String display;
    private final String symbol;
    private final long minCp;
    private final long maxCp; // -1 تعني بلا حد أعلى

    public ClanTier(String id, String name, String display, String symbol, long minCp, long maxCp) {
        this.id = id;
        this.name = name;
        this.display = display;
        this.symbol = symbol;
        this.minCp = minCp;
        this.maxCp = maxCp;
    }

    public boolean matches(long cp) {
        return cp >= minCp && (maxCp == -1 || cp <= maxCp);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }

    public String getSymbol() {
        return symbol;
    }

    public long getMinCp() {
        return minCp;
    }

    public long getMaxCp() {
        return maxCp;
    }
}
