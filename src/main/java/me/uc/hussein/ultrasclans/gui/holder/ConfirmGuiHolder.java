package me.uc.hussein.ultrasclans.gui.holder;

import me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
import org.bukkit.inventory.Inventory;

import java.util.UUID;

/**
 * واجهة تأكيد عامة قابلة لإعادة الاستخدام لأي عملية حذف حساسة (قسم 57).
 * action يحدد ما الذي سيُنفَّذ عند الضغط "نعم"، وcontextId يحمل أي معرّف
 * إضافي لازم (مثال: رقم الـwarp).
 */
public final class ConfirmGuiHolder implements UltrasGuiHolder {

    public enum Action { DELETE_WARP, DISBAND_ALLIANCE, ADMIN_RESET_CLAN, ADMIN_DELETE_CLAN }

    private final UUID ownerUuid;
    private final UUID clanId;
    private final Action action;
    private final int contextId;
    private final UUID contextUuid;
    private Inventory inventory;

    public ConfirmGuiHolder(UUID ownerUuid, UUID clanId, Action action, int contextId) {
        this(ownerUuid, clanId, action, contextId, null);
    }

    public ConfirmGuiHolder(UUID ownerUuid, UUID clanId, Action action, int contextId, UUID contextUuid) {
        this.ownerUuid = ownerUuid;
        this.clanId = clanId;
        this.action = action;
        this.contextId = contextId;
        this.contextUuid = contextUuid;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public UUID getClanId() {
        return clanId;
    }

    public Action getAction() {
        return action;
    }

    public int getContextId() {
        return contextId;
    }

    public UUID getContextUuid() {
        return contextUuid;
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
