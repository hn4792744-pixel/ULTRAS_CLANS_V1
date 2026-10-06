package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class AllianceBrowseGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private List<Clan> currentEntries;
    private Inventory inventory;

    public AllianceBrowseGuiHolder(UUID ownerUuid, UUID clanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public void setCurrentEntries(List<Clan> entries) {
        this.currentEntries = entries;
    }

    public List<Clan> getCurrentEntries() {
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
