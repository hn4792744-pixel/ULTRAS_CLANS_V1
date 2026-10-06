package me.uc.hussein.ultrasclans.upgrades;

public final class UpgradeResult {

    public enum Status { SUCCESS, NOT_ENOUGH_CP, MAX_REACHED, NO_PERMISSION }

    private final Status status;
    private final long cost;
    private final String newValueDisplay;

    private UpgradeResult(Status status, long cost, String newValueDisplay) {
        this.status = status;
        this.cost = cost;
        this.newValueDisplay = newValueDisplay;
    }

    public static UpgradeResult success(long cost, String newValueDisplay) {
        return new UpgradeResult(Status.SUCCESS, cost, newValueDisplay);
    }

    public static UpgradeResult fail(Status status, long cost) {
        return new UpgradeResult(status, cost, null);
    }

    public Status getStatus() {
        return status;
    }

    public long getCost() {
        return cost;
    }

    public String getNewValueDisplay() {
        return newValueDisplay;
    }
}
