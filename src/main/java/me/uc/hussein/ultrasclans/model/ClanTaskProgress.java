package me.uc.hussein.ultrasclans.model;

public final class ClanTaskProgress {

    public enum Period { DAILY, WEEKLY, MONTHLY }

    private long periodKey;
    private long progress;
    private boolean claimed;

    public ClanTaskProgress(long periodKey, long progress, boolean claimed) {
        this.periodKey = periodKey;
        this.progress = progress;
        this.claimed = claimed;
    }

    public long getPeriodKey() { return periodKey; }
    public void setPeriodKey(long periodKey) { this.periodKey = periodKey; }
    public long getProgress() { return progress; }
    public void setProgress(long progress) { this.progress = progress; }
    public boolean isClaimed() { return claimed; }
    public void setClaimed(boolean claimed) { this.claimed = claimed; }
}
