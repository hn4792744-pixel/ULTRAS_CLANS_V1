package me.uc.hussein.ultrasclans.chat;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.repository.PlayerSettingsRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatService {

    private final UltrasClansPlugin plugin;
    private final PlayerSettingsRepository repository;
    private final Map<UUID, PlayerSettingsRepository.Settings> cache = new ConcurrentHashMap<>();

    public ChatService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new PlayerSettingsRepository(plugin.getDatabaseManager());
    }

    public void loadFor(UUID uuid) {
        plugin.getScheduler().supplyAsync(() -> repository.load(uuid))
                .whenComplete((settings, error) -> cache.put(uuid,
                        error == null ? settings : PlayerSettingsRepository.Settings.DEFAULT));
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
    }

    public boolean isClanChatEnabled(UUID uuid) {
        return cache.getOrDefault(uuid, PlayerSettingsRepository.Settings.DEFAULT).clanChat();
    }

    public boolean isTagEnabled(UUID uuid) {
        return cache.getOrDefault(uuid, PlayerSettingsRepository.Settings.DEFAULT).tagEnabled();
    }

    public boolean toggleClanChat(UUID uuid) {
        var current = cache.getOrDefault(uuid, PlayerSettingsRepository.Settings.DEFAULT);
        var updated = new PlayerSettingsRepository.Settings(!current.clanChat(), current.tagEnabled());
        cache.put(uuid, updated);
        persist(uuid, updated);
        return updated.clanChat();
    }

    public boolean toggleTag(UUID uuid) {
        var current = cache.getOrDefault(uuid, PlayerSettingsRepository.Settings.DEFAULT);
        var updated = new PlayerSettingsRepository.Settings(current.clanChat(), !current.tagEnabled());
        cache.put(uuid, updated);
        persist(uuid, updated);
        return updated.tagEnabled();
    }

    public void setClanChat(UUID uuid, boolean value) {
        var current = cache.getOrDefault(uuid, PlayerSettingsRepository.Settings.DEFAULT);
        if (current.clanChat() == value) return;
        var updated = new PlayerSettingsRepository.Settings(value, current.tagEnabled());
        cache.put(uuid, updated);
        persist(uuid, updated);
    }

    private void persist(UUID uuid, PlayerSettingsRepository.Settings settings) {
        plugin.getScheduler().runAsync(() -> repository.save(uuid, settings));
    }
}
