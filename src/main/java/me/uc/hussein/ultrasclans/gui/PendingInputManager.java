package me.uc.hussein.ultrasclans.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * يتتبع اللاعبين الذين يُنتظر منهم كتابة شيء في الشات (مثل اسم كلان
 * جديد بعد الضغط على Create - قسم 6). يُستخدم من ChatInputListener.
 */
public final class PendingInputManager {

    public enum InputType { CREATE_CLAN_NAME, RENAME_CLAN, SEARCH_CLANS, BANK_DEPOSIT_CUSTOM, BANK_WITHDRAW_CUSTOM }

    public record PendingInput(InputType type, long expiresAt) {
    }

    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public void request(UUID playerUuid, InputType type, long timeoutSeconds) {
        pending.put(playerUuid, new PendingInput(type, System.currentTimeMillis() + (timeoutSeconds * 1000)));
    }

    public PendingInput get(UUID playerUuid) {
        PendingInput input = pending.get(playerUuid);
        if (input == null) {
            return null;
        }
        if (System.currentTimeMillis() > input.expiresAt()) {
            pending.remove(playerUuid);
            return null;
        }
        return input;
    }

    public void clear(UUID playerUuid) {
        pending.remove(playerUuid);
    }

    public boolean isPending(UUID playerUuid) {
        return get(playerUuid) != null;
    }
}
