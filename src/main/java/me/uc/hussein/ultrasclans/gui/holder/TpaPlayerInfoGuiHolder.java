package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class TpaPlayerInfoGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID targetUuid;
    private Inventory inventory;

    public TpaPlayerInfoGuiHolder(UUID ownerUuid, UUID targetUuid) {
        this.ownerUuid = ownerUuid;
        this.targetUuid = targetUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getTargetUuid() {
        return targetUuid;
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
