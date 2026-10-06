package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.ClanAlliance;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class AlliancesGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private List<ClanAlliance> currentEntries;
    private Inventory inventory;

    public AlliancesGuiHolder(UUID ownerUuid, UUID clanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public void setCurrentEntries(List<ClanAlliance> entries) {
        this.currentEntries = entries;
    }

    public List<ClanAlliance> getCurrentEntries() {
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
