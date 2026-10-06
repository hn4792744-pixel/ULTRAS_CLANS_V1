package me.uc.hussein.ultrasclans;

import me.uc.hussein.ultrasclans.admin.AdminCommand;
import me.uc.hussein.ultrasclans.admin.AdminGui;
import me.uc.hussein.ultrasclans.placeholder.UltrasPlaceholderExpansion;
import me.uc.hussein.ultrasclans.alliance.AllianceService;
import me.uc.hussein.ultrasclans.bank.BankService;
import me.uc.hussein.ultrasclans.chat.ChatService;
import me.uc.hussein.ultrasclans.command.ClanCommand;
import me.uc.hussein.ultrasclans.command.ClansCommand;
import me.uc.hussein.ultrasclans.config.ConfigManager;
import me.uc.hussein.ultrasclans.database.DatabaseManager;
import me.uc.hussein.ultrasclans.gui.GuiManager;
import me.uc.hussein.ultrasclans.hook.EconomyHook;
import me.uc.hussein.ultrasclans.listener.ChatInputListener;
import me.uc.hussein.ultrasclans.listener.AlliancePvpListener;
import me.uc.hussein.ultrasclans.listener.ChatListener;
import me.uc.hussein.ultrasclans.listener.CombatListener;
import me.uc.hussein.ultrasclans.listener.GuiClickListener;
import me.uc.hussein.ultrasclans.listener.MiningListener;
import me.uc.hussein.ultrasclans.listener.PlayerConnectionListener;
import me.uc.hussein.ultrasclans.listener.TeleportListener;
import me.uc.hussein.ultrasclans.message.MessageManager;
import me.uc.hussein.ultrasclans.message.MessageService;
import me.uc.hussein.ultrasclans.service.PrefsService;
import me.uc.hussein.ultrasclans.gui.SettingsGui;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanMember;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.points.MiningProgressManager;
import me.uc.hussein.ultrasclans.points.PointsService;
import me.uc.hussein.ultrasclans.rank.RankTemplate;
import me.uc.hussein.ultrasclans.security.ClickThrottle;
import me.uc.hussein.ultrasclans.service.ClanService;
import me.uc.hussein.ultrasclans.service.InviteService;
import me.uc.hussein.ultrasclans.service.MemberService;
import me.uc.hussein.ultrasclans.service.PermissionService;
import me.uc.hussein.ultrasclans.service.RequestService;
import me.uc.hussein.ultrasclans.spawn.SpawnService;
import me.uc.hussein.ultrasclans.storage.StorageService;
import me.uc.hussein.ultrasclans.storage.StorageSessionManager;
import me.uc.hussein.ultrasclans.listener.StorageCloseListener;
import me.uc.hussein.ultrasclans.teleport.TeleportService;
import me.uc.hussein.ultrasclans.tpa.TpaService;
import me.uc.hussein.ultrasclans.task.HousekeepingTask;
import me.uc.hussein.ultrasclans.task.TaskService;
import me.uc.hussein.ultrasclans.task.TaskTickTask;
import me.uc.hussein.ultrasclans.event.ArenaService;
import me.uc.hussein.ultrasclans.event.EventService;
import me.uc.hussein.ultrasclans.event.EventTickTask;
import me.uc.hussein.ultrasclans.listener.EventCombatListener;
import me.uc.hussein.ultrasclans.upgrades.UpgradeService;
import me.uc.hussein.ultrasclans.warp.WarpService;
import me.uc.hussein.ultrasclans.util.Scheduler;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

/**
 * ULTRAS CLANS - v1
 * نقطة الدخول الرئيسية للبلاجن. المسؤولية الوحيدة لهذه الفئة هي
 * تجميع (wiring) كل المدراء والخدمات والأوامر والمستمعين معًا - لا
 * يوجد أي منطق أعمال هنا (راجع قسم 88: "لا تجعل Main class ضخمة").
 */
public final class UltrasClansPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private MessageService messageService;
    private PrefsService prefsService;
    private SettingsGui settingsGui;
    private DatabaseManager databaseManager;
    private Scheduler scheduler;
    private RankTemplate rankTemplate;
    private EconomyHook economyHook;
    private ClickThrottle clickThrottle;

    private ClanService clanService;
    private MemberService memberService;
    private PermissionService permissionService;
    private InviteService inviteService;
    private RequestService requestService;

    private PointsService pointsService;
    private MiningProgressManager miningProgressManager;
    private BankService bankService;
    private StorageService storageService;
    private StorageSessionManager storageSessions;
    private UpgradeService upgradeService;
    private TeleportService teleportService;
    private WarpService warpService;
    private SpawnService spawnService;
    private TpaService tpaService;
    private ChatService chatService;
    private AllianceService allianceService;
    private TaskService taskService;
    private TaskTickTask taskTickTask;
    private ArenaService arenaService;
    private EventService eventService;
    private EventTickTask eventTickTask;
    private AdminGui adminGui;

    private GuiManager guiManager;
    private ClanCommand clanCommand;
    private HousekeepingTask housekeepingTask;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        this.configManager = new ConfigManager(this);
        configManager.load();

        this.messageManager = new MessageManager(this);
        messageManager.load();

        this.databaseManager = new DatabaseManager(this);
        try {
            databaseManager.connect();
        } catch (Exception e) {
            getLogger().severe("Failed to connect to the database. Disabling Ultras Clans. Cause: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.scheduler = new Scheduler(this, databaseManager.getExecutor());
        this.prefsService = new PrefsService(this);
        this.messageService = new MessageService(this);
        this.rankTemplate = new RankTemplate(this);
        rankTemplate.load();

        this.economyHook = new EconomyHook(this);
        economyHook.setup();

        long throttleMs = configManager.get().getLong("security.gui-click-throttle-ms", 250);
        this.clickThrottle = new ClickThrottle(throttleMs);

        this.clanService = new ClanService(this);
        this.memberService = new MemberService(this, clanService);
        this.permissionService = new PermissionService(clanService);
        this.inviteService = new InviteService(this, clanService, memberService);
        this.requestService = new RequestService(this, clanService, memberService);

        this.pointsService = new PointsService(this);
        this.miningProgressManager = new MiningProgressManager(this);
        this.bankService = new BankService(this);
        this.storageService = new StorageService(this);
        this.storageSessions = new StorageSessionManager(this);
        this.upgradeService = new UpgradeService(this);
        this.teleportService = new TeleportService(this);
        this.warpService = new WarpService(this);
        this.spawnService = new SpawnService(this);
        this.tpaService = new TpaService(this);
        this.chatService = new ChatService(this);
        this.allianceService = new AllianceService(this);
        this.taskService = new TaskService(this);
        this.arenaService = new ArenaService(this);
        this.eventService = new EventService(this);
        this.adminGui = new AdminGui(this);

        this.guiManager = new GuiManager(this);
        this.settingsGui = new SettingsGui(this);

        this.clanCommand = new ClanCommand(this);
        registerCommand("clan", clanCommand, clanCommand);

        ClansCommand clansCommand = new ClansCommand(this);
        registerCommand("clans", clansCommand, null);

        registerCommand("clansettings", new me.uc.hussein.ultrasclans.command.SettingsCommand(this), null);

        AdminCommand adminCommand = new AdminCommand(this);
        registerCommand("uc", adminCommand, adminCommand);

        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatInputListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new MiningListener(this), this);
        getServer().getPluginManager().registerEvents(new StorageCloseListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new AlliancePvpListener(this), this);
        getServer().getPluginManager().registerEvents(new EventCombatListener(this), this);

        this.housekeepingTask = new HousekeepingTask(this);
        housekeepingTask.start();

        prefsService.loadAll().thenCompose(v -> clanService.loadAll()).thenCompose(v -> allianceService.loadAll()).thenCompose(v -> taskService.loadAll())
                .thenCompose(v -> arenaService.loadAll())
                .whenComplete((v, error) -> {
                    if (error != null) {
                        getLogger().severe("Failed to load clan/alliance/task/arena cache: " + error.getMessage());
                    } else {
                        getLogger().info("Ultras Clans enabled in " + (System.currentTimeMillis() - start) + "ms.");
                    }
                    eventService.loadAll();
                });

        this.taskTickTask = new TaskTickTask(this);
        taskTickTask.start();

        this.eventTickTask = new EventTickTask(this);
        eventTickTask.start();

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UltrasPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hook enabled (%ultras_clan_*%).");
        }
    }

    @Override
    public void onDisable() {
        // إغلاق أي واجهة مخزن مفتوحة الآن يُطلق حفظها النهائي قبل إغلاق قاعدة البيانات
        for (org.bukkit.entity.Player online : getServer().getOnlinePlayers()) {
            if (online.getOpenInventory().getTopInventory().getHolder()
                    instanceof me.uc.hussein.ultrasclans.gui.UltrasGuiHolder) {
                online.closeInventory();
            }
        }
        if (eventService != null) {
            eventService.shutdown();
        }
        if (taskService != null) {
            taskService.flushDirty();
        }
        if (miningProgressManager != null) {
            miningProgressManager.flushAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
        getLogger().info("Ultras Clans disabled - all data flushed.");
    }

    private void registerCommand(String name, org.bukkit.command.CommandExecutor executor,
                                  org.bukkit.command.TabCompleter tabCompleter) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command '" + name + "' is missing from plugin.yml!");
            return;
        }
        command.setExecutor(executor);
        if (tabCompleter != null) {
            command.setTabCompleter(tabCompleter);
        }
    }

    public void reloadConfigAndMessages() {
        configManager.reload();
        messageManager.load();
        rankTemplate.load();
        economyHook.setup();
        if (pointsService != null) {
            pointsService.reload();
        }
        if (taskService != null) {
            taskService.reloadTemplates();
        }
    }

    /** يُشغِّل صوتًا مُعرَّفًا في config.yml إن كانت الأصوات مفعّلة. */
    public void playSound(Player player, String key) {
        if (!configManager.get().getBoolean("sounds.enabled", true)
                || !prefsService.get(player.getUniqueId()).sounds()) {
            return;
        }
        String soundName = configManager.get().getString("sounds." + key, null);
        if (soundName == null) {
            return;
        }
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
            // اسم صوت غير صالح في الـconfig - يُتجاهل بأمان بدلًا من كسر السيرفر
        }
    }

    /** يرسل رسالة لكل أونلاين من أعضاء الكلان الذين يملكون صلاحية معالجة طلبات الانضمام. */
    public void notifyClanManagers(Clan clan, String messageKey, Map<String, String> placeholders) {
        for (ClanMember member : clanService.getMembers(clan.getId())) {
            boolean isManager = clan.getOwnerUuid().equals(member.getUuid())
                    || permissionService.hasPermission(member.getUuid(), ClanPermission.CLAN_ACCEPT_REQUEST);
            if (!isManager || !prefsService.get(member.getUuid()).joinRequests()) {
                continue;
            }
            Player online = getServer().getPlayer(member.getUuid());
            if (online != null) {
                messageService.send(online, messageKey, placeholders);
                playSound(online, "request");
            }
        }
    }

    // ------------------------------------------------------------ Getters

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public RankTemplate getRankTemplate() {
        return rankTemplate;
    }

    public EconomyHook getEconomyHook() {
        return economyHook;
    }

    public ClickThrottle getClickThrottle() {
        return clickThrottle;
    }

    public ClanService getClanService() {
        return clanService;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public InviteService getInviteService() {
        return inviteService;
    }

    public RequestService getRequestService() {
        return requestService;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ClanCommand getClanCommand() {
        return clanCommand;
    }

    public PointsService getPointsService() {
        return pointsService;
    }

    public MiningProgressManager getMiningProgressManager() {
        return miningProgressManager;
    }

    public BankService getBankService() {
        return bankService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public UpgradeService getUpgradeService() {
        return upgradeService;
    }

    public StorageSessionManager getStorageSessions() {
        return storageSessions;
    }

    public TeleportService getTeleportService() {
        return teleportService;
    }

    public WarpService getWarpService() {
        return warpService;
    }

    public SpawnService getSpawnService() {
        return spawnService;
    }

    public TpaService getTpaService() {
        return tpaService;
    }

    public ChatService getChatService() {
        return chatService;
    }

    public AllianceService getAllianceService() {
        return allianceService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public ArenaService getArenaService() {
        return arenaService;
    }

    public EventService getEventService() {
        return eventService;
    }

    public AdminGui getAdminGui() {
        return adminGui;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public PrefsService getPrefsService() {
        return prefsService;
    }

    public SettingsGui getSettingsGui() {
        return settingsGui;
    }
}
