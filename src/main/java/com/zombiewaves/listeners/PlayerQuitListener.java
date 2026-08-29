package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Cleans up per-player state on disconnect. Before this listener existed,
 * ZombieWaves had no PlayerQuitEvent handling at all: a player who quit while
 * in a lobby stayed "in arena" forever (blocking them from rejoining once back,
 * and inflating player counts shown in chat/GUI), and their scoreboard entry
 * was never removed from ScoreboardManager either.
 */
public class PlayerQuitListener implements Listener {

    private final ZombieWaves plugin;

    public PlayerQuitListener(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        plugin.getLobbyManager().handleQuit(player);
        plugin.getSpectatorManager().handleQuit(player);
        plugin.getScoreboardManager().onPlayerQuit(player);
    }
}
