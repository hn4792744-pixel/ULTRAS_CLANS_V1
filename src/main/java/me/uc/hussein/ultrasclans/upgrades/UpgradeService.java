package me.uc.hussein.ultrasclans.upgrades;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * يدير كل الترقيات المدفوعة بـCP (قسم 30). كل عملية شراء تتحقق من
 * الصلاحية، تحسب التكلفة من upgrades.yml، تخصم CP عبر PointsService
 * (الذي يُحدّث الـTier تلقائيًا كأثر جانبي إن انخفض CP تحت عتبة)، ثم
 * تُطبِّق التغيير فعليًا وتحفظه.
 */
public final class UpgradeService {

    private final UltrasClansPlugin plugin;

    public UpgradeService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration config() {
        return plugin.getConfigManager().loadYaml("upgrades.yml");
    }

    public void attemptUpgrade(Player player, Clan clan, UpgradeType type, Consumer<UpgradeResult> callback) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_UPGRADES)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.NO_PERMISSION, 0));
            return;
        }

        FileConfiguration cfg = config();

        if (type == UpgradeType.STORAGE_LEVEL) {
            handleStorageUpgrade(clan, cfg, callback);
            return;
        }

        var section = cfg.getConfigurationSection(type.configKey());
        if (section == null) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.MAX_REACHED, 0));
            return;
        }

        long baseCost = section.getLong("base-cost", 1000);
        long costIncrement = section.getLong("cost-increment", 500);
        long increasePerPurchase = Math.max(1, section.getLong("increase-per-purchase", 1));
        long maxCapacity = section.getLong("max-capacity", section.getLong("max-level", Long.MAX_VALUE));

        long currentValue = currentValueOf(clan, type);
        long defaultValue = defaultValueOf(clan, type);
        long purchasesSoFar = Math.max(0, (currentValue - defaultValue) / increasePerPurchase);
        long cost = baseCost + (purchasesSoFar * costIncrement);

        if (currentValue >= maxCapacity) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.MAX_REACHED, cost));
            return;
        }

        boolean spent = plugin.getPointsService().spendCp(clan, cost);
        if (!spent) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.NOT_ENOUGH_CP, cost));
            return;
        }

        long newValue;
        synchronized (clan) {
            newValue = applyIncrease(clan, type, increasePerPurchase);
        }
        persistAndLog(clan, player.getUniqueId(), type, cost, currentValue, newValue);
        callback.accept(UpgradeResult.success(cost, String.valueOf(newValue)));
    }

    private void handleStorageUpgrade(Clan clan, FileConfiguration cfg, Consumer<UpgradeResult> callback) {
        var section = cfg.getConfigurationSection("storage-level");
        int maxLevel = section != null ? section.getInt("max-level", 10) : 10;
        List<Integer> costs = section != null ? section.getIntegerList("costs") : List.of();

        int currentLevel = clan.getStorageLevel();
        if (currentLevel >= maxLevel) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.MAX_REACHED, 0));
            return;
        }
        int index = currentLevel - 1;
        long cost = (index >= 0 && index < costs.size()) ? costs.get(index) : 5000L * (currentLevel);

        boolean spent = plugin.getPointsService().spendCp(clan, cost);
        if (!spent) {
            callback.accept(UpgradeResult.fail(UpgradeResult.Status.NOT_ENOUGH_CP, cost));
            return;
        }

        int newLevel;
        synchronized (clan) {
            newLevel = currentLevel + 1;
            clan.setStorageLevel(newLevel);
        }
        persistAndLog(clan, null, UpgradeType.STORAGE_LEVEL, cost, currentLevel, newLevel);
        callback.accept(UpgradeResult.success(cost, String.valueOf(newLevel)));
    }

    private long currentValueOf(Clan clan, UpgradeType type) {
        return switch (type) {
            case MEMBER_CAPACITY -> clan.getMemberCapacity();
            case BANK_CAPACITY -> (long) clan.getBankCapacity();
            case WARP_CAPACITY -> clan.getWarpCapacity();
            case ALLIANCE_CAPACITY -> clan.getAllianceCapacity();
            case CLAN_LEVEL -> clan.getClanLevel();
            case STORAGE_LEVEL -> clan.getStorageLevel();
        };
    }

    private long defaultValueOf(Clan clan, UpgradeType type) {
        var defaults = plugin.getConfigManager().get().getConfigurationSection("clan-defaults");
        if (defaults == null) {
            return 0;
        }
        return switch (type) {
            case MEMBER_CAPACITY -> defaults.getLong("member-capacity", 10);
            case BANK_CAPACITY -> (long) defaults.getDouble("bank-capacity", 50000);
            case WARP_CAPACITY -> defaults.getLong("warp-capacity", 1);
            case ALLIANCE_CAPACITY -> defaults.getLong("alliance-capacity", 1);
            case CLAN_LEVEL -> 1;
            case STORAGE_LEVEL -> defaults.getLong("storage-level", 1);
        };
    }

    private long applyIncrease(Clan clan, UpgradeType type, long increasePerPurchase) {
        switch (type) {
            case MEMBER_CAPACITY -> {
                int newVal = clan.getMemberCapacity() + (int) increasePerPurchase;
                clan.setMemberCapacity(newVal);
                return newVal;
            }
            case BANK_CAPACITY -> {
                double newVal = clan.getBankCapacity() + increasePerPurchase;
                clan.setBankCapacity(newVal);
                return (long) newVal;
            }
            case WARP_CAPACITY -> {
                int newVal = clan.getWarpCapacity() + (int) increasePerPurchase;
                clan.setWarpCapacity(newVal);
                return newVal;
            }
            case ALLIANCE_CAPACITY -> {
                int newVal = clan.getAllianceCapacity() + (int) increasePerPurchase;
                clan.setAllianceCapacity(newVal);
                return newVal;
            }
            case CLAN_LEVEL -> {
                int newVal = clan.getClanLevel() + 1;
                clan.setClanLevel(newVal);
                return newVal;
            }
            default -> {
                return 0;
            }
        }
    }

    private void persistAndLog(Clan clan, UUID actorUuid, UpgradeType type, long cost, long before, long after) {
        plugin.getScheduler().runAsync(() -> {
            plugin.getClanService().getClanRepository().update(clan);
            plugin.getClanService().getAuditRepository().log(
                    java.util.UUID.randomUUID().toString(), actorUuid, clan.getId(),
                    "UPGRADE_" + type.name(), String.valueOf(cost), String.valueOf(before), String.valueOf(after), "command");
        });
    }
}
