package com.zombiewaves.utils;

import com.zombiewaves.ZombieWaves;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final ZombieWaves plugin;
    private FileConfiguration config;

    public ConfigManager(ZombieWaves plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        load();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    // General settings
    public int getTotalWaves() {
        return config.getInt("general.total-waves", 10);
    }

    public int getWaveDelay() {
        return config.getInt("general.wave-delay", 30);
    }

    public int getMinPlayers() {
        return config.getInt("general.min-players", 1);
    }

    public int getGracePeriod() {
        return config.getInt("general.grace-period", 60);
    }

    public int getDefaultMaxPlayers() {
        return config.getInt("general.default-max-players", 20);
    }

    // Spectator settings
    public double getSpectatorMaxDistance() {
        return config.getDouble("spectator.max-distance", 60);
    }

    // Wave settings
    public int getBaseMobs() {
        return config.getInt("waves.base-mobs", 5);
    }

    public int getMobIncreasePerWave() {
        return config.getInt("waves.mob-increase-per-wave", 3);
    }

    public int getSpawnDelay() {
        return config.getInt("waves.spawn-delay", 10);
    }

    public int getMaxActiveMobs() {
        return config.getInt("waves.max-active-mobs", 15);
    }

    public double getPlayerScalingMultiplier() {
        return config.getDouble("waves.player-scaling-multiplier", 0.5);
    }

    public int getMinPlayersForScaling() {
        return config.getInt("waves.min-players-for-scaling", 1);
    }

    /**
     * Calculate mob count for a wave based on wave number and player count.
     * Formula: baseMobs + (wave * mobIncreasePerWave) * (1 + (playerScalingMultiplier * effectivePlayers))
     */
    public int getMobCountForWave(int wave, int playerCount) {
        int effectivePlayers = Math.max(playerCount, getMinPlayersForScaling());
        double baseCount = getBaseMobs() + (wave * getMobIncreasePerWave());
        double scalingFactor = 1.0 + (getPlayerScalingMultiplier() * effectivePlayers);
        return (int) Math.ceil(baseCount * scalingFactor);
    }

    /**
     * Calculate mob count for a wave (legacy, uses player count of 1)
     */
    public int getMobCountForWave(int wave) {
        return getMobCountForWave(wave, 1);
    }

    /**
     * Same formula as {@link #getMobCountForWave(int, int)}, but lets an arena override
     * base-mobs and mob-increase-per-wave (pass -1 for either to use the global default).
     */
    public int getMobCountForWave(int wave, int playerCount, int baseMobsOverride, int increaseOverride) {
        int base = baseMobsOverride >= 0 ? baseMobsOverride : getBaseMobs();
        int increase = increaseOverride >= 0 ? increaseOverride : getMobIncreasePerWave();
        int effectivePlayers = Math.max(playerCount, getMinPlayersForScaling());
        double baseCount = base + (wave * increase);
        double scalingFactor = 1.0 + (getPlayerScalingMultiplier() * effectivePlayers);
        return (int) Math.ceil(baseCount * scalingFactor);
    }

    // Difficulty settings
    public double getHealthMultiplier() {
        return config.getDouble("difficulty.health-multiplier", 0.15);
    }

    public double getDamageMultiplier() {
        return config.getDouble("difficulty.damage-multiplier", 0.10);
    }

    public double getSpeedMultiplier() {
        return config.getDouble("difficulty.speed-multiplier", 0.05);
    }

    // Get spawn points for a map
    public List<Location> getSpawnPoints(String mapName) {
        List<Location> locations = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("spawn-points." + mapName);
        
        if (section == null) {
            // Return default spawn points if map not found
            section = config.getConfigurationSection("spawn-points.default");
        }
        
        if (section != null) {
            for (String key : section.getKeys(false)) {
                String worldName = section.getString(key + ".world", "world");
                double x = section.getDouble(key + ".x", 0);
                double y = section.getDouble(key + ".y", 64);
                double z = section.getDouble(key + ".z", 0);
                
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    locations.add(new Location(world, x, y, z));
                }
            }
        }
        
        // If still empty, return default location
        if (locations.isEmpty()) {
            World defaultWorld = plugin.getServer().getWorlds().get(0);
            locations.add(new Location(defaultWorld, 0, 64, 0));
        }
        
        return locations;
    }

    // Get all spawn point map names
    public List<String> getSpawnPointMaps() {
        ConfigurationSection section = config.getConfigurationSection("spawn-points");
        if (section == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(section.getKeys(false));
    }

    // Mob type configuration
    public MobTypeConfig getMobTypeConfig(String type) {
        ConfigurationSection section = config.getConfigurationSection("mob-types." + type);
        if (section == null) {
            return new MobTypeConfig(type, "ZOMBIE", 20.0, 1.0, 5, 60);
        }
        
        return new MobTypeConfig(
            type,
            section.getString("entity-type", "ZOMBIE"),
            section.getDouble("base-health", 20.0),
            section.getDouble("damage-multiplier", 1.0),
            section.getInt("gold-per-kill", 5),
            section.getInt("spawn-weight", 60)
        );
    }

    // Get all mob type configs
    public List<MobTypeConfig> getAllMobTypes() {
        List<MobTypeConfig> types = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("mob-types");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                types.add(getMobTypeConfig(key));
            }
        }
        return types;
    }

    // Get random spawn weight for mob selection
    public MobTypeConfig getRandomMobType() {
        return getRandomMobType(null);
    }

    /**
     * Picks a random mob type, restricted to {@code allowedKeys} if non-empty (falls back
     * to every configured mob type otherwise - e.g. an arena with no mob-types override).
     *
     * The pick is weighted by each type's configured "spawn-weight" divided by its "power"
     * (baseHealth x damageMultiplier): for an equal spawn-weight, a tougher mob is naturally
     * picked less often than a weaker one, so a wave stays balanced between "how many of each
     * type the admin wants" and "how dangerous each type actually is".
     */
    public MobTypeConfig getRandomMobType(List<String> allowedKeys) {
        List<MobTypeConfig> pool = getMobTypePool(allowedKeys);
        if (pool.isEmpty()) pool = getAllMobTypes();
        if (pool.isEmpty()) return new MobTypeConfig("zombie", "ZOMBIE", 20.0, 1.0, 5, 60);

        double totalWeight = 0;
        double[] effectiveWeights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            effectiveWeights[i] = effectiveWeight(pool.get(i));
            totalWeight += effectiveWeights[i];
        }

        double roll = Math.random() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += effectiveWeights[i];
            if (roll < cumulative) {
                return pool.get(i);
            }
        }
        return pool.get(pool.size() - 1);
    }

    private List<MobTypeConfig> getMobTypePool(List<String> allowedKeys) {
        if (allowedKeys == null || allowedKeys.isEmpty()) {
            return getAllMobTypes();
        }
        List<MobTypeConfig> pool = new ArrayList<>();
        for (String key : allowedKeys) {
            ConfigurationSection section = config.getConfigurationSection("mob-types." + key);
            if (section != null) {
                pool.add(getMobTypeConfig(key));
            }
        }
        return pool;
    }

    /** "Power" proxy used to balance spawn odds: raw threat = health x damage. */
    private double mobPower(MobTypeConfig type) {
        return Math.max(1.0, type.getBaseHealth() * type.getDamageMultiplier());
    }

    private double effectiveWeight(MobTypeConfig type) {
        return Math.max(0.1, type.getSpawnWeight() / mobPower(type));
    }

    /**
     * Weighted-average gold-per-kill across {@code allowedKeys} (or every mob type if empty),
     * using the same effective (spawn-weight / power) weighting as the actual spawn selection
     * so this reflects the gold a player can realistically expect to earn per kill on average.
     * Used to derive balanced shop prices - see ShopManager.
     */
    public double getAverageGoldPerKill(List<String> allowedKeys) {
        List<MobTypeConfig> pool = getMobTypePool(allowedKeys);
        if (pool.isEmpty()) pool = getAllMobTypes();
        if (pool.isEmpty()) return 5.0;

        double totalWeight = 0;
        double weightedGold = 0;
        for (MobTypeConfig type : pool) {
            double w = effectiveWeight(type);
            totalWeight += w;
            weightedGold += w * type.getGoldPerKill();
        }
        return totalWeight > 0 ? weightedGold / totalWeight : 5.0;
    }

    // Messages
    public String getMessage(String path) {
        return colorize(config.getString("messages." + path, "&cMessage not found: " + path));
    }

    public String getPrefix() {
        return colorize(config.getString("messages.prefix", "&6&l[ZombieWaves] &r"));
    }

    // Utility
    public String colorize(String text) {
        if (text == null) return "";
        return text.replace("&", "§");
    }

    // Inner class for mob type configuration
    public static class MobTypeConfig {
        private final String name;
        private final String entityType;
        private final double baseHealth;
        private final double damageMultiplier;
        private final int goldPerKill;
        private final int spawnWeight;

        public MobTypeConfig(String name, String entityType, double baseHealth, 
                           double damageMultiplier, int goldPerKill, int spawnWeight) {
            this.name = name;
            this.entityType = entityType;
            this.baseHealth = baseHealth;
            this.damageMultiplier = damageMultiplier;
            this.goldPerKill = goldPerKill;
            this.spawnWeight = spawnWeight;
        }

        public String getName() { return name; }
        public String getEntityType() { return entityType; }
        public double getBaseHealth() { return baseHealth; }
        public double getDamageMultiplier() { return damageMultiplier; }
        public int getGoldPerKill() { return goldPerKill; }
        public int getSpawnWeight() { return spawnWeight; }
    }
}