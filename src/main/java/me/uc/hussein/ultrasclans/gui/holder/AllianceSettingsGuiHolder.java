package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

public final class AllianceSettingsGuiHolder implements UltrasGuiHolder {

    private final UUID ownerUuid;
    private final UUID clanId;
    private final UUID allianceId;
    private final UUID otherClanId;
    private Inventory inventory;

    public AllianceSettingsGuiHolder(UUID ownerUuid, UUID clanId, UUID allianceId, UUID otherClanId) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.allianceId = allianceId;
        this.otherClanId = otherClanId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public UUID getAllianceId() {
        return allianceId;
    }

    public UUID getOtherClanId() {
        return otherClanId;
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
