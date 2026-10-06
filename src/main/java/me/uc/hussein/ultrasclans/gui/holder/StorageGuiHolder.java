package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class StorageGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private final int unlockedSlots;
    private Inventory inventory;

    public StorageGuiHolder(UUID ownerUuid, UUID clanId, int unlockedSlots) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.unlockedSlots = unlockedSlots;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public int getUnlockedSlots() {
        return unlockedSlots;
    }

    public boolean isUnlocked(int slot) {
        return slot < unlockedSlots;
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
