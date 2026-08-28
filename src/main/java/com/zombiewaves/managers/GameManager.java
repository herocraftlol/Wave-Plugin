package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    private final ZombieWaves plugin;
    private boolean gameRunning = false;
    private int currentWave = 0;
    private String selectedArena = null;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private BukkitRunnable countdownTask;
    private int countdownSeconds;

    public GameManager(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // Reset game state
    }

    public boolean isGameRunning() {
        return gameRunning;
    }

    public String getSelectedArena() {
        return selectedArena;
    }

    public void setSelectedArena(String arenaName) {
        this.selectedArena = arenaName;
    }

    public void startGame() {
        if (gameRunning) return;
        
        // Check if arena is selected
        if (selectedArena == null) {
            plugin.getLogger().warning("No arena selected! Use /wave selectarena <name>");
            return;
        }
        
        gameRunning = true;
        currentWave = 0;
        playerDataMap.clear();
        
        // Initialize player data for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerDataMap.put(player.getUniqueId(), new PlayerData());
        }
        
        // Broadcast start message
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("game-start"));
        
        // Start first wave after grace period
        int gracePeriod = plugin.getConfigManager().getGracePeriod();
        startCountdown(gracePeriod);
    }

    public void stopGame() {
        gameRunning = false;
        currentWave = 0;
        
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        // Clear all active mobs
        plugin.getWaveManager().clearAllMobs();
        
        // Reset player data
        playerDataMap.clear();
    }

    public void startCountdown(int seconds) {
        countdownSeconds = seconds;
        
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameRunning) {
                    cancel();
                    return;
                }
                
                countdownSeconds--;
                
                if (countdownSeconds <= 0) {
                    cancel();
                    nextWave();
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void nextWave() {
        if (!gameRunning) return;
        
        currentWave++;
        
        // Check if we've completed all waves
        if (currentWave > plugin.getConfigManager().getTotalWaves()) {
            endGame();
            return;
        }
        
        // Start the wave
        plugin.getWaveManager().startWave(currentWave);
    }

    private void endGame() {
        int maxWave = plugin.getConfigManager().getTotalWaves();
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("game-over")
                .replace("{wave}", String.valueOf(maxWave)));
        
        stopGame();
    }

    public void onPlayerJoin(Player player) {
        if (!playerDataMap.containsKey(player.getUniqueId())) {
            playerDataMap.put(player.getUniqueId(), new PlayerData());
        }
    }

    public void onPlayerQuit(Player player) {
        // Keep player data for when they rejoin
    }

    public void addKill(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.addKill();
        }
    }

    public void addGold(Player player, int amount) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null) {
            data.addGold(amount);
        }
    }

    public boolean removeGold(Player player, int amount) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        if (data != null && data.getGold() >= amount) {
            data.removeGold(amount);
            return true;
        }
        return false;
    }

    public int getPlayerGold(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        return data != null ? data.getGold() : 0;
    }

    public int getPlayerKills(Player player) {
        PlayerData data = playerDataMap.get(player.getUniqueId());
        return data != null ? data.getKills() : 0;
    }

    public int getCurrentWave() {
        return currentWave;
    }

    public void setCurrentWave(int wave) {
        this.currentWave = wave;
    }

    public int getMaxWave() {
        return plugin.getConfigManager().getTotalWaves();
    }

    public int getRemainingMobs() {
        return plugin.getWaveManager().getRemainingMobs();
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public PlayerData getPlayerData(Player player) {
        return playerDataMap.get(player.getUniqueId());
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        return playerDataMap;
    }
}