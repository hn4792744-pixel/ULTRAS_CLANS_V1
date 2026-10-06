package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import me.uc.hussein.ultrasclans.model.ClanInvite;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

public final class InvitesGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private List<ClanInvite> currentEntries;
    private Inventory inventory;

    public InvitesGuiHolder(UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void setCurrentEntries(List<ClanInvite> entries) {
        this.currentEntries = entries;
    }

    public List<ClanInvite> getCurrentEntries() {
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
