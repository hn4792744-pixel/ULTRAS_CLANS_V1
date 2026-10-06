package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.JoinRequest;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class RequestsGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private List<JoinRequest> currentEntries;
    private Inventory inventory;

    public RequestsGuiHolder(UUID ownerUuid, UUID clanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public void setCurrentEntries(List<JoinRequest> entries) {
        this.currentEntries = entries;
    }

    public List<JoinRequest> getCurrentEntries() {
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
