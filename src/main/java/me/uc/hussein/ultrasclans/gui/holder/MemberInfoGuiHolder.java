package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class MemberInfoGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private final UUID targetUuid;
    private Inventory inventory;

    public MemberInfoGuiHolder(UUID ownerUuid, UUID clanId, UUID targetUuid) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.targetUuid = targetUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
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
