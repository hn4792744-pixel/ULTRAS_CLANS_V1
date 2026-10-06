package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class TpaGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private List<Player> currentEntries;
    private Inventory inventory;

    public TpaGuiHolder(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setCurrentEntries(List<Player> entries) {
        this.currentEntries = entries;
    }

    public List<Player> getCurrentEntries() {
        return currentEntries;
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
