package me.uc.hussein.ultrasclans.service;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.model.Clan;
import me.uc.hussein.ultrasclans.model.ClanPermission;
import me.uc.hussein.ultrasclans.model.PlayerPrefs;
import me.uc.hussein.ultrasclans.model.PlayerPrefs.Setting;
import me.uc.hussein.ultrasclans.repository.PrefsRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * تفضيلات اللاعبين الشخصية. تُحمَّل كلها عند الإقلاع (الجدول لا يحتوي إلا من غيّر شيئًا)
 * فتبقى القراءات متزامنة وفورية حتى للاعبين غير المتصلين - وهذا ما يسمح بفحص
 * "هل يقبل مدراء الكلان طلبات التحالف؟" دون استعلام قاعدة بيانات.
 */
public final class PrefsService {

    private final UltrasClansPlugin plugin;
    private final PrefsRepository repository;
    private final Map<UUID, PlayerPrefs> cache = new ConcurrentHashMap<>();

    public PrefsService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new PrefsRepository(plugin.getDatabaseManager());
    }

    public CompletableFuture<Void> loadAll() {
        return plugin.getScheduler().supplyAsync(repository::loadAll)
                .thenAccept(cache::putAll);
    }

    public PlayerPrefs get(UUID uuid) {
        return cache.getOrDefault(uuid, PlayerPrefs.DEFAULT);
    }

    private void store(UUID uuid, PlayerPrefs prefs) {
        cache.put(uuid, prefs);
        plugin.getScheduler().runAsync(() -> repository.save(uuid, prefs));
    }

    /** @return القيمة الجديدة بعد القلب */
    public boolean toggle(UUID uuid, Setting setting) {
        PlayerPrefs current = get(uuid);
        boolean next = !setting.read(current);
        store(uuid, current.with(setting, next));
        return next;
    }

    public void setLanguage(UUID uuid, String language) {
        store(uuid, get(uuid).withLanguage(language));
    }

    public void reset(UUID uuid) {
        store(uuid, PlayerPrefs.DEFAULT);
    }

    /**
     * هل يوجد على الأقل مدير واحد في الكلان (المالك أو حامل الصلاحية) يقبل هذا النوع من الطلبات؟
     * إن عطّل كل المدراء الاستقبال يُرفض الطلب عند المرسل برسالة "تعذر".
     */
    public boolean anyManagerAccepts(Clan clan, ClanPermission permission, Setting setting) {
        boolean foundManager = false;
        for (var member : plugin.getClanService().getMembers(clan.getId())) {
            boolean manager = clan.getOwnerUuid().equals(member.getUuid())
                    || plugin.getPermissionService().hasPermission(member.getUuid(), permission);
            if (!manager) continue;
            foundManager = true;
            if (setting.read(get(member.getUuid()))) {
                return true;
            }
        }
        return !foundManager;
    }
}
