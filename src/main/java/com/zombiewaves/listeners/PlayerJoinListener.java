package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final ZombieWaves plugin;

    public PlayerJoinListener(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Only relevant if this player was already tracked as an active in-game player
        // (e.g. a very fast reconnect before PlayerQuitListener's cleanup ran). A normal
        // new/returning player gets nothing here - they must /wave join like anyone else.
        if (plugin.getGameManager().isActivePlayer(player)) {
            plugin.getScoreboardManager().onPlayerJoin(player);
        }
    }
}