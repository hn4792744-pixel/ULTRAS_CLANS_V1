package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.ClanMember;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class MembersGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private int page;
    private List<ClanMember> currentPageEntries;
    private Inventory inventory;

    public MembersGuiHolder(UUID ownerUuid, UUID clanId, int page) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.page = page;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void setCurrentPageEntries(List<ClanMember> entries) {
        this.currentPageEntries = entries;
    }

    public List<ClanMember> getCurrentPageEntries() {
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
