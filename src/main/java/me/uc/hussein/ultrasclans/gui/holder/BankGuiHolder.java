package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class BankGuiHolder implements UltrasGuiHolder {

    public enum Mode { DEPOSIT, WITHDRAW }

    private final UUID ownerUuid;
    private final UUID clanId;
    private final Mode mode;
    private Inventory inventory;

    public BankGuiHolder(UUID ownerUuid, UUID clanId, Mode mode) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.mode = mode;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public Mode getMode() {
        return mode;
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
