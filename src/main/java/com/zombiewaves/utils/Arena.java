package com.zombiewaves.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

@SerializableAs("Arena")
public class Arena implements ConfigurationSerializable {

    private final String name;
    private Location pos1;
    private Location pos2;
    private final List<Location> spawnPoints;
    private Location lobbyLocation;
    private Location gameSpawnLocation;
    private boolean active;

    // Per-arena overrides (0/-1/empty = "use the global config default", set via /wave commands)
    private int minPlayers = 0;
    private int maxPlayers = 0;
    private int baseMobs = -1;
    private int mobIncreasePerWave = -1;
    private final List<String> mobTypes = new ArrayList<>();

    public Arena(String name) {
        this.name = name;
        this.spawnPoints = new ArrayList<>();
        this.active = false;
    }

    public Arena(String name, Location pos1, Location pos2, List<Location> spawnPoints) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.spawnPoints = spawnPoints != null ? spawnPoints : new ArrayList<>();
        this.active = false;
    }

    public String getName() {
        return name;
    }

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public List<Location> getSpawnPoints() {
        return spawnPoints;
    }

    public void addSpawnPoint(Location location) {
        if (location != null && !spawnPoints.contains(location)) {
            spawnPoints.add(location);
        }
    }

    public void removeSpawnPoint(Location location) {
        spawnPoints.remove(location);
    }

    public void clearSpawnPoints() {
        spawnPoints.clear();
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public void setLobbyLocation(Location lobbyLocation) {
        this.lobbyLocation = lobbyLocation;
    }

    public Location getGameSpawnLocation() {
        return gameSpawnLocation;
    }

    public void setGameSpawnLocation(Location gameSpawnLocation) {
        this.gameSpawnLocation = gameSpawnLocation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // ── Per-arena wave/player overrides (0/-1/empty = use global config default) ──

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = Math.max(0, minPlayers);
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(0, maxPlayers);
    }

    public int getBaseMobs() {
        return baseMobs;
    }

    public void setBaseMobs(int baseMobs) {
        this.baseMobs = baseMobs;
    }

    public int getMobIncreasePerWave() {
        return mobIncreasePerWave;
    }

    public void setMobIncreasePerWave(int mobIncreasePerWave) {
        this.mobIncreasePerWave = mobIncreasePerWave;
    }

    /** Empty list means "use every mob type defined in config.yml's mob-types section". */
    public List<String> getMobTypes() {
        return mobTypes;
    }

    public void setMobTypes(List<String> types) {
        mobTypes.clear();
        if (types != null) {
            for (String t : types) {
                if (t != null && !t.isBlank()) mobTypes.add(t.toLowerCase());
            }
        }
    }

    public boolean isComplete() {
        return pos1 != null && pos2 != null && !spawnPoints.isEmpty() && lobbyLocation != null;
    }

    public World getWorld() {
        if (pos1 != null) return pos1.getWorld();
        if (pos2 != null) return pos2.getWorld();
        if (!spawnPoints.isEmpty()) return spawnPoints.get(0).getWorld();
        return null;
    }

    public Location getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) return null;
        return spawnPoints.get(new Random().nextInt(spawnPoints.size()));
    }

    public Location getRandomLocationInArena() {
        if (!isComplete()) return null;

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        World world = pos1.getWorld();
        
        // Find a valid spawn location (not in water/lava, on solid ground)
        for (int attempts = 0; attempts < 20; attempts++) {
            int x = minX + new Random().nextInt(maxX - minX + 1);
            int z = minZ + new Random().nextInt(maxZ - minZ + 1);
            int y = findSafeY(world, minY, maxY, x, z);
            
            if (y != -1) {
                return new Location(world, x + 0.5, y, z + 0.5);
            }
        }
        
        // Fallback: return a random spawn point
        return getRandomSpawnPoint();
    }

    private int findSafeY(World world, int minY, int maxY, int x, int z) {
        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            
            if (isSafeBlock(block) && isPassableBlock(above)) {
                return y + 1;
            }
        }
        return -1;
    }

    private boolean isSafeBlock(Block block) {
        return !block.getType().name().contains("WATER") &&
               !block.getType().name().contains("LAVA") &&
               !block.getType().isSolid();
    }

    private boolean isPassableBlock(Block block) {
        return !block.getType().isSolid() &&
               !block.getType().name().contains("WATER") &&
               !block.getType().name().contains("LAVA");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("pos1", pos1 != null ? locToString(pos1) : null);
        map.put("pos2", pos2 != null ? locToString(pos2) : null);
        map.put("spawnPoints", spawnPoints.stream().map(this::locToString).toList());
        map.put("lobbyLocation", lobbyLocation != null ? locToString(lobbyLocation) : null);
        map.put("gameSpawnLocation", gameSpawnLocation != null ? locToString(gameSpawnLocation) : null);
        map.put("active", active);
        map.put("minPlayers", minPlayers);
        map.put("maxPlayers", maxPlayers);
        map.put("baseMobs", baseMobs);
        map.put("mobIncreasePerWave", mobIncreasePerWave);
        map.put("mobTypes", mobTypes);
        return map;
    }

    public static Arena deserialize(Map<String, Object> map) {
        String name = (String) map.get("name");
        Arena arena = new Arena(name);
        
        if (map.containsKey("pos1") && map.get("pos1") != null) {
            arena.setPos1(stringToLoc((String) map.get("pos1")));
        }
        if (map.containsKey("pos2") && map.get("pos2") != null) {
            arena.setPos2(stringToLoc((String) map.get("pos2")));
        }
        if (map.containsKey("spawnPoints")) {
            @SuppressWarnings("unchecked")
            List<String> spawnList = (List<String>) map.get("spawnPoints");
            for (String locStr : spawnList) {
                arena.addSpawnPoint(stringToLoc(locStr));
            }
        }
        if (map.containsKey("lobbyLocation") && map.get("lobbyLocation") != null) {
            arena.setLobbyLocation(stringToLoc((String) map.get("lobbyLocation")));
        }
        if (map.containsKey("gameSpawnLocation") && map.get("gameSpawnLocation") != null) {
            arena.setGameSpawnLocation(stringToLoc((String) map.get("gameSpawnLocation")));
        }
        if (map.containsKey("active")) {
            arena.setActive((Boolean) map.get("active"));
        }
        if (map.containsKey("minPlayers")) {
            arena.setMinPlayers(((Number) map.get("minPlayers")).intValue());
        }
        if (map.containsKey("maxPlayers")) {
            arena.setMaxPlayers(((Number) map.get("maxPlayers")).intValue());
        }
        if (map.containsKey("baseMobs")) {
            arena.setBaseMobs(((Number) map.get("baseMobs")).intValue());
        }
        if (map.containsKey("mobIncreasePerWave")) {
            arena.setMobIncreasePerWave(((Number) map.get("mobIncreasePerWave")).intValue());
        }
        if (map.containsKey("mobTypes")) {
            @SuppressWarnings("unchecked")
            List<String> types = (List<String>) map.get("mobTypes");
            arena.setMobTypes(types);
        }
        
        return arena;
    }

    private String locToString(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    private static Location stringToLoc(String str) {
        String[] parts = str.split(":");
        World world = BukkitUtil.getWorld(parts[0]);
        double x = Double.parseDouble(parts[1]);
        double y = Double.parseDouble(parts[2]);
        double z = Double.parseDouble(parts[3]);
        float yaw = Float.parseFloat(parts[4]);
        float pitch = Float.parseFloat(parts[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }
}