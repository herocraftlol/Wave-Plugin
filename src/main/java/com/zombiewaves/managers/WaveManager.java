package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Husk;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class WaveManager {

    private final ZombieWaves plugin;
    private final Set<UUID> activeMobs;
    private final Map<UUID, String> activeMobTypeKeys;
    private final List<BukkitTask> activeTasks;
    private int mobsToSpawn;
    private int mobsSpawned;
    private int currentWaveNumber;
    private boolean waveInProgress;
    private BukkitTask spawnTask;

    public WaveManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.activeMobs = new HashSet<>();
        this.activeMobTypeKeys = new HashMap<>();
        this.activeTasks = new ArrayList<>();
    }

    public void startWave(int waveNumber) {
        if (waveInProgress) return;
        
        currentWaveNumber = waveNumber;
        waveInProgress = true;
        
        // Get player count from arena players
        int playerCount = getPlayerCountInArena();
        
        // Calculate total mobs for this wave, honoring this arena's base-mobs/increase
        // overrides if configured (falls back to the global config.yml defaults otherwise).
        com.zombiewaves.utils.Arena arena = getSelectedArena();
        int baseOverride = arena != null ? arena.getBaseMobs() : -1;
        int increaseOverride = arena != null ? arena.getMobIncreasePerWave() : -1;
        mobsToSpawn = plugin.getConfigManager().getMobCountForWave(waveNumber, playerCount, baseOverride, increaseOverride);
        mobsSpawned = 0;
        
        // Broadcast wave start with player info
        String waveMsg = plugin.getConfigManager().getMessage("wave-start")
            .replace("{wave}", String.valueOf(waveNumber));
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + waveMsg);
        
        // Announce mob count
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            "§e" + mobsToSpawn + " mobs incoming! (§7" + playerCount + " players§e)");
        
        // Start spawning mobs with delay
        startSpawning();
    }

    private int getPlayerCountInArena() {
        String arenaName = plugin.getGameManager().getSelectedArena();
        if (arenaName == null) return 1;
        return Math.max(1, plugin.getArenaManager().getPlayerCountInArena(arenaName));
    }

    private void startSpawning() {
        int spawnDelay = plugin.getConfigManager().getSpawnDelay();
        int maxActive = plugin.getConfigManager().getMaxActiveMobs();
        
        spawnTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!waveInProgress || !plugin.getGameManager().isGameRunning()) {
                    cancel();
                    return;
                }
                
                // Check if we still have mobs to spawn
                if (mobsSpawned >= mobsToSpawn) {
                    cancel();
                    return;
                }
                
                // Check if we can spawn more (max active limit)
                if (activeMobs.size() >= maxActive) {
                    return;
                }
                
                // Spawn a mob
                spawnMob();
                mobsSpawned++;
                
                // Check if wave is complete
                if (mobsSpawned >= mobsToSpawn && activeMobs.isEmpty()) {
                    // All mobs spawned and killed, end wave
                    waveInProgress = false;
                    onWaveComplete();
                }
            }
        }.runTaskTimer(plugin, 20L, spawnDelay);
        
        activeTasks.add(spawnTask);
    }

    private com.zombiewaves.utils.Arena getSelectedArena() {
        String arenaName = plugin.getGameManager().getSelectedArena();
        if (arenaName == null) return null;
        return plugin.getArenaManager().getArena(arenaName);
    }

    private void spawnMob() {
        // Get spawn points from arena
        List<Location> spawnPoints = getArenaSpawnPoints();
        if (spawnPoints.isEmpty()) {
            plugin.getLogger().warning("No spawn points in selected arena!");
            return;
        }
        
        Location spawnLoc = spawnPoints.get(new Random().nextInt(spawnPoints.size()));
        
        // Pick a mob type, restricted to this arena's configured roster if it has one
        // (falls back to every mob type in config.yml otherwise). The pick is weighted
        // both by the type's configured spawn-weight AND its power (health x damage), so
        // mobs are spread out fairly instead of just uniformly at random.
        com.zombiewaves.utils.Arena arena = getSelectedArena();
        List<String> allowedTypes = arena != null ? arena.getMobTypes() : null;
        ConfigManager.MobTypeConfig mobType = plugin.getConfigManager().getRandomMobType(allowedTypes);
        
        // Spawn the entity
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(mobType.getEntityType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid entity type: " + mobType.getEntityType());
            entityType = EntityType.ZOMBIE;
        }
        
        Entity entity = spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
        
        if (entity instanceof LivingEntity livingEntity) {
            // Apply wave difficulty scaling
            applyDifficultyScaling(livingEntity, mobType);
            
            // Track the mob and which config key it was spawned as (used for the gold
            // reward on death - see EntityDeathListener - instead of guessing it back
            // from the entity type, which breaks as soon as two mob-types share a type).
            activeMobs.add(entity.getUniqueId());
            activeMobTypeKeys.put(entity.getUniqueId(), mobType.getName());
        }
    }

    private List<Location> getArenaSpawnPoints() {
        com.zombiewaves.utils.Arena arena = getSelectedArena();
        if (arena == null) {
            return new ArrayList<>();
        }
        
        return arena.getSpawnPoints();
    }

    private void applyDifficultyScaling(LivingEntity entity, ConfigManager.MobTypeConfig mobType) {
        int wave = currentWaveNumber;
        
        // Calculate scaled health
        double healthMultiplier = 1.0 + (wave * plugin.getConfigManager().getHealthMultiplier());
        double scaledHealth = mobType.getBaseHealth() * healthMultiplier;
        
        // Set max health
        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(scaledHealth);
        }
        entity.setHealth(Math.min(scaledHealth, entity.getHealth()));
        
        // Set custom name to show health
        entity.setCustomName("§c" + entity.getType().name() + " §7[§eWave " + wave + "§7] §c" + (int) scaledHealth + " HP");
        entity.setCustomNameVisible(true);
        
        // Apply movement speed scaling (capped at 2.0x)
        double speedMultiplier = Math.min(1.0 + (wave * plugin.getConfigManager().getSpeedMultiplier()), 2.0);
        
        // Note: Speed modification requires NMS or attributes
        // For simplicity, we'll rely on the default speed
    }

    public void onMobKilled(UUID mobId) {
        activeMobs.remove(mobId);
        activeMobTypeKeys.remove(mobId);
        
        // Check if wave is complete
        if (mobsSpawned >= mobsToSpawn && activeMobs.isEmpty() && waveInProgress) {
            waveInProgress = false;
            onWaveComplete();
        }
    }

    /** Config key ("zombie", "skeleton", ...) this mob was spawned as, or null if untracked. */
    public String getMobTypeKey(UUID mobId) {
        return activeMobTypeKeys.get(mobId);
    }

    private void onWaveComplete() {
        // Broadcast wave end
        Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("wave-end")
                .replace("{wave}", String.valueOf(currentWaveNumber)));
        
        // Start countdown to next wave
        int waveDelay = plugin.getConfigManager().getWaveDelay();
        plugin.getGameManager().startCountdown(waveDelay);
    }

    public void clearAllMobs() {
        // Cancel all tasks
        for (BukkitTask task : activeTasks) {
            task.cancel();
        }
        activeTasks.clear();
        
        if (spawnTask != null) {
            spawnTask.cancel();
            spawnTask = null;
        }
        
        // Remove all tracked mobs
        for (UUID mobId : activeMobs) {
            Entity entity = Bukkit.getEntity(mobId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        activeMobs.clear();
        activeMobTypeKeys.clear();
        
        waveInProgress = false;
    }

    public int getRemainingMobs() {
        return activeMobs.size() + (mobsToSpawn - mobsSpawned);
    }

    public int getActiveMobCount() {
        return activeMobs.size();
    }

    public boolean isWaveInProgress() {
        return waveInProgress;
    }

    public int getCurrentWaveNumber() {
        return currentWaveNumber;
    }

    public Set<UUID> getActiveMobIds() {
        return activeMobs;
    }
}