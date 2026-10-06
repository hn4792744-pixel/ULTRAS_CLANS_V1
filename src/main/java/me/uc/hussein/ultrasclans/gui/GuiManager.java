package me.uc.hussein.ultrasclans.gui;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import org.bukkit.entity.Player;

/**
 * نقطة دخول مركزية لكل واجهات البلاجن. الأوامر والمستمعون (listeners)
 * يتعاملون فقط مع هذه الفئة بدلًا من إنشاء كل GUI class يدويًا.
 */
public final class GuiManager {

    private final UltrasClansPlugin plugin;
    private final ClanMainGui clanMainGui;
    private final MyClanGui myClanGui;
    private final ClansListGui clansListGui;
    private final ClanInfoGui clanInfoGui;
    private final MembersGui membersGui;
    private final MemberInfoGui memberInfoGui;
    private final RequestsGui requestsGui;
    private final InvitesGui invitesGui;
    private final BankGui bankGui;
    private final StorageGui storageGui;
    private final UpgradesGui upgradesGui;
    private final BankInteraction bankInteraction;
    private final WarpsGui warpsGui;
    private final ConfirmGui confirmGui;
    private final TpaGui tpaGui;
    private final TpaPlayerInfoGui tpaPlayerInfoGui;
    private final AlliancesGui alliancesGui;
    private final AllianceBrowseGui allianceBrowseGui;
    private final AllianceInfoGui allianceInfoGui;
    private final AllianceSettingsGui allianceSettingsGui;
    private final TasksGui tasksGui;
    private final EventsGui eventsGui;
    private final ChallengeBrowseGui challengeBrowseGui;
    private final ChallengeSetupGui challengeSetupGui;
    private final TopGui topGui;
    private final StatsGui statsGui;
    private final PendingInputManager pendingInputManager = new PendingInputManager();

    public GuiManager(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.clanMainGui = new ClanMainGui(plugin);
        this.myClanGui = new MyClanGui(plugin);
        this.clansListGui = new ClansListGui(plugin);
        this.clanInfoGui = new ClanInfoGui(plugin);
        this.membersGui = new MembersGui(plugin);
        this.memberInfoGui = new MemberInfoGui(plugin);
        this.requestsGui = new RequestsGui(plugin);
        this.invitesGui = new InvitesGui(plugin);
        this.bankGui = new BankGui(plugin);
        this.storageGui = new StorageGui(plugin);
        this.upgradesGui = new UpgradesGui(plugin);
        this.bankInteraction = new BankInteraction(plugin);
        this.warpsGui = new WarpsGui(plugin);
        this.confirmGui = new ConfirmGui(plugin);
        this.tpaGui = new TpaGui(plugin);
        this.tpaPlayerInfoGui = new TpaPlayerInfoGui(plugin);
        this.alliancesGui = new AlliancesGui(plugin);
        this.allianceBrowseGui = new AllianceBrowseGui(plugin);
        this.allianceInfoGui = new AllianceInfoGui(plugin);
        this.allianceSettingsGui = new AllianceSettingsGui(plugin);
        this.tasksGui = new TasksGui(plugin);
        this.eventsGui = new EventsGui(plugin);
        this.challengeBrowseGui = new ChallengeBrowseGui(plugin);
        this.challengeSetupGui = new ChallengeSetupGui(plugin);
        this.topGui = new TopGui(plugin);
        this.statsGui = new StatsGui(plugin);
    }

    /** يفتح الواجهة المناسبة تلقائيًا حسب امتلاك اللاعب لكلان أم لا (قسم 5). */
    public void openClanRoot(Player player) {
        var clanOpt = plugin.getClanService().getPlayerClan(player.getUniqueId());
        if (clanOpt.isPresent()) {
            myClanGui.open(player, clanOpt.get());
        } else {
            clanMainGui.open(player);
        }
    }

    public ClanMainGui clanMain() {
        return clanMainGui;
    }

    public MyClanGui myClan() {
        return myClanGui;
    }

    public ClansListGui clansList() {
        return clansListGui;
    }

    public ClanInfoGui clanInfo() {
        return clanInfoGui;
    }

    public MembersGui members() {
        return membersGui;
    }

    public MemberInfoGui memberInfo() {
        return memberInfoGui;
    }

    public RequestsGui requests() {
        return requestsGui;
    }

    public InvitesGui invites() {
        return invitesGui;
    }

    public PendingInputManager pendingInput() {
        return pendingInputManager;
    }

    public BankGui bank() {
        return bankGui;
    }

    public StorageGui storage() {
        return storageGui;
    }

    public UpgradesGui upgrades() {
        return upgradesGui;
    }

    public BankInteraction bankInteraction() {
        return bankInteraction;
    }

    public WarpsGui warps() {
        return warpsGui;
    }

    public ConfirmGui confirm() {
        return confirmGui;
    }

    public TpaGui tpa() {
        return tpaGui;
    }

    public TpaPlayerInfoGui tpaPlayerInfo() {
        return tpaPlayerInfoGui;
    }

    public AlliancesGui alliances() {
        return alliancesGui;
    }

    public AllianceBrowseGui allianceBrowse() {
        return allianceBrowseGui;
    }

    public AllianceInfoGui allianceInfo() {
        return allianceInfoGui;
    }

    public AllianceSettingsGui allianceSettings() {
        return allianceSettingsGui;
    }

    public TasksGui tasks() {
        return tasksGui;
    }

    public EventsGui events() {
        return eventsGui;
    }

    public ChallengeBrowseGui challengeBrowse() {
        return challengeBrowseGui;
    }

    public ChallengeSetupGui challengeSetup() {
        return challengeSetupGui;
    }

    public TopGui top() {
        return topGui;
    }

    public StatsGui stats() {
        return statsGui;
    }
}
