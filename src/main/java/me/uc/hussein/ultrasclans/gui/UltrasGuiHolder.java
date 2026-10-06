package me.uc.hussein.ultrasclans.gui;

import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

/**
 * كل واجهة (GUI) في البلاجن يجب أن تنفّذ هذه الواجهة، بحيث يستطيع
 * GuiClickListener التحقق من هوية الواجهة ومالكها قبل تنفيذ أي إجراء
 * (قسم 56: "لا تعتمد فقط على اسم الـGUI" - Holder + ownerUuid + state).
 */
public interface UltrasGuiHolder extends InventoryHolder {

    /** اللاعب الذي فُتحت له هذه الواجهة (لمنع استغلالها من لاعب آخر عبر exploits). */
    UUID getOwnerUuid();
}
