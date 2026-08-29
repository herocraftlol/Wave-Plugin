package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.ConfigManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.UUID;

public class EntityDeathListener implements Listener {

    private final ZombieWaves plugin;

    public EntityDeathListener(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        
        // Check if the mob is tracked by our wave manager (this alone identifies a
        // ZombieWaves mob, regardless of its EntityType - no more hardcoded allowlist).
        UUID mobId = entity.getUniqueId();
        String mobTypeKey = plugin.getWaveManager().getMobTypeKey(mobId);
        if (mobTypeKey == null) {
            // Not a tracked wave mob
            return;
        }
        
        // Get the killer
        Player killer = (entity instanceof org.bukkit.entity.LivingEntity living) ? living.getKiller() : null;
        
        if (killer != null) {
            // Add kill to player stats
            plugin.getGameManager().addKill(killer);
            
            // Calculate and add gold, using the exact mob-types config entry this mob was
            // spawned as (not a guess reconstructed from its EntityType).
            ConfigManager.MobTypeConfig mobType = plugin.getConfigManager().getMobTypeConfig(mobTypeKey);
            int goldReward = mobType.getGoldPerKill();
            
            // Apply wave bonus (extra gold based on wave number)
            int wave = plugin.getGameManager().getCurrentWave();
            goldReward += (wave * 1); // +1 gold per wave level
            
            plugin.getGameManager().addGold(killer, goldReward);
            
            // Drop gold item
            event.getDrops().clear(); // Clear default drops
            
            // You can add custom loot tables here if needed
        }
        
        // Notify wave manager that mob was killed
        plugin.getWaveManager().onMobKilled(mobId);
    }
}