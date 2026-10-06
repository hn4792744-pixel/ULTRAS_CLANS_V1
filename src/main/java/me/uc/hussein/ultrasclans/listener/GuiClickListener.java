package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.gui.BankGui;
import me.uc.hussein.ultrasclans.gui.PendingInputManager;
import me.uc.hussein.ultrasclans.gui.StorageGui;
import me.uc.hussein.ultrasclans.gui.UpgradesGui;
import me.uc.hussein.ultrasclans.gui.WarpsGui;
import me.uc.hussein.ultrasclans.gui.ConfirmGui;
import me.uc.hussein.ultrasclans.gui.TpaGui;
import me.uc.hussein.ultrasclans.gui.TpaPlayerInfoGui;
import me.uc.hussein.ultrasclans.tpa.TpaRequest;
import me.uc.hussein.ultrasclans.gui.AlliancesGui;
import me.uc.hussein.ultrasclans.gui.AllianceBrowseGui;
import me.uc.hussein.ultrasclans.gui.AllianceInfoGui;
import me.uc.hussein.ultrasclans.gui.AllianceSettingsGui;
import me.uc.hussein.ultrasclans.gui.TasksGui;
import me.uc.hussein.ultrasclans.model.ClanAlliance;
import me.uc.hussein.ultrasclans.event.ClanEvent;
import me.uc.hussein.ultrasclans.event.EventStatus;
import me.uc.hussein.ultrasclans.event.EventService;
import me.uc.hussein.ultrasclans.event.LocationType;
import me.uc.hussein.ultrasclans.gui.ChallengeSetupGui;
import me.uc.hussein.ultrasclans.gui.ChallengeBrowseGui;
import me.uc.hussein.ultrasclans.gui.EventsGui;
import me.uc.hussein.ultrasclans.model.AllianceSettings;
import me.uc.hussein.ultrasclans.model.ClanTaskProgress.Period;
import me.uc.hussein.ultrasclans.gui.holder.*;
import me.uc.hussein.ultrasclans.upgrades.UpgradeType;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanInvite;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.ClanRankDefinition;
import me.uc.hussein.ultrasclans.model.JoinRequest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * المستمع المركزي الوحيد لكل نقرات واجهات البلاجن.
 * قاعدة أمان صارمة (قسم 55/56): كل حدث يُلغى افتراضيًا، ولا يُنفَّذ أي
 * إجراء إلا بعد التحقق من: نوع الـHolder + هوية المالك + الصلاحية على
 * الخادم + عدم تجاوز حد سرعة النقر (ClickThrottle).
 */
public final class GuiClickListener implements Listener {

    private final UltrasClansPlugin plugin;

    public GuiClickListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        // يمنع أي drag (سحب أيتمات عبر عدة slots دفعة واحدة) داخل أي من
        // واجهات البلاجن - حماية من drag exploit (قسم 55/56).
        if (isOurHolder(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }

    private boolean isOurHolder(InventoryHolder holder) {
        return holder instanceof me.uc.hussein.ultrasclans.gui.UltrasGuiHolder;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!isOurHolder(rawHolder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }

        me.uc.hussein.ultrasclans.gui.UltrasGuiHolder holder =
                (me.uc.hussein.ultrasclans.gui.UltrasGuiHolder) rawHolder;

        if (!holder.getOwnerUuid().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return; // لا يمكن لأي لاعب التفاعل مع واجهة لاعب آخر
        }

        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            event.setCancelled(true);
            return; // النقر في envanter اللاعب السفلي - تجاهل تمامًا
        }

        // استثناء وحيد ومحدود جدًا: خانات Storage غير المقفلة تسمح بحركة
        // العنصر فعليًا (وضع/سحب) لأنها مخزن تفاعلي حقيقي (قسم 21). كل شيء
        // آخر (بما فيه double-click على خانات Storage نفسها) يبقى مُلغى.
        boolean allowRealItemMovement = holder instanceof StorageGuiHolder storageHolder
                && storageHolder.isUnlocked(event.getSlot())
                && event.getSlot() < StorageGui.MAX_USABLE_SLOTS
                && event.getAction() != org.bukkit.event.inventory.InventoryAction.COLLECT_TO_CURSOR;

        event.setCancelled(!allowRealItemMovement);

        if (!plugin.getClickThrottle().attempt(player.getUniqueId())) {
            event.setCancelled(true);
            return; // click-spam protection
        }

        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (allowRealItemMovement) {
            plugin.getStorageSessions().markDirty((StorageGuiHolder) holder); // حفظ مؤجل
            return;
        }

        if (holder instanceof ClanMainGuiHolder) {
            handleClanMain(player, slot);
        } else if (holder instanceof MyClanGuiHolder h) {
            handleMyClan(player, h, slot);
        } else if (holder instanceof ClansListGuiHolder h) {
            handleClansList(player, h, slot);
        } else if (holder instanceof ClanInfoGuiHolder h) {
            handleClanInfo(player, h, slot);
        } else if (holder instanceof MembersGuiHolder h) {
            handleMembers(player, h, slot);
        } else if (holder instanceof MemberInfoGuiHolder h) {
            handleMemberInfo(player, h, slot);
        } else if (holder instanceof RequestsGuiHolder h) {
            handleRequests(player, h, slot, click);
        } else if (holder instanceof InvitesGuiHolder h) {
            handleInvites(player, h, slot, click);
        } else if (holder instanceof BankGuiHolder h) {
            handleBank(player, h, slot);
        } else if (holder instanceof StorageGuiHolder h) {
            handleStorage(player, h, slot);
        } else if (holder instanceof UpgradesGuiHolder h) {
            handleUpgrades(player, h, slot);
        } else if (holder instanceof WarpsGuiHolder h) {
            handleWarps(player, h, slot, click);
        } else if (holder instanceof ConfirmGuiHolder h) {
            handleConfirm(player, h, slot);
        } else if (holder instanceof TpaGuiHolder h) {
            handleTpaList(player, h, slot);
        } else if (holder instanceof TpaPlayerInfoGuiHolder h) {
            handleTpaPlayerInfo(player, h, slot);
        } else if (holder instanceof AlliancesGuiHolder h) {
            handleAlliances(player, h, slot);
        } else if (holder instanceof AllianceBrowseGuiHolder h) {
            handleAllianceBrowse(player, h, slot);
        } else if (holder instanceof AllianceInfoGuiHolder h) {
            handleAllianceInfo(player, h, slot);
        } else if (holder instanceof AllianceSettingsGuiHolder h) {
            handleAllianceSettings(player, h, slot);
        } else if (holder instanceof TasksGuiHolder h) {
            handleTasks(player, h, slot);
        } else if (holder instanceof EventsGuiHolder h) {
            handleEvents(player, h, slot, click);
        } else if (holder instanceof ChallengeBrowseGuiHolder h) {
            handleChallengeBrowse(player, h, slot);
        } else if (holder instanceof ChallengeSetupGuiHolder h) {
            handleChallengeSetup(player, h, slot);
        } else if (holder instanceof TopGuiHolder) {
            var category = plugin.getGuiManager().top().categoryAt(slot);
            if (category != null) {
                plugin.getGuiManager().top().open(player, category);
            } else if (slot == me.uc.hussein.ultrasclans.gui.TopGui.SLOT_BACK) {
                plugin.getGuiManager().openClanRoot(player);
            }
        } else if (holder instanceof StatsGuiHolder) {
            if (slot == me.uc.hussein.ultrasclans.gui.StatsGui.SLOT_BACK) {
                plugin.getGuiManager().openClanRoot(player);
            }
        } else if (holder instanceof SettingsGuiHolder) {
            plugin.getSettingsGui().handleClick(player, slot);
        } else if (holder instanceof AdminGuiHolder h) {
            handleAdminList(player, h, slot);
        } else if (holder instanceof AdminClanGuiHolder h) {
            handleAdminClan(player, h, slot);
        }
    }

    // ------------------------------------------------------------ Phase 6 (Admin)

    private boolean denyIfNotAdmin(Player player) {
        if (!player.hasPermission("ultras.clans.admin")) {
            player.closeInventory();
            plugin.getMessageService().send(player, "general.no-permission");
            return true;
        }
        return false;
    }

    private void handleAdminList(Player player, AdminGuiHolder holder, int slot) {
        if (denyIfNotAdmin(player)) return;
        var adminGui = plugin.getAdminGui();
        if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_PREV) {
            adminGui.open(player, holder.getPage() - 1);
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_NEXT) {
            adminGui.open(player, holder.getPage() + 1);
        } else if (holder.getCurrentEntries() != null && slot < holder.getCurrentEntries().size()) {
            adminGui.openClan(player, holder.getCurrentEntries().get(slot));
        }
    }

    private void handleAdminClan(Player player, AdminClanGuiHolder holder, int slot) {
        if (denyIfNotAdmin(player)) return;
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { plugin.getAdminGui().open(player, 0); return; }
        Clan clan = clanOpt.get();
        var adminGui = plugin.getAdminGui();

        if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_BACK) {
            adminGui.open(player, 0);
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_CP_ADD) {
            long before = clan.getCp();
            plugin.getPointsService().addCpForClan(clan, me.uc.hussein.ultrasclans.admin.AdminGui.CP_STEP, "Admin");
            adminAudit(player, clan, "ADMIN_CP_ADD", me.uc.hussein.ultrasclans.admin.AdminGui.CP_STEP, before, clan.getCp());
            adminGui.openClan(player, clan);
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_CP_REMOVE) {
            long before = clan.getCp();
            long take = Math.min(me.uc.hussein.ultrasclans.admin.AdminGui.CP_STEP, before);
            if (take > 0) plugin.getPointsService().spendCp(clan, take);
            adminAudit(player, clan, "ADMIN_CP_REMOVE", take, before, clan.getCp());
            adminGui.openClan(player, clan);
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_BANK_ADD
                || slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_BANK_REMOVE) {
            boolean add = slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_BANK_ADD;
            double before;
            synchronized (clan) {
                before = clan.getBankBalance();
                double step = me.uc.hussein.ultrasclans.admin.AdminGui.BANK_STEP;
                clan.setBankBalance(add ? Math.min(clan.getBankCapacity(), before + step) : Math.max(0, before - step));
            }
            plugin.getClanService().persist(clan);
            adminAudit(player, clan, add ? "ADMIN_BANK_ADD" : "ADMIN_BANK_REMOVE",
                    (long) me.uc.hussein.ultrasclans.admin.AdminGui.BANK_STEP, (long) before, (long) clan.getBankBalance());
            adminGui.openClan(player, clan);
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_INSPECT) {
            player.closeInventory();
            player.performCommand("uc clan admin inspect " + clan.getName());
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_RESET) {
            plugin.getGuiManager().confirm().open(player, clan.getId(), ConfirmGuiHolder.Action.ADMIN_RESET_CLAN, 0,
                    "&6Reset clan " + clan.getName() + "?");
        } else if (slot == me.uc.hussein.ultrasclans.admin.AdminGui.SLOT_DELETE) {
            plugin.getGuiManager().confirm().open(player, clan.getId(), ConfirmGuiHolder.Action.ADMIN_DELETE_CLAN, 0,
                    "&4DELETE clan " + clan.getName() + "?");
        }
    }

    private void adminAudit(Player admin, Clan clan, String action, long amount, long before, long after) {
        String correlation = java.util.UUID.randomUUID().toString();
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getAuditRepository().log(
                correlation, admin.getUniqueId(), clan.getId(), action, String.valueOf(amount),
                String.valueOf(before), String.valueOf(after), "admin-gui"));
    }

    // ------------------------------------------------------------ Phase 5

    private void handleEvents(Player player, EventsGuiHolder holder, int slot, ClickType click) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == EventsGui.SLOT_BACK) {
            gui.myClan().open(player, clan);
            return;
        }
        if (slot == EventsGui.SLOT_CHALLENGE) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_EVENT_CREATE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "events.no-permission");
                return;
            }
            gui.challengeBrowse().open(player, clan);
            return;
        }
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) return;
        ClanEvent event = holder.getCurrentEntries().get(slot);
        if (event.getStatus() != EventStatus.PENDING_INVITE || !event.getClanB().equals(clan.getId())) return;

        player.closeInventory();
        if (click.isLeftClick()) {
            plugin.getEventService().handleAccept(player, event);
        } else if (click.isRightClick()) {
            plugin.getEventService().handleDeny(player, event);
        }
    }

    private void handleChallengeBrowse(Player player, ChallengeBrowseGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        if (slot == ChallengeBrowseGui.SLOT_BACK) {
            plugin.getGuiManager().events().open(player, clan);
            return;
        }
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) return;
        plugin.getGuiManager().challengeSetup().open(player, clan, holder.getCurrentEntries().get(slot));
    }

    private void handleChallengeSetup(Player player, ChallengeSetupGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        Optional<Clan> targetOpt = plugin.getClanService().getClan(holder.getTargetClanId());
        if (clanOpt.isEmpty() || targetOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();
        var config = plugin.getConfigManager().loadYaml("events.yml");

        switch (slot) {
            case ChallengeSetupGui.SLOT_MONEY -> {
                double[] presets = ChallengeSetupGui.MONEY_PRESETS;
                int idx = 0;
                for (int i = 0; i < presets.length; i++) if (presets[i] == holder.getMoneyWager()) idx = i;
                holder.setMoneyWager(presets[(idx + 1) % presets.length]);
            }
            case ChallengeSetupGui.SLOT_CP -> {
                long[] presets = ChallengeSetupGui.CP_PRESETS;
                int idx = 0;
                for (int i = 0; i < presets.length; i++) if (presets[i] == holder.getCpWager()) idx = i;
                holder.setCpWager(presets[(idx + 1) % presets.length]);
            }
            case ChallengeSetupGui.SLOT_LOCATION -> {
                LocationType[] all = LocationType.values();
                LocationType next = holder.getLocationType();
                for (int i = 0; i < all.length; i++) {
                    next = all[(next.ordinal() + 1) % all.length];
                    boolean usable = next != LocationType.ADMIN_ARENA || plugin.getArenaService().get().isFullyConfigured();
                    if (usable) break;
                }
                holder.setLocationType(next);
            }
            case ChallengeSetupGui.SLOT_DURATION -> {
                java.util.List<Integer> options = config.getIntegerList("round-duration-options-minutes");
                if (!options.isEmpty()) {
                    int idx = options.indexOf(holder.getRoundDurationMinutes());
                    holder.setRoundDurationMinutes(options.get((idx + 1) % options.size()));
                }
            }
            case ChallengeSetupGui.SLOT_ROUNDS -> {
                int max = Math.max(1, config.getInt("max-rounds", 15));
                holder.setTotalRounds(holder.getTotalRounds() >= max ? 1 : holder.getTotalRounds() + 1);
            }
            case ChallengeSetupGui.SLOT_BACK -> {
                gui.challengeBrowse().open(player, clan);
                return;
            }
            case ChallengeSetupGui.SLOT_CONFIRM -> {
                Clan target = targetOpt.get();
                player.closeInventory();
                plugin.getEventService().createChallenge(player, clan, target, holder.getLocationType(),
                        holder.getRoundDurationMinutes() * 60, holder.getTotalRounds(),
                        holder.getMoneyWager(), holder.getCpWager(),
                        fail -> {
                            String key = switch (fail) {
                                case SELF -> "alliance.self";
                                case ALREADY_IN_EVENT -> "events.already-in-event";
                                case ALREADY_PENDING -> "events.already-pending";
                                case NO_WAGER -> "events.no-wager";
                                case NO_PERMISSION -> "events.no-permission";
                                case NO_LOCATION -> "events.no-location";
                                case TARGET_DISABLED -> "prefs.blocked-challenges";
                            };
                            plugin.getMessageService().send(player, key);
                            plugin.playSound(player, "error");
                        },
                        () -> {
                            plugin.getMessageService().send(player, "events.challenge-sent",
                                    Map.of("clan", target.getName()));
                            plugin.playSound(player, "invite");
                            for (var m : plugin.getClanService().getMembers(target.getId())) {
                                Player online = plugin.getServer().getPlayer(m.getUuid());
                                boolean manager = target.getOwnerUuid().equals(m.getUuid())
                                        || plugin.getPermissionService().hasPermission(m.getUuid(), ClanPermission.CLAN_EVENT_ACCEPT);
                                if (online != null && manager && plugin.getPrefsService().get(m.getUuid()).challenges()) {
                                    plugin.getMessageService().send(online, "events.challenge-received",
                                            Map.of("clan", clan.getName()));
                                    plugin.playSound(online, "request");
                                }
                            }
                        });
                return;
            }
            default -> {
                return;
            }
        }
        gui.challengeSetup().reopen(player, holder);
    }

    // ------------------------------------------------------------ Phase 4

    private void handleAlliances(Player player, AlliancesGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == AlliancesGui.SLOT_BACK) {
            gui.myClan().open(player, clan);
            return;
        }
        if (slot == AlliancesGui.SLOT_PROPOSE) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_ALLIANCE_MANAGE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "alliance.no-permission");
                return;
            }
            gui.allianceBrowse().open(player, clan);
            return;
        }
        for (int i = 0; i < AlliancesGui.ALLIANCE_SLOTS.length; i++) {
            if (AlliancesGui.ALLIANCE_SLOTS[i] != slot) continue;
            if (holder.getCurrentEntries() == null || i >= holder.getCurrentEntries().size()) return;
            ClanAlliance alliance = holder.getCurrentEntries().get(i);
            plugin.getClanService().getClan(alliance.otherClan(clan.getId()))
                    .ifPresent(other -> gui.allianceInfo().open(player, clan, alliance, other));
            return;
        }
    }

    private void handleAllianceBrowse(Player player, AllianceBrowseGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        if (slot == AllianceBrowseGui.SLOT_BACK) {
            plugin.getGuiManager().alliances().open(player, clan);
            return;
        }
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) return;
        Clan target = holder.getCurrentEntries().get(slot);
        player.closeInventory();
        plugin.getAllianceService().sendInvite(player, clan, target, fail -> {
            String key = switch (fail) {
                case SELF -> "alliance.self";
                case ALREADY_ALLIED -> "alliance.already-allied";
                case ALREADY_PENDING -> "alliance.already-pending";
                case LIMIT_REACHED -> "alliance.limit-reached";
                case NO_PERMISSION -> "alliance.no-permission";
                case TARGET_DISABLED -> "prefs.blocked-alliance";
            };
            plugin.getMessageService().send(player, key);
        }, () -> {
            plugin.getMessageService().send(player, "alliance.invite-sent",
                    Map.of("clan", target.getName()));
            plugin.playSound(player, "invite");
            plugin.getClanService().getMembers(target.getId()).forEach(m -> {
                Player online = plugin.getServer().getPlayer(m.getUuid());
                boolean manager = target.getOwnerUuid().equals(m.getUuid())
                        || plugin.getPermissionService().hasPermission(m.getUuid(), ClanPermission.CLAN_ALLIANCE_MANAGE);
                if (online != null && manager && plugin.getPrefsService().get(m.getUuid()).alliance()) {
                    plugin.getMessageService().send(online, "alliance.invite-received",
                            Map.of("clan", clan.getName()));
                }
            });
        });
    }

    private void handleAllianceInfo(Player player, AllianceInfoGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        Optional<Clan> otherOpt = plugin.getClanService().getClan(holder.getOtherClanId());
        if (clanOpt.isEmpty() || otherOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        Clan other = otherOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == AllianceInfoGui.SLOT_BACK) {
            gui.alliances().open(player, clan);
        } else if (slot == AllianceInfoGui.SLOT_MANAGE) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_ALLIANCE_MANAGE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "alliance.no-permission");
                return;
            }
            gui.allianceSettings().open(player, clan, holder.getAllianceId(), holder.getOtherClanId());
        } else if (slot == AllianceInfoGui.SLOT_DISBAND) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_ALLIANCE_MANAGE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "alliance.no-permission");
                return;
            }
            gui.confirm().open(player, clan.getId(), ConfirmGuiHolder.Action.DISBAND_ALLIANCE, 0,
                    holder.getAllianceId(), "&cDisband alliance with " + other.getName() + "?");
        }
    }

    private void handleAllianceSettings(Player player, AllianceSettingsGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        Optional<Clan> otherOpt = plugin.getClanService().getClan(holder.getOtherClanId());
        if (clanOpt.isEmpty() || otherOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();

        if (slot == AllianceSettingsGui.SLOT_BACK) {
            plugin.getClanService().getClan(holder.getOtherClanId()).ifPresent(other -> {
                var alliance = plugin.getAllianceService().getAlliance(clan.getId(), other.getId());
                alliance.ifPresent(a -> plugin.getGuiManager().allianceInfo().open(player, clan, a, other));
            });
            return;
        }

        AllianceSettings settings = plugin.getAllianceService().getSettings(holder.getAllianceId(), clan.getId());
        boolean changed = true;
        switch (slot) {
            case AllianceSettingsGui.SLOT_SPAWN -> settings.setAllowSpawn(!settings.isAllowSpawn());
            case AllianceSettingsGui.SLOT_WARP -> settings.setAllowWarp(!settings.isAllowWarp());
            case AllianceSettingsGui.SLOT_STORAGE -> settings.setAllowStorage(!settings.isAllowStorage());
            case AllianceSettingsGui.SLOT_BANK -> settings.setAllowBank(!settings.isAllowBank());
            case AllianceSettingsGui.SLOT_MEMBER_LIST -> settings.setAllowMemberList(!settings.isAllowMemberList());
            case AllianceSettingsGui.SLOT_CLAN_INFO -> settings.setAllowClanInfo(!settings.isAllowClanInfo());
            case AllianceSettingsGui.SLOT_PREVENT_PVP -> settings.setPreventPvp(!settings.isPreventPvp());
            default -> changed = false;
        }
        if (changed) {
            plugin.getAllianceService().updateSettings(holder.getAllianceId(), clan.getId(), settings);
            plugin.getMessageService().send(player, "alliance.settings-updated",
                    Map.of("clan", otherOpt.get().getName()));
            plugin.playSound(player, "click");
            plugin.getGuiManager().allianceSettings().open(player, clan, holder.getAllianceId(), holder.getOtherClanId());
        }
    }

    private void handleTasks(Player player, TasksGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) { player.closeInventory(); return; }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == TasksGui.SLOT_BACK) {
            gui.myClan().open(player, clan);
            return;
        }
        Period period = switch (slot) {
            case TasksGui.SLOT_DAILY -> Period.DAILY;
            case TasksGui.SLOT_WEEKLY -> Period.WEEKLY;
            case TasksGui.SLOT_MONTHLY -> Period.MONTHLY;
            default -> null;
        };
        if (period == null) return;

        plugin.getTaskService().claim(player, clan, period, fail -> {
            String key = fail == me.uc.hussein.ultrasclans.task.TaskService.ClaimResult.ALREADY_CLAIMED
                    ? "tasks.already-claimed" : "tasks.not-complete";
            plugin.getMessageService().send(player, key);
        }, (template, reward) -> {
            plugin.getMessageService().send(player, "tasks.claimed",
                    Map.of("cp", String.valueOf(reward), "task", template.name()));
            plugin.playSound(player, "success");
            gui.tasks().open(player, clan);
        });
    }

    // ------------------------------------------------------------ Phase 3

    private void handleWarps(Player player, WarpsGuiHolder holder, int slot, ClickType click) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == WarpsGui.SLOT_BACK) {
            gui.myClan().open(player, clan);
            return;
        }
        if (slot == WarpsGui.SLOT_CREATE) {
            plugin.getWarpService().create(player, clan,
                    fail -> {
                        String key = switch (fail) {
                            case NO_PERMISSION -> "general.no-permission";
                            case LIMIT_REACHED -> "warps.limit";
                            case MAX_ABSOLUTE_REACHED -> "warps.limit";
                        };
                        plugin.getMessageService().send(player, key);
                        plugin.playSound(player, "error");
                    },
                    warp -> {
                        plugin.getMessageService().send(player, "warps.created",
                                Map.of("id", String.valueOf(warp.getWarpId())));
                        plugin.playSound(player, "success");
                        gui.warps().open(player, clan);
                    });
            return;
        }
        for (int i = 0; i < WarpsGui.WARP_SLOTS.length; i++) {
            if (WarpsGui.WARP_SLOTS[i] != slot) continue;
            if (holder.getCurrentEntries() == null || i >= holder.getCurrentEntries().size()) return;
            var warp = holder.getCurrentEntries().get(i);
            if (click.isShiftClick()) {
                gui.confirm().open(player, clan.getId(), ConfirmGuiHolder.Action.DELETE_WARP, warp.getWarpId(),
                        plugin.getMessageService().text(player, "warps.delete-confirm-title",
                                Map.of("id", String.valueOf(warp.getWarpId()))));
            } else {
                player.closeInventory();
                plugin.getWarpService().teleport(player, clan, warp.getWarpId(), fail -> {
                    String key = switch (fail) {
                        case NO_PERMISSION -> "general.no-permission";
                        case NOT_FOUND -> "warps.not-found";
                        case WORLD_UNLOADED -> "warps.world-missing";
                        case UNSAFE -> "warps.unsafe";
                    };
                    plugin.getMessageService().send(player, key,
                            Map.of("id", String.valueOf(warp.getWarpId())));
                });
            }
            return;
        }
    }

    private void handleConfirm(Player player, ConfirmGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        if (holder.getAction() == ConfirmGuiHolder.Action.ADMIN_RESET_CLAN
                || holder.getAction() == ConfirmGuiHolder.Action.ADMIN_DELETE_CLAN) {
            if (denyIfNotAdmin(player)) return;
            if (slot == ConfirmGui.SLOT_YES) {
                if (holder.getAction() == ConfirmGuiHolder.Action.ADMIN_RESET_CLAN) {
                    plugin.getClanService().resetClan(clan, player.getUniqueId());
                    plugin.getAdminGui().openClan(player, clan);
                } else {
                    plugin.getClanService().deleteClan(clan.getId(), "ADMIN_DELETE", player.getUniqueId())
                            .whenComplete((v, e) -> plugin.getScheduler().runSync(() -> plugin.getAdminGui().open(player, 0)));
                }
            } else if (slot == ConfirmGui.SLOT_NO) {
                plugin.getAdminGui().openClan(player, clan);
            }
            return;
        }
        if (slot == ConfirmGui.SLOT_YES && holder.getAction() == ConfirmGuiHolder.Action.DISBAND_ALLIANCE) {
            var otherOpt = plugin.getAllianceService().getAlliances(clan.getId()).stream()
                    .filter(a -> a.getId().equals(holder.getContextUuid())).findFirst();
            if (otherOpt.isEmpty()) {
                plugin.getGuiManager().alliances().open(player, clan);
                return;
            }
            ClanAlliance alliance = otherOpt.get();
            UUID otherClanId = alliance.otherClan(clan.getId());
            plugin.getAllianceService().disband(alliance.getId(), alliance.getClanA(), alliance.getClanB())
                    .whenComplete((v, err) -> plugin.getScheduler().runSync(() -> {
                        plugin.getClanService().getClan(otherClanId).ifPresent(other -> {
                            plugin.getMessageService().send(player, "alliance.disbanded",
                                    Map.of("clan", other.getName()));
                            plugin.getClanService().getMembers(other.getId()).forEach(m -> {
                                Player online = plugin.getServer().getPlayer(m.getUuid());
                                if (online != null) {
                                    plugin.getMessageService().send(online, "alliance.disbanded-other",
                                            Map.of("clan", clan.getName()));
                                }
                            });
                        });
                        plugin.getGuiManager().alliances().open(player, clan);
                    }));
            return;
        }
        if (slot == ConfirmGui.SLOT_YES && holder.getAction() == ConfirmGuiHolder.Action.DELETE_WARP) {
            plugin.getWarpService().delete(player, clan, holder.getContextId(),
                    fail -> {
                        plugin.getMessageService().send(player, fail == me.uc.hussein.ultrasclans.warp.WarpService.DeleteFail.NO_PERMISSION
                                        ? "general.no-permission" : "warps.not-found");
                        plugin.getGuiManager().warps().open(player, clan);
                    },
                    () -> {
                        plugin.getMessageService().send(player, "warps.deleted",
                                Map.of("id", String.valueOf(holder.getContextId())));
                        plugin.getGuiManager().warps().open(player, clan);
                    });
        } else if (slot == ConfirmGui.SLOT_NO) {
            if (holder.getAction() == ConfirmGuiHolder.Action.DISBAND_ALLIANCE) {
                plugin.getGuiManager().alliances().open(player, clan);
            } else {
                plugin.getGuiManager().warps().open(player, clan);
            }
        }
    }

    private void handleTpaList(Player player, TpaGuiHolder holder, int slot) {
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) {
            return;
        }
        Player target = holder.getCurrentEntries().get(slot);
        if (!target.isOnline()) {
            plugin.getMessageService().send(player, "tpa.offline");
            return;
        }
        plugin.getGuiManager().tpaPlayerInfo().open(player, target);
    }

    private void handleTpaPlayerInfo(Player player, TpaPlayerInfoGuiHolder holder, int slot) {
        Player target = plugin.getServer().getPlayer(holder.getTargetUuid());
        if (target == null) {
            player.closeInventory();
            plugin.getMessageService().send(player, "tpa.offline");
            return;
        }
        if (slot == TpaPlayerInfoGui.SLOT_BACK) {
            plugin.getClanService().getPlayerClan(player.getUniqueId())
                    .ifPresent(c -> plugin.getGuiManager().tpa().open(player, c));
            return;
        }
        if (slot == TpaPlayerInfoGui.SLOT_SEND_TPA || slot == TpaPlayerInfoGui.SLOT_REQUEST_HERE) {
            boolean here = slot == TpaPlayerInfoGui.SLOT_REQUEST_HERE;
            player.closeInventory();
            plugin.getTpaService().sendAndNotify(player, target, here ? TpaRequest.Mode.HERE : TpaRequest.Mode.STANDARD);
        }
    }

    // ------------------------------------------------------------ Phase 2

    private void handleBank(Player player, BankGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty() || !plugin.getClanService().getPlayerClan(player.getUniqueId())
                .map(c -> c.getId().equals(holder.getClanId())).orElse(false)) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == BankGui.SLOT_MODE_DEPOSIT) {
            gui.bank().open(player, clan, BankGuiHolder.Mode.DEPOSIT);
            return;
        }
        if (slot == BankGui.SLOT_MODE_WITHDRAW) {
            gui.bank().open(player, clan, BankGuiHolder.Mode.WITHDRAW);
            return;
        }
        if (slot == BankGui.SLOT_BACK) {
            gui.myClan().open(player, clan);
            return;
        }

        for (int i = 0; i < BankGui.AMOUNT_SLOTS.length; i++) {
            if (BankGui.AMOUNT_SLOTS[i] != slot) continue;
            double amount = BankGui.PRESET_AMOUNTS[i];
            if (amount < 0) {
                player.closeInventory();
                plugin.getMessageService().sendLiteral(player, "&7Type the amount in chat, or &fcancel&7 to abort.");
                gui.pendingInput().request(player.getUniqueId(),
                        holder.getMode() == BankGuiHolder.Mode.DEPOSIT
                                ? PendingInputManager.InputType.BANK_DEPOSIT_CUSTOM
                                : PendingInputManager.InputType.BANK_WITHDRAW_CUSTOM, 30);
                return;
            }
            plugin.getGuiManager().bankInteraction().execute(player, clan, holder.getMode(), amount);
            return;
        }
    }

    private void handleStorage(Player player, StorageGuiHolder holder, int slot) {
        // نصل هنا فقط للخانات المقفلة أو صف التحكم (الخانات المفتوحة عولجت مسبقًا)
        if (slot == StorageGui.SLOT_BACK) {
            plugin.getClanService().getClan(holder.getClanId())
                    .ifPresent(c -> plugin.getGuiManager().myClan().open(player, c));
            return;
        }
        if (slot < StorageGui.MAX_USABLE_SLOTS && !holder.isUnlocked(slot)) {
            plugin.getMessageService().send(player, "storage.locked-slot");
            plugin.playSound(player, "error");
        }
    }

    private void handleUpgrades(Player player, UpgradesGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        if (slot == UpgradesGui.SLOT_BACK) {
            plugin.getGuiManager().myClan().open(player, clan);
            return;
        }
        UpgradeType type = UpgradesGui.SLOT_TYPES.get(slot);
        if (type == null) {
            return;
        }
        var messages = plugin.getMessageManager();
        plugin.getUpgradeService().attemptUpgrade(player, clan, type, result -> {
            switch (result.getStatus()) {
                case SUCCESS -> {
                    plugin.getMessageService().send(player, "upgrades.success", Map.of(
                            "upgrade", type.name().toLowerCase().replace('_', ' '),
                            "value", result.getNewValueDisplay()));
                    plugin.playSound(player, "upgrade");
                }
                case NOT_ENOUGH_CP -> {
                    plugin.getMessageService().send(player, "upgrades.not-enough-cp", Map.of(
                            "cost", String.valueOf(result.getCost()),
                            "current", String.valueOf(clan.getCp())));
                    plugin.playSound(player, "error");
                }
                case MAX_REACHED -> plugin.getMessageService().send(player, "upgrades.max-reached");
                case NO_PERMISSION -> plugin.getMessageService().send(player, "upgrades.no-permission");
            }
            plugin.getGuiManager().upgrades().open(player, clan);
        });
    }

    // ------------------------------------------------------------

    private void handleClanMain(Player player, int slot) {
        var gui = plugin.getGuiManager();
        switch (slot) {
            case me.uc.hussein.ultrasclans.gui.ClanMainGui.SLOT_CREATE -> {
                player.closeInventory();
                plugin.getMessageService().send(player, "clan.create-prompt");
                gui.pendingInput().request(player.getUniqueId(), PendingInputManager.InputType.CREATE_CLAN_NAME, 60);
            }
            case me.uc.hussein.ultrasclans.gui.ClanMainGui.SLOT_JOIN -> gui.clansList().open(player, 0, null);
            case me.uc.hussein.ultrasclans.gui.ClanMainGui.SLOT_INVITES -> gui.invites().open(player);
            case me.uc.hussein.ultrasclans.gui.ClanMainGui.SLOT_HELP -> plugin.getClanCommand().sendHelp(player);
            case 22 -> plugin.getSettingsGui().open(player);
            case me.uc.hussein.ultrasclans.gui.ClanMainGui.SLOT_TOP ->
                    gui.top().open(player, me.uc.hussein.ultrasclans.gui.TopGui.Category.CP);
            default -> {
            }
        }
    }

    private void handleMyClan(Player player, MyClanGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == 10) {
            gui.clanInfo().open(player, clan, false);
        } else if (slot == 12) {
            gui.members().open(player, clan, 0);
        } else if (slot == 14) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_ACCEPT_REQUEST)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "general.no-permission");
                return;
            }
            gui.requests().open(player, clan);
        } else if (slot == 19) {
            gui.warps().open(player, clan);
        } else if (slot == 21) {
            gui.tpa().open(player, clan);
        } else if (slot == 23) {
            boolean allowedChat = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_CHAT)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowedChat) {
                plugin.getMessageService().send(player, "chat.no-permission-chat");
                return;
            }
            boolean nowOn = plugin.getChatService().toggleClanChat(player.getUniqueId());
            plugin.getMessageService().send(player, nowOn ? "chat.chat-on" : "chat.chat-off");
            plugin.playSound(player, "click");
            gui.myClan().open(player, clan);
        } else if (slot == 25) {
            boolean nowOn = plugin.getChatService().toggleTag(player.getUniqueId());
            plugin.getMessageService().send(player, nowOn ? "chat.tag-on" : "chat.tag-off");
            plugin.playSound(player, "click");
            gui.myClan().open(player, clan);
        } else if (slot == 28) {
            gui.bank().open(player, clan, me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder.Mode.DEPOSIT);
        } else if (slot == 30) {
            gui.storage().open(player, clan);
        } else if (slot == 32) {
            gui.upgrades().open(player, clan);
        } else if (slot == 16) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_INVITE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "general.no-permission");
                return;
            }
            player.closeInventory();
            plugin.getMessageService().sendLiteral(player, "&7Type &f/clan invite <player> &7to send an invite.");
        } else if (slot == 49) {
            player.closeInventory();
            plugin.getMessageService().sendLiteral(player, "&7Type &f/clan leave confirm &7to confirm leaving your clan.");
        }
    }

    private void handleClansList(Player player, ClansListGuiHolder holder, int slot) {
        var gui = plugin.getGuiManager();
        if (slot == me.uc.hussein.ultrasclans.gui.ClansListGui.SLOT_PREV) {
            gui.clansList().open(player, holder.getPage() - 1, holder.getSearchQuery());
        } else if (slot == me.uc.hussein.ultrasclans.gui.ClansListGui.SLOT_NEXT) {
            gui.clansList().open(player, holder.getPage() + 1, holder.getSearchQuery());
        } else if (slot == me.uc.hussein.ultrasclans.gui.ClansListGui.SLOT_SEARCH) {
            player.closeInventory();
            plugin.getMessageService().sendLiteral(player, "&7Type a clan name to search, or &fcancel&7 to clear the search.");
            gui.pendingInput().request(player.getUniqueId(), PendingInputManager.InputType.SEARCH_CLANS, 30);
        } else if (slot == me.uc.hussein.ultrasclans.gui.ClansListGui.SLOT_BACK) {
            gui.openClanRoot(player);
        } else if (holder.getCurrentPageEntries() != null && slot < holder.getCurrentPageEntries().size()) {
            Clan clan = holder.getCurrentPageEntries().get(slot);
            gui.clanInfo().open(player, clan, true);
        }
    }

    private void handleClanInfo(Player player, ClanInfoGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == me.uc.hussein.ultrasclans.gui.ClanInfoGui.SLOT_JOIN && holder.isFromBrowser()) {
            player.closeInventory();
            if (plugin.getClanService().isInClan(player.getUniqueId())) {
                plugin.getMessageService().send(player, "clan.already-in-clan");
                return;
            }
            if (clan.isPublic()) {
                plugin.getMemberService().join(player.getUniqueId(), clan.getId(),
                        fail -> plugin.getMessageService().send(player, "clan.join-clan-full"),
                        restored -> {
                            plugin.getMessageService().send(player, "clan.join-success",
                                    Map.of("clan", clan.getName()));
                            plugin.playSound(player, "success");
                        });
            } else {
                if (!plugin.getPrefsService().anyManagerAccepts(clan, ClanPermission.CLAN_ACCEPT_REQUEST,
                        me.uc.hussein.ultrasclans.model.PlayerPrefs.Setting.JOIN_REQUESTS)) {
                    plugin.getMessageService().send(player, "prefs.blocked-join-requests", Map.of("clan", clan.getName()));
                    plugin.playSound(player, "error");
                    return;
                }
                plugin.getRequestService().send(player.getUniqueId(), clan.getId(),
                        fail -> plugin.getMessageService().send(player, "requests.already-pending"),
                        () -> {
                            plugin.getMessageService().send(player, "requests.sent",
                                    Map.of("clan", clan.getName()));
                            plugin.notifyClanManagers(clan, "requests.received", Map.of("player", player.getName()));
                            plugin.playSound(player, "request");
                        });
            }
        } else if (slot == me.uc.hussein.ultrasclans.gui.ClanInfoGui.SLOT_MEMBERS) {
            gui.members().open(player, clan, 0);
        } else if (slot == me.uc.hussein.ultrasclans.gui.ClanInfoGui.SLOT_BACK) {
            if (holder.isFromBrowser()) {
                gui.clansList().open(player, 0, null);
            } else {
                gui.openClanRoot(player);
            }
        }
    }

    private void handleMembers(Player player, MembersGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        if (clanOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        var gui = plugin.getGuiManager();

        if (slot == me.uc.hussein.ultrasclans.gui.MembersGui.SLOT_PREV) {
            gui.members().open(player, clan, holder.getPage() - 1);
        } else if (slot == me.uc.hussein.ultrasclans.gui.MembersGui.SLOT_NEXT) {
            gui.members().open(player, clan, holder.getPage() + 1);
        } else if (slot == me.uc.hussein.ultrasclans.gui.MembersGui.SLOT_BACK) {
            boolean isOwnClan = plugin.getClanService().getPlayerClan(player.getUniqueId())
                    .map(Clan::getId).map(id -> id.equals(clan.getId())).orElse(false);
            if (isOwnClan) {
                gui.myClan().open(player, clan);
            } else {
                gui.clanInfo().open(player, clan, true);
            }
        } else if (holder.getCurrentPageEntries() != null && slot < holder.getCurrentPageEntries().size()) {
            ClanMember target = holder.getCurrentPageEntries().get(slot);
            gui.memberInfo().open(player, clan, target);
        }
    }

    private void handleMemberInfo(Player player, MemberInfoGuiHolder holder, int slot) {
        Optional<Clan> clanOpt = plugin.getClanService().getClan(holder.getClanId());
        Optional<ClanMember> targetOpt = plugin.getClanService().getMember(holder.getTargetUuid());
        if (clanOpt.isEmpty() || targetOpt.isEmpty()) {
            player.closeInventory();
            return;
        }
        Clan clan = clanOpt.get();
        ClanMember target = targetOpt.get();
        var gui = plugin.getGuiManager();

        boolean isTargetOwner = clan.getOwnerUuid().equals(target.getUuid());

        if (slot == me.uc.hussein.ultrasclans.gui.MemberInfoGui.SLOT_KICK && !isTargetOwner) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_KICK)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed || !plugin.getPermissionService().canManage(player.getUniqueId(), target.getUuid())) {
                plugin.getMessageService().send(player, "general.no-permission");
                return;
            }
            String targetName = plugin.getServer().getOfflinePlayer(target.getUuid()).getName();
            plugin.getMemberService().kick(target.getUuid()).whenComplete((v, err) ->
                    plugin.getScheduler().runSync(() -> {
                        plugin.getMessageService().send(player, "clan.kick-success",
                                Map.of("player", targetName != null ? targetName : "Unknown"));
                        var onlineTarget = plugin.getServer().getPlayer(target.getUuid());
                        if (onlineTarget != null) {
                            plugin.getMessageService().send(onlineTarget, "clan.kick-target",
                                    Map.of("clan", clan.getName()));
                        }
                        gui.members().open(player, clan, 0);
                    }));
        } else if ((slot == me.uc.hussein.ultrasclans.gui.MemberInfoGui.SLOT_PROMOTE
                || slot == me.uc.hussein.ultrasclans.gui.MemberInfoGui.SLOT_DEMOTE) && !isTargetOwner) {
            boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), ClanPermission.CLAN_PROMOTE)
                    || plugin.getPermissionService().isOwner(player.getUniqueId());
            if (!allowed) {
                plugin.getMessageService().send(player, "general.no-permission");
                return;
            }
            boolean up = slot == me.uc.hussein.ultrasclans.gui.MemberInfoGui.SLOT_PROMOTE;
            List<ClanRankDefinition> ranks = plugin.getClanService().getRanks(clan.getId());
            Optional<ClanRankDefinition> newRank = findAdjacentRank(ranks, target.getRankKey(), up);
            if (newRank.isEmpty()) {
                return;
            }
            plugin.getMemberService().setRank(target.getUuid(), newRank.get().getRankKey())
                    .whenComplete((v, err) -> plugin.getScheduler().runSync(() -> gui.memberInfo().open(player, clan, target)));
        } else if (slot == me.uc.hussein.ultrasclans.gui.MemberInfoGui.SLOT_BACK) {
            gui.members().open(player, clan, 0);
        }
    }

    private Optional<ClanRankDefinition> findAdjacentRank(List<ClanRankDefinition> ranks, String currentKey, boolean up) {
        ranks = new java.util.ArrayList<>(ranks);
        ranks.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        int index = -1;
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getRankKey().equalsIgnoreCase(currentKey)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return Optional.empty();
        }
        int target = up ? index + 1 : index - 1;
        if (target < 0 || target >= ranks.size()) {
            return Optional.empty();
        }
        if (ranks.get(target).isProtectedRank() && ranks.get(target).getPriority() >= 100) {
            return Optional.empty(); // لا يمكن الترقية إلى رتبة Owner
        }
        return Optional.of(ranks.get(target));
    }

    private void handleRequests(Player player, RequestsGuiHolder holder, int slot, ClickType click) {
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) {
            if (slot == me.uc.hussein.ultrasclans.gui.RequestsGui.SLOT_BACK) {
                plugin.getGuiManager().openClanRoot(player);
            }
            return;
        }
        JoinRequest request = holder.getCurrentEntries().get(slot);
        String requesterName = plugin.getServer().getOfflinePlayer(request.getRequesterUuid()).getName();

        if (click.isLeftClick()) {
            plugin.getRequestService().accept(request.getId(),
                    fail -> plugin.getMessageService().send(player, "errors.generic"),
                    clanId -> {
                        plugin.getMessageService().send(player, "requests.accepted",
                                Map.of("player", requesterName != null ? requesterName : "Unknown"));
                        var target = plugin.getServer().getPlayer(request.getRequesterUuid());
                        if (target != null) {
                            plugin.getClanService().getClan(clanId).ifPresent(c ->
                                    plugin.getMessageService().send(target, "requests.accepted-target",
                                            Map.of("clan", c.getName())));
                        }
                        plugin.getClanService().getClan(holder.getClanId()).ifPresent(c ->
                                plugin.getGuiManager().requests().open(player, c));
                    });
        } else if (click.isRightClick()) {
            plugin.getRequestService().deny(request.getId()).whenComplete((v, err) ->
                    plugin.getScheduler().runSync(() -> {
                        plugin.getMessageService().send(player, "requests.denied",
                                Map.of("player", requesterName != null ? requesterName : "Unknown"));
                        var target = plugin.getServer().getPlayer(request.getRequesterUuid());
                        plugin.getClanService().getClan(holder.getClanId()).ifPresent(c -> {
                            if (target != null) {
                                plugin.getMessageService().send(target, "requests.denied-target",
                                        Map.of("clan", c.getName()));
                            }
                            plugin.getGuiManager().requests().open(player, c);
                        });
                    }));
        }
    }

    private void handleInvites(Player player, InvitesGuiHolder holder, int slot, ClickType click) {
        if (holder.getCurrentEntries() == null || slot >= holder.getCurrentEntries().size()) {
            if (slot == me.uc.hussein.ultrasclans.gui.InvitesGui.SLOT_BACK) {
                plugin.getGuiManager().openClanRoot(player);
            }
            return;
        }
        ClanInvite invite = holder.getCurrentEntries().get(slot);

        if (click.isLeftClick()) {
            plugin.getInviteService().accept(invite.getId(), player.getUniqueId(),
                    fail -> plugin.getMessageService().send(player, "errors.generic"),
                    clanId -> plugin.getClanService().getClan(clanId).ifPresent(c -> {
                        plugin.getMessageService().send(player, "invites.accepted",
                                Map.of("clan", c.getName()));
                        plugin.playSound(player, "success");
                        player.closeInventory();
                    }));
        } else if (click.isRightClick()) {
            plugin.getInviteService().deny(invite.getId()).whenComplete((v, err) ->
                    plugin.getScheduler().runSync(() -> plugin.getClanService().getClan(invite.getClanId()).ifPresentOrElse(
                            c -> {
                                plugin.getMessageService().send(player, "invites.denied",
                                        Map.of("clan", c.getName()));
                                plugin.getGuiManager().invites().open(player);
                            },
                            () -> plugin.getGuiManager().invites().open(player)
                    )));
        }
    }
}
