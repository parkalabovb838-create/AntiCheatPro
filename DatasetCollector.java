package com.good.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DatasetCollector {

    private final AntiCheatPlugin plugin;
    private boolean running = false;
    private BukkitRunnable task;
    private long startTime;
    private static final long DURATION = 10 * 60 * 1000L;

    public DatasetCollector(AntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isRunning() { return running; }

    public void start() {
        if (running) return;
        running = true;
        startTime = System.currentTimeMillis();

        File file = new File(plugin.getDataFolder(), "dataset.csv");
        try (FileWriter fw = new FileWriter(file, false)) {
            fw.write("yaw,pitch,angle,cps,gcd,reach,ai,vl,ping,timestamp\n");
        } catch (IOException e) {
            plugin.getLogger().warning("Dataset: " + e.getMessage());
        }

        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!running) { cancel(); return; }
                if (System.currentTimeMillis() - startTime > DURATION) {
                    stop();
                    plugin.getLogger().info("[ANTICHEAT] Dataset collection finished.");
                    cancel();
                    return;
                }
                collect();
            }
        };
        task.runTaskTimerAsynchronously(plugin, 0L, 20L);
    }

    public void stop() {
        running = false;
        if (task != null) task.cancel();
    }

    private void collect() {
        File file = new File(plugin.getDataFolder(), "dataset.csv");
        try (FileWriter fw = new FileWriter(file, true)) {
            Bukkit.getOnlinePlayers().forEach(p -> {
                PlayerData d = plugin.getData(p.getUniqueId());
                if (d.lastAttack == 0) return;
                try {
                    fw.write(String.format("%.2f,%.2f,%.2f,%d,%.4f,%.2f,%.3f,%d,%d,%d\n",
                            d.lastYaw, d.lastPitch,
                            d.angles.peekLast() != null ? d.angles.peekLast() : 0f,
                            countCps(d),
                            d.yawDeltas.peekLast() != null ? d.yawDeltas.peekLast() : 0f,
                            d.reaches.peekLast() != null ? d.reaches.peekLast() : 0d,
                            d.lastAI,
                            d.killauraVl + d.cpsVl + d.reachVl + d.gcdVl + d.aiVl,
                            p.getPing(),
                            System.currentTimeMillis()));
                } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Dataset write: " + e.getMessage());
        }
    }

    private int countCps(PlayerData d) {
        long now = System.currentTimeMillis();
        int c = 0;
        for (long t : d.clickTimes) if (now - t <= 1000) c++;
        return c;
    }
}