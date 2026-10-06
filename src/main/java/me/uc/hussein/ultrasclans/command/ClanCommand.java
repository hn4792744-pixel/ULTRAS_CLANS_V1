package me.uc.hussein.ultrasclans.command;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanInvite;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ينفّذ /clan وكل الأوامر الفرعية الخاصة باللاعب (قسم 3). الواجهات
 * (GUI) تبقى الطريقة الأساسية للتفاعل، لكن كل الأوامر النصية هنا
 * تنفّذ المنطق نفسه مباشرة عبر الخدمات - وليست واجهات وهمية.
 */
public final class ClanCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "create", "invite", "accept", "deny", "leave", "kick", "promote", "demote",
            "info", "members", "requests", "invites", "bank", "storage", "upgrades",
            "warp", "warps", "setspawn", "spawn", "tpa", "tpa_accept", "chat", "tag",
            "alliance", "alliances", "tasks", "quests", "event", "events", "challenge", "transfer", "settings", "prefs", "top", "stats", "help"
    );

    private final UltrasClansPlugin plugin;

    public ClanCommand(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessageService().sendLiteral(sender, plugin.getMessageManager().get("general.player-only"));
            return true;
        }

        if (args.length == 0) {
            plugin.getGuiManager().openClanRoot(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create" -> handleCreate(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "leave" -> handleLeave(player, args);
            case "kick" -> handleKick(player, args);
            case "promote" -> handlePromote(player, args);
            case "demote" -> handleDemote(player, args);
            case "info" -> handleInfo(player, args);
            case "members" -> handleMembers(player);
            case "requests" -> handleRequests(player);
            case "invites" -> handleInvites(player);
            case "bank" -> handleBank(player);
            case "storage" -> handleStorage(player);
            case "upgrades" -> handleUpgrades(player);
            case "warp" -> handleWarp(player, args);
            case "warps" -> handleWarps(player);
            case "setspawn" -> handleSetSpawn(player);
            case "spawn" -> handleSpawnUse(player);
            case "tpa" -> handleTpa(player, args);
            case "tpa_accept" -> handleTpaAccept(player, args);
            case "chat" -> handleChatToggle(player, args);
            case "tag" -> handleTagToggle(player, args);
            case "alliance" -> handleAlliance(player, args);
            case "alliances" -> handleAlliances(player);
            case "tasks", "quests" -> handleTasks(player);
            case "event" -> handleEvent(player, args);
            case "events" -> handleEvents(player);
            case "challenge" -> handleChallenge(player, args);
            case "prefs" -> plugin.getSettingsGui().open(player);
            case "top" -> plugin.getGuiManager().top().open(player, me.uc.hussein.ultrasclans.gui.TopGui.Category.CP);
            case "stats" -> requireOwnClan(player).ifPresent(c -> plugin.getGuiManager().stats().open(player, c));
            case "transfer" -> handleTransfer(player, args);
            case "settings" -> handleSettings(player, args);
            case "help" -> sendHelp(player);
            default -> plugin.getMessageService().send(player, "general.unknown-command");
        }
        return true;
    }

    // ------------------------------------------------------------

    private void handleCreate(Player player) {
        if (plugin.getClanService().isInClan(player.getUniqueId())) {
            plugin.getMessageService().send(player, "clan.already-in-clan");
            return;
        }
        plugin.getMessageService().send(player, "clan.create-prompt");
        plugin.getGuiManager().pendingInput().request(player.getUniqueId(),
                me.uc.hussein.ultrasclans.gui.PendingInputManager.InputType.CREATE_CLAN_NAME, 60);
    }

    private void handleInvite(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan invite <player>");
            return;
        }
        if (!requirePermission(player, ClanPermission.CLAN_INVITE)) return;

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getUniqueId() == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            plugin.getMessageService().send(player, "errors.invalid-player");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            plugin.getMessageService().send(player, "errors.invalid-player");
            return;
        }

        Clan clan = clanOpt.get();
        if (!plugin.getPrefsService().get(target.getUniqueId()).invites()) {
            plugin.getMessageService().send(player, "prefs.blocked-invites",
                    Map.of("player", target.getName() != null ? target.getName() : args[1]));
            plugin.playSound(player, "error");
            return;
        }
        plugin.getInviteService().send(player.getUniqueId(), clan.getId(), target.getUniqueId(),
                fail -> {
                    switch (fail) {
                        case RECEIVER_IN_CLAN -> plugin.getMessageService().send(player, "clan.target-already-in-clan",
                                Map.of("player", target.getName() != null ? target.getName() : args[1]));
                        case ALREADY_PENDING -> plugin.getMessageService().send(player, "invites.already-pending");
                        case LIMIT_REACHED -> plugin.getMessageService().send(player, "invites.limit-reached");
                        default -> plugin.getMessageService().send(player, "errors.generic");
                    }
                },
                () -> {
                    plugin.getMessageService().send(player, "invites.sent",
                            Map.of("player", target.getName() != null ? target.getName() : args[1]));
                    plugin.playSound(player, "invite");
                    Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null) {
                        plugin.getMessageService().send(onlineTarget, "invites.received",
                                Map.of("clan", clan.getName()));
                        plugin.playSound(onlineTarget, "invite");
                    }
                });
    }

    private void handleAccept(Player player, String[] args) {
        if (plugin.getClanService().isInClan(player.getUniqueId())) {
            plugin.getMessageService().send(player, "clan.already-in-clan");
            return;
        }
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan accept <clan>");
            return;
        }
        Optional<Clan> clanOpt = plugin.getClanService().getClanByName(args[1]);
        if (clanOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.invalid-clan");
            return;
        }
        Clan clan = clanOpt.get();
        plugin.getInviteService().findPendingForClanAndReceiver(clan.getId(), player.getUniqueId())
                .whenComplete((opt, error) -> plugin.getScheduler().runSync(() -> {
                    if (error != null || opt.isEmpty()) {
                        plugin.getMessageService().send(player, "invites.none-pending");
                        return;
                    }
                    ClanInvite invite = opt.get();
                    plugin.getInviteService().accept(invite.getId(), player.getUniqueId(),
                            fail -> plugin.getMessageService().send(player, "errors.generic"),
                            clanId -> {
                                plugin.getMessageService().send(player, "invites.accepted",
                                        Map.of("clan", clan.getName()));
                                plugin.playSound(player, "success");
                            });
                }));
    }

    private void handleDeny(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan deny <clan>");
            return;
        }
        Optional<Clan> clanOpt = plugin.getClanService().getClanByName(args[1]);
        if (clanOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.invalid-clan");
            return;
        }
        Clan clan = clanOpt.get();
        plugin.getInviteService().findPendingForClanAndReceiver(clan.getId(), player.getUniqueId())
                .whenComplete((opt, error) -> plugin.getScheduler().runSync(() -> {
                    if (error != null || opt.isEmpty()) {
                        plugin.getMessageService().send(player, "invites.none-pending");
                        return;
                    }
                    plugin.getInviteService().deny(opt.get().getId()).whenComplete((v, err) ->
                            plugin.getScheduler().runSync(() ->
                                    plugin.getMessageService().send(player, "invites.denied",
                                            Map.of("clan", clan.getName()))));
                }));
    }

    private void handleLeave(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        Clan clan = clanOpt.get();

        boolean isOwner = clan.getOwnerUuid().equals(player.getUniqueId());
        if (isOwner) {
            plugin.getMessageService().send(player, "clan.leave-owner-must-transfer");
            return;
        }

        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            plugin.getMessageService().sendLiteral(player, "&eType &f/clan leave confirm &eto confirm.");
            return;
        }

        plugin.getMemberService().leave(player.getUniqueId()).whenComplete((outcome, error) ->
                plugin.getScheduler().runSync(() -> {
                    if (error != null) {
                        plugin.getMessageService().send(player, "errors.generic");
                        return;
                    }
                    plugin.getMessageService().send(player, "clan.leave-success",
                            Map.of("clan", clan.getName()));
                    plugin.playSound(player, "click");
                }));
    }

    private void handleKick(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan kick <player>");
            return;
        }
        if (!requirePermission(player, ClanPermission.CLAN_KICK)) return;

        Clan clan = clanOpt.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Optional<ClanMember> targetMember = plugin.getClanService().getMember(target.getUniqueId());
        if (targetMember.isEmpty() || !targetMember.get().getClanId().equals(clan.getId())) {
            plugin.getMessageService().send(player, "clan.target-not-in-clan",
                    Map.of("player", args[1]));
            return;
        }
        if (clan.getOwnerUuid().equals(target.getUniqueId())) {
            plugin.getMessageService().send(player, "general.no-permission");
            return;
        }
        if (!plugin.getPermissionService().canManage(player.getUniqueId(), target.getUniqueId())) {
            plugin.getMessageService().send(player, "general.no-permission");
            return;
        }

        plugin.getMemberService().kick(target.getUniqueId()).whenComplete((v, error) ->
                plugin.getScheduler().runSync(() -> {
                    plugin.getMessageService().send(player, "clan.kick-success",
                            Map.of("player", target.getName() != null ? target.getName() : args[1]));
                    Player onlineTarget = target.getPlayer();
                    if (onlineTarget != null) {
                        plugin.getMessageService().send(onlineTarget, "clan.kick-target",
                                Map.of("clan", clan.getName()));
                    }
                }));
    }

    private void handlePromote(Player player, String[] args) {
        changeRank(player, args, true);
    }

    private void handleDemote(Player player, String[] args) {
        changeRank(player, args, false);
    }

    private void changeRank(Player player, String[] args, boolean up) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan " + (up ? "promote" : "demote") + " <player>");
            return;
        }
        if (!requirePermission(player, ClanPermission.CLAN_PROMOTE)) return;

        Clan clan = clanOpt.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        Optional<ClanMember> targetMember = plugin.getClanService().getMember(target.getUniqueId());
        if (targetMember.isEmpty() || !targetMember.get().getClanId().equals(clan.getId())) {
            plugin.getMessageService().send(player, "clan.target-not-in-clan", Map.of("player", args[1]));
            return;
        }
        if (clan.getOwnerUuid().equals(target.getUniqueId())) {
            plugin.getMessageService().send(player, "general.no-permission");
            return;
        }

        var ranks = new ArrayList<>(plugin.getClanService().getRanks(clan.getId()));
        ranks.sort((a, b) -> Integer.compare(a.getPriority(), b.getPriority()));
        int idx = -1;
        for (int i = 0; i < ranks.size(); i++) {
            if (ranks.get(i).getRankKey().equalsIgnoreCase(targetMember.get().getRankKey())) {
                idx = i;
                break;
            }
        }
        int newIdx = up ? idx + 1 : idx - 1;
        if (idx == -1 || newIdx < 0 || newIdx >= ranks.size() || ranks.get(newIdx).getPriority() >= 100) {
            plugin.getMessageService().send(player, "errors.generic");
            return;
        }

        var newRank = ranks.get(newIdx);
        plugin.getMemberService().setRank(target.getUniqueId(), newRank.getRankKey()).whenComplete((v, error) ->
                plugin.getScheduler().runSync(() -> {
                    String key = up ? "clan.promote-success" : "clan.demote-success";
                    plugin.getMessageService().send(player, key, Map.of(
                            "player", target.getName() != null ? target.getName() : args[1],
                            "rank", ColorUtil.stripColor(newRank.getDisplay())
                    ));
                }));
    }

    private void handleInfo(Player player, String[] args) {
        Optional<Clan> clanOpt;
        if (args.length >= 2) {
            clanOpt = plugin.getClanService().getClanByName(args[1]);
        } else {
            clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        }
        if (clanOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.invalid-clan");
            return;
        }
        Clan clan = clanOpt.get();
        boolean own = plugin.getClanService().getPlayerClan(player.getUniqueId())
                .map(Clan::getId).map(id -> id.equals(clan.getId())).orElse(false);
        plugin.getGuiManager().clanInfo().open(player, clan, !own);
    }

    private void handleMembers(Player player) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        clanOpt.ifPresent(clan -> plugin.getGuiManager().members().open(player, clan, 0));
    }

    private void handleRequests(Player player) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (!requirePermission(player, ClanPermission.CLAN_ACCEPT_REQUEST)) return;
        plugin.getGuiManager().requests().open(player, clanOpt.get());
    }

    private void handleInvites(Player player) {
        plugin.getGuiManager().invites().open(player);
    }

    private void handleBank(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().bank().open(player, clan,
                me.uc.hussein.ultrasclans.gui.holder.BankGuiHolder.Mode.DEPOSIT));
    }

    private void handleStorage(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().storage().open(player, clan));
    }

    private void handleUpgrades(Player player) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (!requirePermission(player, ClanPermission.CLAN_UPGRADES)) return;
        plugin.getGuiManager().upgrades().open(player, clanOpt.get());
    }

    private void handleWarp(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (args.length < 2) {
            plugin.getMessageService().send(player, "warps.invalid-id");
            return;
        }
        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageService().send(player, "warps.invalid-id");
            return;
        }
        plugin.getWarpService().teleport(player, clanOpt.get(), id, fail -> {
            String key = switch (fail) {
                case NO_PERMISSION -> "general.no-permission";
                case NOT_FOUND -> "warps.not-found";
                case WORLD_UNLOADED -> "warps.world-missing";
                case UNSAFE -> "warps.unsafe";
            };
            plugin.getMessageService().send(player, key, Map.of("id", args[1]));
        });
    }

    private void handleWarps(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().warps().open(player, clan));
    }

    private void handleSetSpawn(Player player) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        plugin.getSpawnService().set(player, clanOpt.get(),
                fail -> plugin.getMessageService().send(player, "general.no-permission"),
                () -> {
                    plugin.getMessageService().send(player, "spawn.set-success");
                    plugin.playSound(player, "success");
                });
    }

    private void handleSpawnUse(Player player) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        plugin.getSpawnService().use(player, clanOpt.get(), fail -> {
            String key = switch (fail) {
                case NO_PERMISSION -> "general.no-permission";
                case NOT_SET -> "spawn.not-set";
                case WORLD_UNLOADED -> "warps.world-missing";
                case UNSAFE -> "warps.unsafe";
            };
            plugin.getMessageService().send(player, key);
        });
    }

    private void handleTpa(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (args.length < 2) {
            plugin.getGuiManager().tpa().open(player, clanOpt.get());
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageService().send(player, "tpa.offline");
            return;
        }
        boolean here = args.length >= 3 && args[2].equalsIgnoreCase("here");
        plugin.getTpaService().sendAndNotify(player, target,
                here ? me.uc.hussein.ultrasclans.tpa.TpaRequest.Mode.HERE
                        : me.uc.hussein.ultrasclans.tpa.TpaRequest.Mode.STANDARD);
    }

    private void handleTpaAccept(Player player, String[] args) {
        UUID filter = null;
        if (args.length >= 2) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            filter = target.getUniqueId();
        }
        plugin.getTpaService().accept(player, filter,
                fail -> plugin.getMessageService().send(player, fail == me.uc.hussein.ultrasclans.tpa.TpaService.RespondFail.NONE_PENDING
                                ? "tpa.none-pending" : "tpa.expired"),
                (sender, request) -> {
                    boolean here = request.getMode() == me.uc.hussein.ultrasclans.tpa.TpaRequest.Mode.HERE;
                    Player toMove = here ? player : sender;
                    Player stayer = here ? sender : player;
                    long delay = plugin.getConfigManager().loadYaml("warps.yml").getLong("tpa-delay-seconds", 3);
                    plugin.getTeleportService().start(toMove, stayer.getLocation(), delay, "warps");
                    plugin.getMessageService().send(player, "tpa.accepted");
                    plugin.getMessageService().send(sender, "tpa.accepted-sender",
                            Map.of("player", player.getName()));
                });
    }

    private void handleChatToggle(Player player, String[] args) {
        if (!requirePermission(player, ClanPermission.CLAN_CHAT)) return;
        boolean newState;
        if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            newState = args[1].equalsIgnoreCase("on");
            plugin.getChatService().setClanChat(player.getUniqueId(), newState);
        } else {
            newState = plugin.getChatService().toggleClanChat(player.getUniqueId());
        }
        plugin.getMessageService().send(player, newState ? "chat.chat-on" : "chat.chat-off");
    }

    private void handleTagToggle(Player player, String[] args) {
        boolean newState;
        if (args.length >= 2 && (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off"))) {
            newState = args[1].equalsIgnoreCase("on");
            if (newState != plugin.getChatService().isTagEnabled(player.getUniqueId())) {
                plugin.getChatService().toggleTag(player.getUniqueId());
            }
        } else {
            newState = plugin.getChatService().toggleTag(player.getUniqueId());
        }
        plugin.getMessageService().send(player, newState ? "chat.tag-on" : "chat.tag-off");
    }

    private void handleAlliance(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        Clan clan = clanOpt.get();
        if (args.length < 3) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan alliance <invite|accept|deny|disband> <clan>");
            return;
        }
        String action = args[1].toLowerCase();
        Optional<Clan> targetOpt = plugin.getClanService().getClanByName(args[2]);
        if (targetOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.invalid-clan");
            return;
        }
        Clan target = targetOpt.get();

        switch (action) {
            case "invite" -> plugin.getAllianceService().sendInvite(player, clan, target, fail -> {
                String key = switch (fail) {
                    case SELF -> "alliance.self";
                    case ALREADY_ALLIED -> "alliance.already-allied";
                    case ALREADY_PENDING -> "alliance.already-pending";
                    case LIMIT_REACHED -> "alliance.limit-reached";
                    case NO_PERMISSION -> "alliance.no-permission";
                case TARGET_DISABLED -> "prefs.blocked-alliance";
                };
                plugin.getMessageService().send(player, key);
            }, () -> plugin.getMessageService().send(player, "alliance.invite-sent",
                    Map.of("clan", target.getName())));

            case "accept" -> {
                boolean allowed = requirePermission(player, ClanPermission.CLAN_ALLIANCE_MANAGE);
                if (!allowed) return;
                plugin.getAllianceService().accept(player, clan, target,
                        fail -> plugin.getMessageService().send(player, fail == me.uc.hussein.ultrasclans.alliance.AllianceService.RespondFail.LIMIT_REACHED
                                        ? "alliance.limit-reached" : "alliance.none-pending"),
                        () -> plugin.getMessageService().send(player, "alliance.accepted",
                                Map.of("clan", target.getName())));
            }

            case "deny" -> plugin.getAllianceService().deny(clan, target).whenComplete((found, err) ->
                    plugin.getScheduler().runSync(() -> {
                        if (Boolean.TRUE.equals(found)) {
                            plugin.getMessageService().send(player, "alliance.denied",
                                    Map.of("clan", target.getName()));
                        } else {
                            plugin.getMessageService().send(player, "alliance.none-pending");
                        }
                    }));

            case "disband" -> {
                if (!requirePermission(player, ClanPermission.CLAN_ALLIANCE_MANAGE)) return;
                var allianceOpt = plugin.getAllianceService().getAlliance(clan.getId(), target.getId());
                if (allianceOpt.isEmpty()) {
                    plugin.getMessageService().send(player, "alliance.not-allied");
                    return;
                }
                var alliance = allianceOpt.get();
                plugin.getAllianceService().disband(alliance.getId(), alliance.getClanA(), alliance.getClanB())
                        .whenComplete((v, err) -> plugin.getScheduler().runSync(() ->
                                plugin.getMessageService().send(player, "alliance.disbanded",
                                        Map.of("clan", target.getName()))));
            }

            default -> plugin.getMessageService().sendLiteral(player, "&cUsage: /clan alliance <invite|accept|deny|disband> <clan>");
        }
    }

    private void handleAlliances(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().alliances().open(player, clan));
    }

    private void handleTasks(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().tasks().open(player, clan));
    }

    private void handleEvents(Player player) {
        requireOwnClan(player).ifPresent(clan -> plugin.getGuiManager().events().open(player, clan));
    }

    private void handleChallenge(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (!requirePermission(player, ClanPermission.CLAN_EVENT_CREATE)) return;
        if (args.length < 2) {
            plugin.getGuiManager().challengeBrowse().open(player, clanOpt.get());
            return;
        }
        Optional<Clan> targetOpt = plugin.getClanService().getClanByName(args[1]);
        if (targetOpt.isEmpty()) {
            plugin.getMessageService().send(player, "errors.invalid-clan");
            return;
        }
        if (plugin.getAllianceService().areAllied(clanOpt.get().getId(), targetOpt.get().getId())) {
            plugin.getMessageService().sendLiteral(player, "&cYou can't challenge an allied clan.");
            return;
        }
        plugin.getGuiManager().challengeSetup().open(player, clanOpt.get(), targetOpt.get());
    }

    private void handleEvent(Player player, String[] args) {
        var eventService = plugin.getEventService();

        if (args.length >= 2 && args[1].equalsIgnoreCase("leave")) {
            if (!eventService.isInActiveEvent(player.getUniqueId())) {
                plugin.getMessageService().send(player, "events.none-pending");
                return;
            }
            if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
                plugin.getMessageService().send(player, "events.leave-confirm-prompt");
                plugin.getMessageService().sendLiteral(player, "&eType &f/clan event leave confirm &eto confirm.");
                return;
            }
            eventService.leaveEvent(player);
            return;
        }

        if (args.length >= 3 && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))) {
            Optional<Clan> own = requireOwnClan(player);
            Optional<Clan> other = plugin.getClanService().getClanByName(args[2]);
            if (own.isEmpty()) return;
            if (other.isEmpty()) {
                plugin.getMessageService().send(player, "errors.invalid-clan");
                return;
            }
            var pending = eventService.findPendingChallenge(own.get().getId(), other.get().getId());
            if (pending.isEmpty()) {
                plugin.getMessageService().send(player, "events.none-pending");
                return;
            }
            if (args[1].equalsIgnoreCase("accept")) {
                eventService.handleAccept(player, pending.get());
            } else {
                eventService.handleDeny(player, pending.get());
            }
            return;
        }

        if (eventService.isInActiveEvent(player.getUniqueId())) {
            plugin.getMessageService().send(player, "events.leave-confirm-prompt");
            plugin.getMessageService().sendLiteral(player, "&eType &f/clan event leave confirm &eto confirm.");
            return;
        }
        handleEvents(player);
    }

    private void handleTransfer(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        Clan clan = clanOpt.get();
        if (!clan.getOwnerUuid().equals(player.getUniqueId())) {
            plugin.getMessageService().send(player, "clan.transfer-not-owner");
            return;
        }
        if (args.length < 2 || args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            plugin.getMessageService().send(player, "clan.transfer-usage");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        var fail = plugin.getClanService().transferOwnership(clan, target.getUniqueId(), player.getUniqueId(), "command");
        if (fail != null) {
            plugin.getMessageService().send(player, "clan.transfer-failed");
            return;
        }
        plugin.getMessageService().send(player, "clan.owner-transfer-success",
                Map.of("player", target.getName() != null ? target.getName() : args[1]));
        Player online = target.getPlayer();
        if (online != null) {
            plugin.getMessageService().send(online, "clan.owner-transfer-success",
                    Map.of("player", online.getName()));
        }
    }

    private static final List<String> CLAN_COLORS = List.of("WHITE", "GRAY", "DARK_GRAY", "BLACK", "RED", "DARK_RED",
            "GOLD", "YELLOW", "GREEN", "DARK_GREEN", "AQUA", "DARK_AQUA", "BLUE", "DARK_BLUE",
            "LIGHT_PURPLE", "DARK_PURPLE");

    private void handleSettings(Player player, String[] args) {
        Optional<Clan> clanOpt = requireOwnClan(player);
        if (clanOpt.isEmpty()) return;
        if (!requirePermission(player, ClanPermission.CLAN_SETTINGS)) return;
        Clan clan = clanOpt.get();
        if (args.length < 2) {
            plugin.getMessageService().sendLiteral(player, "&cUsage: /clan settings <public|private|color <name>|rename <name>>");
            return;
        }
        switch (args[1].toLowerCase()) {
            case "public", "private" -> {
                boolean makePublic = args[1].equalsIgnoreCase("public");
                synchronized (clan) { clan.setPublic(makePublic); }
                plugin.getClanService().persist(clan);
                plugin.getMessageService().send(player, makePublic ? "clan.visibility-public" : "clan.visibility-private");
            }
            case "color" -> {
                if (args.length < 3 || !CLAN_COLORS.contains(args[2].toUpperCase())) {
                    plugin.getMessageService().send(player, "chat.color-invalid",
                            Map.of("colors", String.join(", ", CLAN_COLORS)));
                    return;
                }
                synchronized (clan) { clan.setColor(args[2].toUpperCase()); }
                plugin.getClanService().persist(clan);
                plugin.getMessageService().send(player, "chat.color-changed");
            }
            case "rename" -> {
                if (args.length < 3) {
                    plugin.getMessageService().sendLiteral(player, "&cUsage: /clan settings rename <name>");
                    return;
                }
                String result = plugin.getClanService().rename(clan, args[2]);
                if (result == null) {
                    plugin.getMessageService().send(player, "clan.rename-success", Map.of("clan", args[2]));
                } else if (result.equals("NAME_TAKEN")) {
                    plugin.getMessageService().send(player, "clan.name-taken-during-rename");
                } else {
                    plugin.getMessageService().send(player, "clan.create-name-invalid",
                            Map.of("reason", result.substring("NAME_INVALID:".length())));
                }
            }
            default -> plugin.getMessageService().sendLiteral(player, "&cUsage: /clan settings <public|private|color <name>|rename <name>>");
        }
    }

    public void sendHelp(Player player) {
        plugin.getMessageService().send(player, "general.help-header");
        Map<String, String> descriptions = Map.ofEntries(
                Map.entry("create", "Create a new clan"),
                Map.entry("invite <player>", "Invite a player to your clan"),
                Map.entry("accept <clan>", "Accept a clan invite"),
                Map.entry("deny <clan>", "Deny a clan invite"),
                Map.entry("leave confirm", "Leave your current clan"),
                Map.entry("kick <player>", "Kick a member from your clan"),
                Map.entry("promote <player>", "Promote a member"),
                Map.entry("demote <player>", "Demote a member"),
                Map.entry("info [clan]", "View clan info"),
                Map.entry("members", "View clan members"),
                Map.entry("requests", "View pending join requests"),
                Map.entry("invites", "View your pending invites"),
                Map.entry("bank", "Open the clan bank"),
                Map.entry("storage", "Open the clan storage"),
                Map.entry("upgrades", "Spend CP on clan upgrades"),
                Map.entry("warp <id>", "Teleport to a clan warp"),
                Map.entry("warps", "Manage clan warps"),
                Map.entry("setspawn", "Set the clan spawn"),
                Map.entry("spawn", "Teleport to the clan spawn"),
                Map.entry("tpa [player]", "Send/view TPA requests"),
                Map.entry("tpa_accept [player]", "Accept a TPA request"),
                Map.entry("chat [on|off]", "Toggle clan chat"),
                Map.entry("tag [on|off]", "Toggle clan tag"),
                Map.entry("alliance invite <clan>", "Propose an alliance"),
                Map.entry("alliances", "View and manage your alliances"),
                Map.entry("tasks", "View daily/weekly/monthly tasks"),
                Map.entry("events", "View and manage clan events"),
                Map.entry("challenge <clan>", "Challenge another clan to battle"),
                Map.entry("event leave confirm", "Leave your current event"),
                Map.entry("transfer <player> confirm", "Transfer clan ownership"),
                Map.entry("settings", "Public/private, color, rename")
        );
        for (var entry : descriptions.entrySet()) {
            plugin.getMessageService().send(player, "general.help-line",
                    Map.of("command", "clan " + entry.getKey(), "description", entry.getValue()));
        }
    }

    // ------------------------------------------------------------

    private Optional<Clan> requireOwnClan(Player player) {
        Optional<Clan> clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isEmpty()) {
            plugin.getMessageService().send(player, "clan.not-in-clan");
        }
        return clanOpt;
    }

    private boolean requirePermission(Player player, ClanPermission permission) {
        boolean allowed = plugin.getPermissionService().hasPermission(player.getUniqueId(), permission)
                || plugin.getPermissionService().isOwner(player.getUniqueId());
        if (!allowed) {
            plugin.getMessageService().send(player, "general.no-permission");
        }
        return allowed;
    }

    // ------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return List.of();
        }
        if (args.length == 1) {
            return filterVisibleSubcommands(player, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("invite", "kick", "promote", "demote").contains(sub)) {
                return onlinePlayerNames(args[1]);
            }
            if (List.of("accept", "deny", "info").contains(sub)) {
                return clanNames(args[1]);
            }
            if (List.of("tpa", "tpa_accept").contains(sub)) {
                return onlinePlayerNames(args[1]);
            }
            if (List.of("chat", "tag").contains(sub)) {
                return List.of("on", "off").stream()
                        .filter(o -> o.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (sub.equals("warp")) {
                return List.of("1", "2", "3", "4", "5").stream()
                        .filter(o -> o.startsWith(args[1])).collect(Collectors.toList());
            }
            if (sub.equals("challenge")) {
                return clanNames(args[1]);
            }
            if (sub.equals("transfer")) {
                return onlinePlayerNames(args[1]);
            }
            if (sub.equals("settings")) {
                return List.of("public", "private", "color", "rename").stream()
                        .filter(o -> o.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (sub.equals("event")) {
                return List.of("accept", "deny", "leave").stream()
                        .filter(o -> o.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (sub.equals("alliance")) {
                return List.of("invite", "accept", "deny", "disband").stream()
                        .filter(o -> o.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
            if (sub.equals("leave")) {
                return List.of("confirm").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("alliance")
                || (args[0].equalsIgnoreCase("event")
                && (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase("deny"))))) {
            return clanNames(args[2]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("settings") && args[1].equalsIgnoreCase("color")) {
            return CLAN_COLORS.stream().filter(c -> c.startsWith(args[2].toUpperCase())).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("transfer")) {
            return List.of("confirm");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("leave")) {
            return List.of("confirm");
        }
        return List.of();
    }

    private List<String> filterVisibleSubcommands(Player player, String prefix) {
        UUID uuid = player.getUniqueId();
        boolean inClan = plugin.getClanService().isInClan(uuid);
        List<String> visible = new ArrayList<>();
        for (String sub : SUBCOMMANDS) {
            boolean requiresClan = List.of("invite", "leave", "kick", "promote", "demote", "members", "requests",
                    "bank", "storage", "upgrades", "warp", "warps", "setspawn", "spawn",
                    "tpa", "tpa_accept", "chat", "tag", "alliance", "alliances", "tasks", "quests",
                    "event", "events", "challenge", "transfer", "settings").contains(sub);
            boolean requiresNoClan = sub.equals("create");
            if (requiresClan && !inClan) continue;
            if (requiresNoClan && inClan) continue;
            visible.add(sub);
        }
        return visible.stream().filter(s -> s.startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }

    private List<String> onlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }

    private List<String> clanNames(String prefix) {
        return plugin.getClanService().getAllClansSnapshot().stream()
                .map(Clan::getName)
                .filter(name -> name.toLowerCase().startsWith(prefix.toLowerCase()))
                .collect(Collectors.toList());
    }
}
