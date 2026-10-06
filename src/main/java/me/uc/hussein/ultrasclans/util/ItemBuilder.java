package me.uc.hussein.ultrasclans.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * أداة بناء ItemStack مخصص للاستخدام في واجهات (GUI) البلاجن.
 */
public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack base) {
        this.item = base.clone();
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder skullOf(OfflinePlayer owner) {
        ItemBuilder builder = new ItemBuilder(Material.PLAYER_HEAD);
        if (builder.meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(owner);
        }
        return builder;
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null && lines != null) {
            List<String> colored = new ArrayList<>();
            for (String line : lines) {
                colored.add(ColorUtil.colorize(line));
            }
            meta.setLore(colored);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(List.of(lines));
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, amount));
        return this;
    }

    /** وهج (enchant glint) بدون إضافة سحر فعلي - يُستخدم لإبراز الخيارات المفعّلة. */
    public ItemBuilder glow(boolean enabled) {
        if (meta != null && enabled) {
            meta.setEnchantmentGlintOverride(true);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }
}
