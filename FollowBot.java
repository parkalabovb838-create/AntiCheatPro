package com.good.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FollowBot {

    private final AntiCheatPlugin plugin;
    private final Map<UUID, BukkitRunnable> tasks = new HashMap<>();
    private final Map<UUID, Boolean> active = new HashMap<>();

    public FollowBot(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public void startFollow(Player target) {
        stopFollow(target);

        active.put(target.getUniqueId(), true);

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isOnline() || !active.getOrDefault(target.getUniqueId(), false)) {
                    cancel();
                    return;
                }

                ticks++;

                if (ticks % 20 == 0) {
                    plugin.getFileLogger().log(target.getName(),
                            "FOLLOW_BOT tracking at "
                                    + String.format("%.1f, %.1f, %.1f",
                                    target.getLocation().getX(),
                                    target.getLocation().getY(),
                                    target.getLocation().getZ()));
                }
            }
        };

        task.runTaskTimer(plugin, 0L, 2L);
        tasks.put(target.getUniqueId(), task);

        plugin.getLogger().info("[ANTICHEAT] Follow-бот запущен для " + target.getName());
        target.sendMessage("§c[ANTICHEAT] §fЗа тобой следит бот проверки.");
    }

    public void stopFollow(Player target) {
        active.put(target.getUniqueId(), false);

        BukkitRunnable task = tasks.remove(target.getUniqueId());
        if (task != null) task.cancel();

        active.remove(target.getUniqueId());

        if (target.isOnline()) {
            target.sendMessage("§c[ANTICHEAT] §fБот проверки убран.");
        }
    }

    public void startPatrol() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hasBot(p)) {
                startFollow(p);
            }
        }
    }

    public void stopAllPatrol() {
        new java.util.ArrayList<>(active.keySet()).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) stopFollow(p);
        });
        active.clear();
        tasks.clear();
    }

    public boolean hasBot(Player p) {
        return active.getOrDefault(p.getUniqueId(), false);
    }

    public int count() {
        return active.size();
    }
}