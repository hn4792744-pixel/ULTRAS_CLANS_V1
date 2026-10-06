package me.uc.hussein.ultrasclans.storage;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.repository.StorageRepository;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageService {

    private final UltrasClansPlugin plugin;
    private final StorageRepository repository;

    /** آخر عملية حفظ لكل كلان - تُسلسل الحفظ لمنع تجاوز لقطة قديمة لقطة أحدث (race condition). */
    private final Map<UUID, CompletableFuture<Void>> saveChains = new java.util.concurrent.ConcurrentHashMap<>();

    public StorageService(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.repository = new StorageRepository(plugin.getDatabaseManager());
    }

    public int getSlotsPerLevel() {
        return plugin.getConfigManager().loadYaml("storage.yml").getInt("slots-per-level", 5);
    }

    public int getUnlockedSlotCount(int storageLevel) {
        return storageLevel * getSlotsPerLevel();
    }

    public CompletableFuture<Map<Integer, ItemStack>> loadContents(UUID clanId) {
        return plugin.getScheduler().supplyAsync(() -> {
            Map<Integer, String> raw = repository.loadAll(clanId);
            Map<Integer, ItemStack> result = new HashMap<>();
            for (var entry : raw.entrySet()) {
                ItemStack item = deserialize(entry.getValue());
                if (item != null) {
                    result.put(entry.getKey(), item);
                }
            }
            return result;
        });
    }

    /**
     * يحفظ كل الـslots المُمرَّرة دفعة واحدة. عمليات الحفظ لنفس الكلان
     * تُنفَّذ بالتسلسل دائمًا (بالترتيب الذي طُلبت به) حتى لا تتخطى لقطة
     * قديمة لقطة أحدث فتتسبب في فقدان أو تكرار عناصر (قسم 55).
     */
    public CompletableFuture<Void> saveContents(UUID clanId, Map<Integer, ItemStack> slots) {
        // نسلسل العناصر إلى نصوص الآن (على الـmain thread) حتى لا نمسّ ItemStack من thread آخر
        Map<Integer, String> serialized = new HashMap<>();
        for (var entry : slots.entrySet()) {
            ItemStack item = entry.getValue();
            serialized.put(entry.getKey(), (item == null || item.getType().isAir()) ? null : serialize(item));
        }

        CompletableFuture<Void> next = new CompletableFuture<>();
        CompletableFuture<Void> previous = saveChains.put(clanId, next);
        Runnable work = () -> plugin.getScheduler().runAsync(() -> {
            for (var entry : serialized.entrySet()) {
                repository.saveSlot(clanId, entry.getKey(), entry.getValue());
            }
        }).whenComplete((v, err) -> {
            if (err != null) {
                plugin.getLogger().severe("Failed to save clan storage " + clanId + ": " + err.getMessage());
            }
            saveChains.remove(clanId, next);
            next.complete(null);
        });

        if (previous == null || previous.isDone()) {
            work.run();
        } else {
            previous.whenComplete((v, err) -> work.run());
        }
        return next;
    }

    private String serialize(ItemStack item) {
        byte[] bytes = item.serializeAsBytes();
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private ItemStack deserialize(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(base64);
            return ItemStack.deserializeBytes(bytes);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize a storage item: " + e.getMessage());
            return null;
        }
    }
}
