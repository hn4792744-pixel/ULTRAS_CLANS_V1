package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.holder.SettingsGuiHolder;
import me.uc.hussein.ultrasclans.message.MessageManager;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.PlayerPrefs;
import me.uc.hussein.ultrasclans.model.PlayerPrefs.Setting;
import me.uc.hussein.ultrasclans.util.ItemBuilder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * واجهة الإعدادات الشخصية (/clansettings). كل خيار هنا يخص اللاعب نفسه فقط ولا يمس
 * بقية الأعضاء: اللغة، رسائل الشات، الأصوات، الأكشن بار، وإيقاف استقبال الدعوات
 * والطلبات. كل زر يتحقق أولًا (كلان؟ صلاحية؟) ثم يُنفَّذ ثم تنقلب قيمته ON↔OFF.
 */
public final class SettingsGui {

    public static final int SIZE = 45;

    private enum Action {
        LANGUAGE, MESSAGES, SOUNDS, HUD, INVITES, TPA, ALLIANCE, JOIN, CHALLENGES, CHAT, TAG, RESET, BACK, CLOSE
    }

    private static final Map<Integer, Action> SLOTS = Map.ofEntries(
            Map.entry(10, Action.LANGUAGE), Map.entry(12, Action.MESSAGES),
            Map.entry(14, Action.SOUNDS), Map.entry(16, Action.HUD),
            Map.entry(20, Action.INVITES), Map.entry(21, Action.TPA), Map.entry(22, Action.ALLIANCE),
            Map.entry(23, Action.JOIN), Map.entry(24, Action.CHALLENGES),
            Map.entry(30, Action.CHAT), Map.entry(32, Action.TAG),
            Map.entry(36, Action.BACK), Map.entry(40, Action.RESET), Map.entry(44, Action.CLOSE));

    private final UltrasClansPlugin plugin;

    public SettingsGui(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ عرض

    public void open(Player player) {
        var ms = plugin.getMessageService();
        SettingsGuiHolder holder = new SettingsGuiHolder(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(holder, SIZE, ms.text(player, "settings.title"));
        holder.setInventory(inv);

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(4, infoItem(player));
        for (var entry : SLOTS.entrySet()) {
            inv.setItem(entry.getKey(), itemFor(player, entry.getValue()));
        }
        player.openInventory(inv);
    }

    private ItemStack infoItem(Player player) {
        var ms = plugin.getMessageService();
        UUID uuid = player.getUniqueId();
        String clanName = plugin.getClanService().getPlayerClan(uuid).map(c -> c.getName()).orElse("-");
        int enabled = 0;
        for (Setting s : Setting.values()) {
            if (s.read(plugin.getPrefsService().get(uuid))) enabled++;
        }
        String lore = ms.text(player, "settings.info-lore", Map.of(
                "clan", clanName,
                "language", ms.text(player, "settings.language-" + ms.languageOf(uuid)),
                "enabled", String.valueOf(enabled),
                "total", String.valueOf(Setting.values().length)));
        return ItemBuilder.skullOf(player)
                .name(ms.text(player, "settings.info-name", Map.of("player", player.getName())))
                .lore(split(lore)).build();
    }

    private ItemStack itemFor(Player player, Action action) {
        var ms = plugin.getMessageService();
        return switch (action) {
            case LANGUAGE -> languageItem(player);
            case RESET -> new ItemBuilder(Material.REDSTONE_BLOCK)
                    .name(ms.text(player, "settings.reset-name"))
                    .lore(split(ms.text(player, "settings.reset-desc"))).build();
            case BACK -> new ItemBuilder(Material.ARROW).name(ms.text(player, "settings.back")).build();
            case CLOSE -> new ItemBuilder(Material.BARRIER).name(ms.text(player, "settings.close")).build();
            default -> toggleItem(player, action);
        };
    }

    private ItemStack languageItem(Player player) {
        var ms = plugin.getMessageService();
        String current = ms.languageOf(player.getUniqueId());
        List<String> lore = new java.util.ArrayList<>(split(ms.text(player, "settings.language-desc")));
        lore.add("");
        for (String lang : MessageManager.LANGUAGES) {
            String label = ms.text(player, "settings.language-" + lang);
            lore.add((lang.equals(current) ? "&a▸ " : "&8  ") + label);
        }
        lore.add("");
        lore.add(ms.text(player, "settings.scope"));
        lore.add(ms.text(player, "settings.click-cycle"));
        return new ItemBuilder(Material.BOOK)
                .name("&f" + ms.text(player, "settings.language-name") + " &8» "
                        + ms.text(player, "settings.language-" + current))
                .lore(lore).build();
    }

    private ItemStack toggleItem(Player player, Action action) {
        var ms = plugin.getMessageService();
        String id = idOf(action);
        boolean on = stateOf(player, action);
        String lock = lockReason(player, action);
        String name = ms.text(player, "settings." + id + "-name");

        List<String> lore = new java.util.ArrayList<>(split(ms.text(player, "settings." + id + "-desc")));
        lore.add("");
        lore.add(ms.text(player, "settings.scope"));
        if (lock != null) {
            lore.add(ms.text(player, lock));
            return new ItemBuilder(Material.GRAY_DYE)
                    .name("&7" + name + " " + ms.text(player, "settings.locked-title"))
                    .lore(lore).build();
        }
        lore.add(ms.text(player, "settings.click-toggle"));
        return new ItemBuilder(materialOf(action))
                .name("&f" + name + " &8» " + ms.text(player, on ? "settings.on" : "settings.off"))
                .lore(lore).glow(on).build();
    }

    // ------------------------------------------------------------ منطق

    private String idOf(Action a) {
        return switch (a) {
            case MESSAGES -> "messages";
            case SOUNDS -> "sounds";
            case HUD -> "hud";
            case INVITES -> "invites";
            case TPA -> "tpa";
            case ALLIANCE -> "alliance";
            case JOIN -> "join";
            case CHALLENGES -> "challenges";
            case CHAT -> "chat";
            case TAG -> "tag";
            default -> "language";
        };
    }

    private Material materialOf(Action a) {
        return switch (a) {
            case MESSAGES -> Material.WRITABLE_BOOK;
            case SOUNDS -> Material.NOTE_BLOCK;
            case HUD -> Material.CLOCK;
            case INVITES -> Material.PAPER;
            case TPA -> Material.ENDER_PEARL;
            case ALLIANCE -> Material.WHITE_BANNER;
            case JOIN -> Material.OAK_DOOR;
            case CHALLENGES -> Material.DIAMOND_SWORD;
            case CHAT -> Material.OAK_SIGN;
            default -> Material.NAME_TAG;
        };
    }

    private Setting settingOf(Action a) {
        return switch (a) {
            case MESSAGES -> Setting.MESSAGES;
            case SOUNDS -> Setting.SOUNDS;
            case HUD -> Setting.HUD;
            case INVITES -> Setting.INVITES;
            case TPA -> Setting.TPA;
            case ALLIANCE -> Setting.ALLIANCE;
            case JOIN -> Setting.JOIN_REQUESTS;
            case CHALLENGES -> Setting.CHALLENGES;
            default -> null;
        };
    }

    private boolean stateOf(Player player, Action action) {
        UUID uuid = player.getUniqueId();
        return switch (action) {
            case CHAT -> plugin.getChatService().isClanChatEnabled(uuid);
            case TAG -> plugin.getChatService().isTagEnabled(uuid);
            default -> {
                Setting setting = settingOf(action);
                yield setting != null && setting.read(plugin.getPrefsService().get(uuid));
            }
        };
    }

    private boolean hasClanPermission(Player player, ClanPermission permission) {
        return plugin.getPermissionService().isOwner(player.getUniqueId())
                || plugin.getPermissionService().hasPermission(player.getUniqueId(), permission);
    }

    /** @return مفتاح رسالة سبب القفل، أو null إن كان الخيار متاحًا للاعب الآن. */
    private String lockReason(Player player, Action action) {
        boolean inClan = plugin.getClanService().isInClan(player.getUniqueId());
        return switch (action) {
            case TPA, TAG -> inClan ? null : "settings.locked-clan";
            case CHAT -> !inClan ? "settings.locked-clan"
                    : (hasClanPermission(player, ClanPermission.CLAN_CHAT) ? null : "settings.locked-permission");
            case ALLIANCE -> gate(inClan, player, ClanPermission.CLAN_ALLIANCE_MANAGE);
            case JOIN -> gate(inClan, player, ClanPermission.CLAN_ACCEPT_REQUEST);
            case CHALLENGES -> gate(inClan, player, ClanPermission.CLAN_EVENT_ACCEPT);
            default -> null;
        };
    }

    private String gate(boolean inClan, Player player, ClanPermission permission) {
        if (!inClan) return "settings.locked-clan";
        return hasClanPermission(player, permission) ? null : "settings.locked-permission";
    }

    // ------------------------------------------------------------ نقرات

    public void handleClick(Player player, int slot) {
        Action action = SLOTS.get(slot);
        if (action == null) {
            return;
        }
        var ms = plugin.getMessageService();
        UUID uuid = player.getUniqueId();

        switch (action) {
            case CLOSE -> player.closeInventory();
            case BACK -> plugin.getGuiManager().openClanRoot(player);
            case RESET -> {
                plugin.getPrefsService().reset(uuid);
                plugin.playSound(player, "click");
                ms.send(player, "settings.reset-done");
                open(player);
            }
            case LANGUAGE -> {
                List<String> langs = MessageManager.LANGUAGES;
                int idx = langs.indexOf(ms.languageOf(uuid));
                plugin.getPrefsService().setLanguage(uuid, langs.get((idx + 1) % langs.size()));
                plugin.playSound(player, "click");
                open(player); // يُعاد رسمها باللغة الجديدة فورًا
            }
            default -> toggle(player, action);
        }
    }

    private void toggle(Player player, Action action) {
        var ms = plugin.getMessageService();
        UUID uuid = player.getUniqueId();

        String lock = lockReason(player, action);
        if (lock != null) {
            ms.send(player, lock);
            plugin.playSound(player, "error");
            return;
        }

        boolean now = switch (action) {
            case CHAT -> plugin.getChatService().toggleClanChat(uuid);
            case TAG -> plugin.getChatService().toggleTag(uuid);
            default -> plugin.getPrefsService().toggle(uuid, settingOf(action));
        };

        if (plugin.getPrefsService().get(uuid).hud()) {
            String text = ms.text(player, "settings.changed", Map.of(
                    "name", ms.text(player, "settings." + idOf(action) + "-name"),
                    "state", ms.text(player, now ? "settings.on" : "settings.off")));
            player.sendActionBar(LegacyComponentSerializer.legacySection().deserialize(text));
        }
        plugin.playSound(player, "click");
        open(player);
    }

    private List<String> split(String text) {
        return Arrays.asList(text.split("\\|", -1));
    }
}
