package me.uc.hussein.ultrasclans.listener;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;
import me.uc.hussein.ultrasclans.task.TaskType;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class MiningListener implements Listener {

    private final UltrasClansPlugin plugin;
    private final NamespacedKey placedKey;

    public MiningListener(UltrasClansPlugin plugin) {
        this.plugin = plugin;
        this.placedKey = new NamespacedKey(plugin, "player_placed");
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Chunk chunk = block.getChunk();

        List<String> placed = new ArrayList<>(
                chunk.getPersistentDataContainer().getOrDefault(
                        placedKey,
                        PersistentDataType.LIST.strings(),
                        List.of()
                )
        );

        String location = locationKey(block);
        if (!placed.contains(location)) {
            placed.add(location);
            chunk.getPersistentDataContainer().set(
                    placedKey,
                    PersistentDataType.LIST.strings(),
                    placed
            );
        }

        plugin.getClanService().getPlayerClan(event.getPlayer().getUniqueId())
                .ifPresent(clan -> plugin.getTaskService()
                        .incrementProgress(clan, TaskType.PLACE_BLOCKS, 1));
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Block block = event.getBlock();
        Chunk chunk = block.getChunk();

        List<String> placed = new ArrayList<>(
                chunk.getPersistentDataContainer().getOrDefault(
                        placedKey,
                        PersistentDataType.LIST.strings(),
                        List.of()
                )
        );

        String location = locationKey(block);

        if (placed.remove(location)) {
            if (placed.isEmpty()) {
                chunk.getPersistentDataContainer().remove(placedKey);
            } else {
                chunk.getPersistentDataContainer().set(
                        placedKey,
                        PersistentDataType.LIST.strings(),
                        placed
                );
            }
            return;
        }

        plugin.getMiningProgressManager()
                .recordBlockBreak(event.getPlayer(), block.getType().name());

        plugin.getClanService().getPlayerClan(event.getPlayer().getUniqueId())
                .ifPresent(clan -> plugin.getTaskService()
                        .incrementProgress(clan, TaskType.MINE_BLOCKS, 1));
    }

    private String locationKey(Block block) {
        return block.getX() + "," + block.getY() + "," + block.getZ();
    }
}
