package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.event.ClanEvent;
import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class EventsGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private List<ClanEvent> currentEntries;
    private Inventory inventory;

    public EventsGuiHolder(UUID ownerUuid, UUID clanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public UUID getClanId() { return clanId; }
    public void setCurrentEntries(List<ClanEvent> entries) { this.currentEntries = entries; }
    public List<ClanEvent> getCurrentEntries() { return currentEntries; }

    @Override
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    public Inventory getInventory() { return inventory; }
}
