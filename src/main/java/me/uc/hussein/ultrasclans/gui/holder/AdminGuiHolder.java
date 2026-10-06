package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class AdminGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final int page;
    private List<Clan> currentEntries;
    private Inventory inventory;

    public AdminGuiHolder(UUID ownerUuid, int page) {
        this.ownerUuid = ownerUuid;
        this.page = page;
    }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public int getPage() { return page; }
    public void setCurrentEntries(List<Clan> entries) { this.currentEntries = entries; }
    public List<Clan> getCurrentEntries() { return currentEntries; }

    @Override
    public UUID getOwnerUuid() { return ownerUuid; }

    @Override
    public Inventory getInventory() { return inventory; }
}
