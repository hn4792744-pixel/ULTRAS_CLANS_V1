package me.uc.hussein.ultrasclans.teleport;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Optional;

/**
 * يبحث عن أقرب مكان آمن للوقوف حول موقع محفوظ (قسم 23):
 * أرضية صلبة، مساحة 1x2 هواء، بدون حمم/ماء/صبار/اختناق.
 */
public final class SafeLocationFinder {

    private SafeLocationFinder() {
    }

    public static Optional<Location> find(Location center, int radius) {
        if (isSafe(center)) {
            return Optional.of(center.clone().add(0.5, 0, 0.5));
        }

        for (int r = 1; r <= radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue; // نبحث في حلقة (ring) بدل مسح المربع كاملًا كل مرة
                    }
                    for (int dy = -2; dy <= 2; dy++) {
                        Location candidate = center.clone().add(dx, dy, dz);
                        if (isSafe(candidate)) {
                            return Optional.of(candidate.add(0.5, 0, 0.5));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static boolean isSafe(Location loc) {
        if (loc.getWorld() == null) {
            return false;
        }
        Block feet = loc.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block floor = feet.getRelative(0, -1, 0);

        if (!floor.getType().isSolid() || isDangerous(floor.getType())) {
            return false;
        }
        return isPassable(feet.getType()) && isPassable(head.getType());
    }

    private static boolean isPassable(Material material) {
        if (material.isAir()) {
            return true;
        }
        return switch (material) {
            case WATER, LAVA, CACTUS, FIRE, SOUL_FIRE, MAGMA_BLOCK, SWEET_BERRY_BUSH,
                 POWDER_SNOW, WITHER_ROSE -> false;
            default -> !material.isSolid();
        };
    }

    private static boolean isDangerous(Material material) {
        return switch (material) {
            case LAVA, MAGMA_BLOCK, CACTUS, FIRE, SOUL_FIRE, WITHER_ROSE -> true;
            default -> false;
        };
    }
}
