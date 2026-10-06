package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class TopGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private Inventory inventory;

    public TopGuiHolder(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    public Inventory getInventory() { return inventory; }
}
