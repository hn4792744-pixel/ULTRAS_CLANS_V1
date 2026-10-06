package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.event.LocationType;
import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class ChallengeSetupGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private final UUID targetClanId;
    private double moneyWager = 0;
    private long cpWager = 0;
    private LocationType locationType = LocationType.RANDOM_SAFE;
    private int roundDurationMinutes;
    private int totalRounds = 3;
    private Inventory inventory;

    public ChallengeSetupGuiHolder(UUID ownerUuid, UUID clanId, UUID targetClanId, int defaultRoundMinutes) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.targetClanId = targetClanId;
        this.roundDurationMinutes = defaultRoundMinutes;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public UUID getTargetClanId() {
        return targetClanId;
    }

    public double getMoneyWager() {
        return moneyWager;
    }

    public void setMoneyWager(double moneyWager) {
        this.moneyWager = moneyWager;
    }

    public long getCpWager() {
        return cpWager;
    }

    public void setCpWager(long cpWager) {
        this.cpWager = cpWager;
    }

    public LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(LocationType locationType) {
        this.locationType = locationType;
    }

    public int getRoundDurationMinutes() {
        return roundDurationMinutes;
    }

    public void setRoundDurationMinutes(int roundDurationMinutes) {
        this.roundDurationMinutes = roundDurationMinutes;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    @Override
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
