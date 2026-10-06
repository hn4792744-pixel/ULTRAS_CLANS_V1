package me.uc.hussein.ultrasclans.admin;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.repository.AuditLogRepository;
import me.uc.hussein.ultrasclans.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * /uc clan admin ... — كل عملية Force تُسجَّل في audit log (قسم 91).
 * العمليات المدمِّرة (remove, reset, logs reset) تتطلب كلمة confirm في آخر الأمر.
 */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> ACTIONS = List.of(
            "reload", "list", "inspect", "remove", "reset", "points", "level", "rank", "bank", "bank-capacity",
            "storage", "members-capacity", "transfer", "add", "remove-member", "logs",
            "setevent1", "setevent2", "setrest1", "setrest2");

    private final UltrasClansPlugin plugin;

    public AdminCommand(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ultras.clans.admin")) {
            plugin.getMessageService().send(sender, "general.no-permission");
            return true;
        }
        if (args.length < 2 || !args[0].equalsIgnoreCase("clan") || !args[1].equalsIgnoreCase("admin")) {
            msg(sender, "&cUsage: /uc clan admin <action> ...");
            return true;
        }
        if (args.length == 2) {
            if (sender instanceof Player player) {
                plugin.getAdminGui().open(player, 0);
            } else {
                msg(sender, "&7Actions: &f" + String.join(", ", ACTIONS));
            }
            return true;
        }

        String action = args[2].toLowerCase();
        String[] a = Arrays.copyOfRange(args, 3, args.length);
        switch (action) {
            case "reload" -> {
                plugin.reloadConfigAndMessages();
                plugin.getMessageService().send(sender, "general.reload-success");
            }
            case "list" -> handleList(sender);
            case "inspect" -> withClan(sender, a, 0, clan -> inspect(sender, clan));
            case "remove" -> handleRemove(sender, a);
            case "reset" -> handleReset(sender, a);
            case "points" -> handlePoints(sender, a);
            case "level" -> handleLevel(sender, a);
            case "rank" -> handleRank(sender, a);
            case "bank" -> handleBank(sender, a);
            case "bank-capacity" -> handleSimpleSet(sender, a, "BANK_CAPACITY");
            case "storage" -> handleSimpleSet(sender, a, "STORAGE_LEVEL");
            case "members-capacity" -> handleSimpleSet(sender, a, "MEMBER_CAPACITY");
            case "transfer" -> handleTransfer(sender, a);
            case "add" -> handleAdd(sender, a);
            case "remove-member" -> handleRemoveMember(sender, a);
            case "logs" -> handleLogs(sender, a);
            case "setevent1", "setevent2", "setrest1", "setrest2" -> handleArenaPoint(sender, action);
            default -> msg(sender, "&cUnknown action. Available: &f" + String.join(", ", ACTIONS));
        }
        return true;
    }

    // ------------------------------------------------------------ قراءة

    private void handleList(CommandSender sender) {
        List<Clan> clans = plugin.getClanService().getAllClansSnapshot();
        msg(sender, "&c&lULTRAS &8| &7Total clans: &f" + clans.size());
        for (Clan clan : clans) {
            int members = plugin.getClanService().getMembers(clan.getId()).size();
            msg(sender, "&7- &f" + clan.getName() + " &8(&7" + members + "/" + clan.getMemberCapacity()
                    + " members&8, &7CP: " + clan.getCp() + "&8)");
        }
    }

    private void inspect(CommandSender sender, Clan clan) {
        var tier = plugin.getRankTemplate().getTierForCp(clan.getCp());
        String owner = Bukkit.getOfflinePlayer(clan.getOwnerUuid()).getName();
        msg(sender, "&c&lINSPECT &f" + clan.getName() + " &8(" + clan.getId() + ")");
        msg(sender, "&7Owner: &f" + owner + " &7| Public: &f" + clan.isPublic());
        msg(sender, "&7CP: &f" + clan.getCp() + " &7| Tier: &f" + (tier != null ? tier.getName() : "-")
                + " &7| Level: &f" + clan.getClanLevel());
        msg(sender, "&7Members: &f" + plugin.getClanService().getMembers(clan.getId()).size() + "/" + clan.getMemberCapacity());
        msg(sender, "&7Bank: &f" + clan.getBankBalance() + "/" + clan.getBankCapacity()
                + " &7| Storage Lv: &f" + clan.getStorageLevel());
        msg(sender, "&7Warp cap: &f" + clan.getWarpCapacity() + " &7| Alliance cap: &f" + clan.getAllianceCapacity()
                + " &7| Alliances: &f" + plugin.getAllianceService().getAlliances(clan.getId()).size());
    }

    // ------------------------------------------------------------ عمليات مدمِّرة (تتطلب confirm)

    private void handleRemove(CommandSender sender, String[] a) {
        withClan(sender, a, 0, clan -> {
            if (!confirmed(a, 1)) {
                msg(sender, "&eThis deletes the clan permanently. Re-run with &f... remove " + clan.getName() + " confirm");
                return;
            }
            UUID actor = actorOf(sender);
            plugin.getClanService().deleteClan(clan.getId(), "ADMIN_DELETE", actor)
                    .whenComplete((v, e) -> plugin.getScheduler().runSync(() ->
                            msg(sender, e == null ? "&aClan &f" + clan.getName() + " &adeleted." : "&cDeletion failed, see console.")));
        });
    }

    private void handleReset(CommandSender sender, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("all")) {
            if (!confirmed(a, 1)) {
                msg(sender, "&eThis resets EVERY clan. Re-run with &f... reset all confirm");
                return;
            }
            plugin.getClanService().getAllClansSnapshot().forEach(c -> plugin.getClanService().resetClan(c, actorOf(sender)));
            msg(sender, "&aAll clans were reset.");
            return;
        }
        withClan(sender, a, 0, clan -> {
            if (!confirmed(a, 1)) {
                msg(sender, "&eRe-run with &f... reset " + clan.getName() + " confirm");
                return;
            }
            plugin.getClanService().resetClan(clan, actorOf(sender));
            msg(sender, "&aClan &f" + clan.getName() + " &awas reset.");
        });
    }

    // ------------------------------------------------------------ تعديل القيم

    private void handlePoints(CommandSender sender, String[] a) {
        if (a.length < 3) { msg(sender, "&cUsage: points <add|remove> <clan> <amount>"); return; }
        withClan(sender, a, 1, clan -> {
            long amount = parsePositive(a[2]);
            if (amount < 0) { msg(sender, "&cInvalid amount."); return; }
            long before = clan.getCp();
            if (a[0].equalsIgnoreCase("add")) {
                plugin.getPointsService().addCpForClan(clan, amount, "Admin");
            } else if (a[0].equalsIgnoreCase("remove")) {
                long take = Math.min(amount, clan.getCp());
                if (take > 0) plugin.getPointsService().spendCp(clan, take);
            } else {
                msg(sender, "&cUsage: points <add|remove> <clan> <amount>");
                return;
            }
            audit(sender, clan, "ADMIN_CP_" + a[0].toUpperCase(), String.valueOf(amount), String.valueOf(before), String.valueOf(clan.getCp()));
            msg(sender, "&aCP of &f" + clan.getName() + "&a: &f" + before + " &7→ &f" + clan.getCp());
        });
    }

    private void handleLevel(CommandSender sender, String[] a) {
        if (a.length < 3 || !a[0].equalsIgnoreCase("set")) { msg(sender, "&cUsage: level set <clan> <level>"); return; }
        withClan(sender, a, 1, clan -> {
            long level = parsePositive(a[2]);
            if (level < 1 || level > 1000) { msg(sender, "&cLevel must be 1-1000."); return; }
            long before = clan.getClanLevel();
            synchronized (clan) { clan.setClanLevel((int) level); }
            plugin.getClanService().persist(clan);
            audit(sender, clan, "ADMIN_LEVEL_SET", String.valueOf(level), String.valueOf(before), String.valueOf(level));
            msg(sender, "&aLevel of &f" + clan.getName() + " &aset to &f" + level);
        });
    }

    /** Clan Rank مشتق من CP (قسم 12)، لذا "rank set" يضبط CP على الحد الأدنى للرتبة المطلوبة. */
    private void handleRank(CommandSender sender, String[] a) {
        if (a.length < 3 || !a[0].equalsIgnoreCase("set")) { msg(sender, "&cUsage: rank set <clan> <tier>"); return; }
        withClan(sender, a, 1, clan -> {
            var tier = plugin.getRankTemplate().getTiers().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(a[2]) || t.getId().equalsIgnoreCase(a[2])).findFirst();
            if (tier.isEmpty()) { msg(sender, "&cUnknown tier."); return; }
            long before = clan.getCp();
            long target = tier.get().getMinCp();
            if (target > before) plugin.getPointsService().addCpForClan(clan, target - before, "Admin rank set");
            else if (target < before) plugin.getPointsService().spendCp(clan, before - target);
            audit(sender, clan, "ADMIN_RANK_SET", tier.get().getName(), String.valueOf(before), String.valueOf(clan.getCp()));
            msg(sender, "&aCP set to &f" + clan.getCp() + " &a(tier &f" + tier.get().getName() + "&a).");
        });
    }

    private void handleBank(CommandSender sender, String[] a) {
        if (a.length < 3) { msg(sender, "&cUsage: bank <add|remove> <clan> <amount>"); return; }
        withClan(sender, a, 1, clan -> {
            long amount = parsePositive(a[2]);
            if (amount < 0) { msg(sender, "&cInvalid amount."); return; }
            double before;
            synchronized (clan) {
                before = clan.getBankBalance();
                if (a[0].equalsIgnoreCase("add")) {
                    clan.setBankBalance(Math.min(clan.getBankCapacity(), before + amount));
                } else if (a[0].equalsIgnoreCase("remove")) {
                    clan.setBankBalance(Math.max(0, before - amount));
                } else {
                    msg(sender, "&cUsage: bank <add|remove> <clan> <amount>");
                    return;
                }
            }
            plugin.getClanService().persist(clan);
            audit(sender, clan, "ADMIN_BANK_" + a[0].toUpperCase(), String.valueOf(amount),
                    String.valueOf(before), String.valueOf(clan.getBankBalance()));
            msg(sender, "&aBank of &f" + clan.getName() + "&a: &f" + before + " &7→ &f" + clan.getBankBalance());
        });
    }

    private void handleSimpleSet(CommandSender sender, String[] a, String field) {
        if (a.length < 2) { msg(sender, "&cUsage: <action> <clan> <value>"); return; }
        withClan(sender, a, 0, clan -> {
            long value = parsePositive(a[1]);
            if (value < 1) { msg(sender, "&cInvalid value."); return; }
            long before;
            synchronized (clan) {
                switch (field) {
                    case "BANK_CAPACITY" -> { before = (long) clan.getBankCapacity(); clan.setBankCapacity(value); }
                    case "STORAGE_LEVEL" -> {
                        before = clan.getStorageLevel();
                        clan.setStorageLevel((int) Math.min(value, 10));
                    }
                    default -> { before = clan.getMemberCapacity(); clan.setMemberCapacity((int) Math.min(value, 500)); }
                }
            }
            plugin.getClanService().persist(clan);
            audit(sender, clan, "ADMIN_" + field, String.valueOf(value), String.valueOf(before), String.valueOf(value));
            msg(sender, "&a" + field + " of &f" + clan.getName() + " &aupdated.");
        });
    }

    // ------------------------------------------------------------ أعضاء وملكية

    private void handleTransfer(CommandSender sender, String[] a) {
        if (a.length < 2) { msg(sender, "&cUsage: transfer <clan> <player>"); return; }
        withClan(sender, a, 0, clan -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(a[1]);
            var fail = plugin.getClanService().transferOwnership(clan, target.getUniqueId(), actorOf(sender), "admin");
            if (fail == null) {
                msg(sender, "&aOwnership of &f" + clan.getName() + " &atransferred to &f" + a[1]);
            } else {
                msg(sender, "&cFailed: " + fail);
            }
        });
    }

    private void handleAdd(CommandSender sender, String[] a) {
        if (a.length < 2) { msg(sender, "&cUsage: add <player> <clan>"); return; }
        withClan(sender, a, 1, clan -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(a[0]);
            plugin.getMemberService().join(target.getUniqueId(), clan.getId(),
                    fail -> msg(sender, "&cCould not add: " + fail),
                    restored -> {
                        audit(sender, clan, "ADMIN_ADD_MEMBER", a[0], null, null);
                        msg(sender, "&a" + a[0] + " added to &f" + clan.getName());
                    });
        });
    }

    private void handleRemoveMember(CommandSender sender, String[] a) {
        if (a.length < 1) { msg(sender, "&cUsage: remove-member <player>"); return; }
        OfflinePlayer target = Bukkit.getOfflinePlayer(a[0]);
        var memberOpt = plugin.getClanService().getMember(target.getUniqueId());
        if (memberOpt.isEmpty()) { msg(sender, "&cThat player is not in a clan."); return; }
        var clanOpt = plugin.getClanService().getClan(memberOpt.get().getClanId());
        if (clanOpt.isPresent() && clanOpt.get().getOwnerUuid().equals(target.getUniqueId())) {
            msg(sender, "&cThat player owns the clan - transfer ownership or remove the clan instead.");
            return;
        }
        clanOpt.ifPresent(c -> audit(sender, c, "ADMIN_REMOVE_MEMBER", a[0], null, null));
        plugin.getMemberService().kick(target.getUniqueId()).whenComplete((v, e) ->
                plugin.getScheduler().runSync(() -> msg(sender, "&a" + a[0] + " removed from their clan.")));
    }

    // ------------------------------------------------------------ السجلات

    private void handleLogs(CommandSender sender, String[] a) {
        if (a.length >= 1 && a[0].equalsIgnoreCase("reset")) {
            if (!confirmed(a, 1)) {
                msg(sender, "&eThis deletes ALL logs. Re-run with &f... logs reset confirm");
                return;
            }
            plugin.getScheduler().supplyAsync(() -> plugin.getClanService().getAuditRepository().deleteAll())
                    .whenComplete((count, e) -> plugin.getScheduler().runSync(() ->
                            msg(sender, e == null ? "&aDeleted &f" + count + " &alog entries." : "&cFailed.")));
            return;
        }
        UUID clanId = null;
        String filter = null;
        if (a.length >= 1) {
            Optional<Clan> clanOpt = plugin.getClanService().getClanByName(a[0]);
            if (clanOpt.isPresent()) {
                clanId = clanOpt.get().getId();
                if (a.length >= 2) filter = a[1];
            } else {
                filter = a[0];
            }
        }
        final UUID fClan = clanId;
        final String fFilter = filter;
        plugin.getScheduler().supplyAsync(() ->
                plugin.getClanService().getAuditRepository().search(fClan, fFilter, 10)
        ).whenComplete((entries, e) -> plugin.getScheduler().runSync(() -> {
            if (e != null) { msg(sender, "&cLog query failed."); return; }
            msg(sender, "&c&lLOGS &7(latest " + entries.size() + ")");
            SimpleDateFormat fmt = new SimpleDateFormat("MM-dd HH:mm");
            for (AuditLogRepository.Entry en : entries) {
                msg(sender, "&8" + fmt.format(new Date(en.timestamp())) + " &f" + en.action()
                        + " &7amt=" + en.amount() + " " + en.before() + "→" + en.after()
                        + " &8#" + en.correlationId().substring(0, 8));
            }
        }));
    }

    // ------------------------------------------------------------ الساحة

    private void handleArenaPoint(CommandSender sender, String action) {
        if (!(sender instanceof Player player)) {
            msg(sender, "&cThis admin action must be run as a player (it uses your location).");
            return;
        }
        switch (action) {
            case "setevent1" -> plugin.getArenaService().setEvent1(player);
            case "setevent2" -> plugin.getArenaService().setEvent2(player);
            case "setrest1" -> plugin.getArenaService().setRest1(player);
            case "setrest2" -> plugin.getArenaService().setRest2(player);
        }
        msg(sender, "&aArena point &f" + action.substring(3) + " &aset to your current location.");
        if (plugin.getArenaService().get().isFullyConfigured()) {
            msg(sender, "&aThe admin arena is now fully configured and usable for events.");
        }
    }

    // ------------------------------------------------------------ أدوات

    private void withClan(CommandSender sender, String[] a, int index, java.util.function.Consumer<Clan> action) {
        if (a.length <= index) { msg(sender, "&cMissing clan name."); return; }
        Optional<Clan> clanOpt = plugin.getClanService().getClanByName(a[index]);
        if (clanOpt.isEmpty()) { msg(sender, "&cClan not found: &f" + a[index]); return; }
        action.accept(clanOpt.get());
    }

    private boolean confirmed(String[] a, int index) {
        return a.length > index && a[index].equalsIgnoreCase("confirm");
    }

    /** يرفض السالب وNaN والنصوص الطويلة والقيم الضخمة (قسم 55). يُرجع -1 عند أي مشكلة. */
    private long parsePositive(String text) {
        if (text == null || text.length() > 15) return -1;
        try {
            long v = Long.parseLong(text);
            return (v < 0 || v > 1_000_000_000_000L) ? -1 : v;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private UUID actorOf(CommandSender sender) {
        return sender instanceof Player p ? p.getUniqueId() : null;
    }

    private void audit(CommandSender sender, Clan clan, String action, String amount, String before, String after) {
        UUID actor = actorOf(sender);
        String correlation = UUID.randomUUID().toString();
        plugin.getScheduler().runAsync(() -> plugin.getClanService().getAuditRepository()
                .log(correlation, actor, clan.getId(), action, amount, before, after, "admin"));
    }

    private void msg(CommandSender sender, String text) {
        plugin.getMessageService().sendLiteral(sender, text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("ultras.clans.admin")) {
            return List.of(); // لا تظهر إطلاقًا للاعب العادي (قسم 79)
        }
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.add("clan");
        } else if (args.length == 2) {
            options.add("admin");
        } else if (args.length == 3) {
            options.addAll(ACTIONS);
        } else {
            String action = args[2].toLowerCase();
            switch (action) {
                case "points", "bank" -> {
                    if (args.length == 4) options.addAll(List.of("add", "remove"));
                    else if (args.length == 5) options.addAll(clanNames());
                }
                case "level", "rank" -> {
                    if (args.length == 4) options.add("set");
                    else if (args.length == 5) options.addAll(clanNames());
                }
                case "inspect", "remove", "reset", "bank-capacity", "storage", "members-capacity", "transfer" -> {
                    if (args.length == 4) options.addAll(clanNames());
                    if (args.length == 5 && (action.equals("remove") || action.equals("reset"))) options.add("confirm");
                    if (args.length == 5 && action.equals("transfer")) {
                        Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    }
                }
                case "add", "remove-member" -> {
                    if (args.length == 4) Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
                    if (args.length == 5 && action.equals("add")) options.addAll(clanNames());
                }
                case "logs" -> {
                    if (args.length == 4) { options.addAll(clanNames()); options.add("reset"); }
                    if (args.length == 5 && args[3].equalsIgnoreCase("reset")) options.add("confirm");
                }
                default -> { }
            }
        }
        String last = args.length > 0 ? args[args.length - 1] : "";
        return options.stream().filter(s -> s.toLowerCase().startsWith(last.toLowerCase())).collect(Collectors.toList());
    }

    private List<String> clanNames() {
        return plugin.getClanService().getAllClansSnapshot().stream().map(Clan::getName).collect(Collectors.toList());
    }
}
