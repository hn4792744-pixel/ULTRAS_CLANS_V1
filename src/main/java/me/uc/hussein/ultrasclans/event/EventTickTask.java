package me.uc.hussein.ultrasclans.event;

import me.uc.hussein.ultrasclans.UltrasClansPlugin;

public final class EventTickTask implements Runnable {

    private static final long PERIOD_TICKS = 20L * 5L;

    private final UltrasClansPlugin plugin;

    public EventTickTask(UltrasClansPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, this, PERIOD_TICKS, PERIOD_TICKS);
    }

    @Override
    public void run() {
        plugin.getEventService().tick();
    }
}
