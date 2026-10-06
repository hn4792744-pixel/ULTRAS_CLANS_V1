package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class ClanInfoGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    /** true إذا فُتحت هذه الواجهة من /clans (للسماح بزر Join)؛ false إذا من سياق آخر. */
    private final boolean fromBrowser;
    private Inventory inventory;

    public ClanInfoGuiHolder(UUID ownerUuid, UUID clanId, boolean fromBrowser) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.fromBrowser = fromBrowser;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public boolean isFromBrowser() {
        return fromBrowser;
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
