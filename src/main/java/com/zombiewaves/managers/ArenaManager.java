package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.Arena;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ArenaManager {

    private final ZombieWaves plugin;
    private final Map<String, Arena> arenas;
    private final Map<UUID, String> playerArenas;
    private final Map<UUID, Location> playerPos1;
    private final Map<UUID, Location> playerPos2;
    private Location globalLobbyLocation;
    private Location globalExitLocation;
    private final File arenasFile;

    public ArenaManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.arenas = new HashMap<>();
        this.playerArenas = new HashMap<>();
        this.playerPos1 = new HashMap<>();
        this.playerPos2 = new HashMap<>();
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
        loadGlobalLocations();
    }

    public void loadArenas() {
        arenas.clear();
        
        if (!arenasFile.exists()) {
            plugin.saveResource("arenas.yml", false);
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        
        if (config.contains("arenas")) {
            for (String name : config.getConfigurationSection("arenas").getKeys(false)) {
                Arena arena = config.getSerializable("arenas." + name, Arena.class);
                if (arena != null) {
                    arenas.put(name.toLowerCase(), arena);
                }
            }
        }
        
        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    private void loadGlobalLocations() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        
        if (config.contains("globalLobby")) {
            globalLobbyLocation = stringToLoc(config.getString("globalLobby"));
        }
        if (config.contains("globalExit")) {
            globalExitLocation = stringToLoc(config.getString("globalExit"));
        }
    }

    public void saveArenas() {
        FileConfiguration config = YamlConfiguration.loadConfiguration(arenasFile);
        config.set("arenas", null);
        
        for (Map.Entry<String, Arena> entry : arenas.entrySet()) {
            config.set("arenas." + entry.getKey(), entry.getValue());
        }
        
        if (globalLobbyLocation != null) {
            config.set("globalLobby", locToString(globalLobbyLocation));
        }
        if (globalExitLocation != null) {
            config.set("globalExit", locToString(globalExitLocation));
        }
        
        try {
            config.save(arenasFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save arenas: " + e.getMessage());
        }
    }

    public Arena createArena(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) {
            return arenas.get(key);
        }
        
        Arena arena = new Arena(name);
        arenas.put(key, arena);
        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        if (arenas.remove(key) != null) {
            saveArenas();
            return true;
        }
        return false;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Arena getActiveArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isActive()) {
                return arena;
            }
        }
        return null;
    }

    public void setActiveArena(String name) {
        // Deactivate all arenas
        for (Arena arena : arenas.values()) {
            arena.setActive(false);
        }
        
        // Activate the specified arena
        Arena arena = getArena(name);
        if (arena != null) {
            arena.setActive(true);
        }
        
        saveArenas();
    }

    /** Deactivates every arena without activating a new one (game ended / arena reset). */
    public void clearActiveArena() {
        for (Arena arena : arenas.values()) {
            arena.setActive(false);
        }
        saveArenas();
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public boolean arenaExists(String name) {
        return arenas.containsKey(name.toLowerCase());
    }

    // Player arena tracking
    public void setPlayerArena(UUID playerId, String arenaName) {
        playerArenas.put(playerId, arenaName.toLowerCase());
    }

    public String getPlayerArena(UUID playerId) {
        return playerArenas.get(playerId);
    }

    public void removePlayerArena(UUID playerId) {
        playerArenas.remove(playerId);
    }

    public boolean isPlayerInArena(UUID playerId) {
        return playerArenas.containsKey(playerId);
    }

    public int getPlayerCountInArena(String arenaName) {
        return (int) playerArenas.values().stream()
            .filter(name -> name.equalsIgnoreCase(arenaName))
            .count();
    }

    public int getMaxPlayersPerArena() {
        return plugin.getConfigManager().getDefaultMaxPlayers();
    }

    /** Effective max players for a specific arena: its own override if set, else the global default. */
    public int getMaxPlayersPerArena(String arenaName) {
        Arena arena = getArena(arenaName);
        if (arena != null && arena.getMaxPlayers() > 0) {
            return arena.getMaxPlayers();
        }
        return getMaxPlayersPerArena();
    }

    /** Effective min players for a specific arena: its own override if set, else the global default. */
    public int getMinPlayersPerArena(String arenaName) {
        Arena arena = getArena(arenaName);
        if (arena != null && arena.getMinPlayers() > 0) {
            return arena.getMinPlayers();
        }
        return plugin.getConfigManager().getMinPlayers();
    }

    public boolean isArenaFull(String arenaName) {
        return getPlayerCountInArena(arenaName) >= getMaxPlayersPerArena(arenaName);
    }

    // Global locations
    public Location getGlobalLobbyLocation() {
        return globalLobbyLocation;
    }

    public void setGlobalLobbyLocation(Location location) {
        this.globalLobbyLocation = location;
        saveArenas();
    }

    public Location getGlobalExitLocation() {
        return globalExitLocation;
    }

    public void setGlobalExitLocation(Location location) {
        this.globalExitLocation = location;
        saveArenas();
    }

    // Position selection for players
    public void setPlayerPos1(UUID playerId, Location location) {
        playerPos1.put(playerId, location);
    }

    public void setPlayerPos2(UUID playerId, Location location) {
        playerPos2.put(playerId, location);
    }

    public Location getPlayerPos1(UUID playerId) {
        return playerPos1.get(playerId);
    }

    public Location getPlayerPos2(UUID playerId) {
        return playerPos2.get(playerId);
    }

    public void clearPlayerPositions(UUID playerId) {
        playerPos1.remove(playerId);
        playerPos2.remove(playerId);
    }

    // Arena editing
    public void setArenaPos1(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.setPos1(location);
            saveArenas();
        }
    }

    public void setArenaPos2(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.setPos2(location);
            saveArenas();
        }
    }

    public void addArenaSpawnPoint(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.addSpawnPoint(location);
            saveArenas();
        }
    }

    public void removeArenaSpawnPoint(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.removeSpawnPoint(location);
            saveArenas();
        }
    }

    public void setArenaLobby(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.setLobbyLocation(location);
            saveArenas();
        }
    }

    public void setArenaGameSpawn(String arenaName, Location location) {
        Arena arena = getArena(arenaName);
        if (arena != null) {
            arena.setGameSpawnLocation(location);
            saveArenas();
        }
    }

    public boolean setArenaMinPlayers(String arenaName, int minPlayers) {
        Arena arena = getArena(arenaName);
        if (arena == null) return false;
        arena.setMinPlayers(minPlayers);
        saveArenas();
        return true;
    }

    public boolean setArenaMaxPlayers(String arenaName, int maxPlayers) {
        Arena arena = getArena(arenaName);
        if (arena == null) return false;
        arena.setMaxPlayers(maxPlayers);
        saveArenas();
        return true;
    }

    /** Pass an empty list to reset the arena back to "use every globally configured mob type". */
    public boolean setArenaMobTypes(String arenaName, List<String> mobTypes) {
        Arena arena = getArena(arenaName);
        if (arena == null) return false;
        arena.setMobTypes(mobTypes);
        saveArenas();
        return true;
    }

    /** Pass -1 for either value to reset it back to the global config default. */
    public boolean setArenaMobCounts(String arenaName, int baseMobs, int mobIncreasePerWave) {
        Arena arena = getArena(arenaName);
        if (arena == null) return false;
        arena.setBaseMobs(baseMobs);
        arena.setMobIncreasePerWave(mobIncreasePerWave);
        saveArenas();
        return true;
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    private Location stringToLoc(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(":");
        org.bukkit.World world = plugin.getServer().getWorld(parts[0]);
        if (world == null) return null;
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
}