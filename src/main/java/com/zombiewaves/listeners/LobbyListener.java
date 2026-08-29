package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class LobbyListener implements Listener {

    private final ZombieWaves plugin;

    public LobbyListener(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getLobbyManager().isInArena(player)) {
            return;
        }

        ItemStack item = event.getItem();

        // Barrier: leave the lobby (available to everyone)
        if (plugin.getLobbyManager().isLeaveItem(item)) {
            event.setCancelled(true);
            plugin.getLobbyManager().leaveArena(player);
            return;
        }

        // Diamond: force start immediately (admins only)
        if (plugin.getLobbyManager().isForceStartItem(item)) {
            event.setCancelled(true);

            if (!player.hasPermission("zombiewaves.admin")) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cYou don't have permission to force start!");
                return;
            }

            String arenaName = plugin.getLobbyManager().getPlayerArenaName(player);
            if (arenaName != null) {
                plugin.getLobbyManager().stopArenaCountdownForAdmin(arenaName);
                
                Bukkit.broadcastMessage(plugin.getConfigManager().getPrefix() + 
                    "§6§l" + player.getName() + " §eforced the game to start!");
                
                plugin.getLobbyManager().startGameNow(arenaName);
            }
        }
    }

    /** Prevents dropping the lobby's leave/force-start items by accident. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getLobbyManager().isInArena(player)) return;

        ItemStack item = event.getItemDrop().getItemStack();
        if (plugin.getLobbyManager().isLeaveItem(item) || plugin.getLobbyManager().isForceStartItem(item)) {
            event.setCancelled(true);
        }
    }
}
