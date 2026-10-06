package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.Clan;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class ClansListGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private int page;
    private String searchQuery;
    private List<Clan> currentPageEntries;
    private Inventory inventory;

    public ClansListGuiHolder(UUID ownerUuid, int page, String searchQuery) {
        this.ownerUuid = ownerUuid;
        this.page = page;
        this.searchQuery = searchQuery;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setCurrentPageEntries(List<Clan> entries) {
        this.currentPageEntries = entries;
    }

    public List<Clan> getCurrentPageEntries() {
        return currentPageEntries;
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
