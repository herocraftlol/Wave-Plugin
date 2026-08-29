package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameManager {

    private final ZombieWaves plugin;
    private boolean gameRunning = false;
    private int currentWave = 0;
    private String selectedArena = null;
    private final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
    private final Set<UUID> activePlayers = new HashSet<>();
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

    /**
     * Starts the game with exactly the given set of players (the ones who were in this
     * arena's lobby). Only these players get PlayerData/gold/kill tracking and the game
     * scoreboard - not every player currently online on the server.
     */
    public void startGame(Set<UUID> players) {
        if (gameRunning) return;
        
        // Check if arena is selected
        if (selectedArena == null) {
            plugin.getLogger().warning("No arena selected! Use /wave selectarena <name>");
            return;
        }
        
        gameRunning = true;
        currentWave = 0;
        playerDataMap.clear();
        activePlayers.clear();
        activePlayers.addAll(players);
        
        for (UUID id : players) {
            playerDataMap.put(id, new PlayerData());
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
        selectedArena = null;
        activePlayers.clear();
        
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        // Clear all active mobs
        plugin.getWaveManager().clearAllMobs();
        
        // Reset player data
        playerDataMap.clear();
        
        // Free up every arena's "active" flag so a new one can start
        plugin.getArenaManager().clearActiveArena();
    }

    public boolean isActivePlayer(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }

    public int getActivePlayerCount() {
        return activePlayers.size();
    }

    /**
     * Removes a player from the running game (they used /wave leave mid-game, or
     * disconnected). If they were the last one left, the game auto-stops and the arena
     * resets so it's immediately available for a new lobby.
     */
    public void removePlayer(Player player) {
        UUID id = player.getUniqueId();
        if (!activePlayers.remove(id)) return;
        playerDataMap.remove(id);

        if (gameRunning && activePlayers.isEmpty()) {
            Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() +
                "§eArena is empty, resetting the game...");
            stopGame();
        }
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
        // No-op: a (re)connecting player only gets tracked once they actually join an
        // arena via /wave join - see LobbyManager. Previously this gave every single
        // connecting player a PlayerData entry (and by extension gold/kill tracking)
        // regardless of whether they were anywhere near the game.
    }

    public void onPlayerQuit(Player player) {
        // Disconnecting counts as leaving the arena outright (see PlayerQuitListener),
        // so there is no lingering PlayerData to keep around for a "rejoin".
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