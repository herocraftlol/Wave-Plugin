package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.gui.ArenaSelectGUI;
import com.zombiewaves.utils.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Handles clicks in the arena-select GUI:
 * - Click on an available arena -> join its lobby
 * - Click on the arena that currently has a game running -> spectate it
 * - Click on the "random arena" button -> join the best available arena
 * - Click on the navigation arrows -> change page
 */
public class ArenaSelectGUIListener implements Listener {

    private final ZombieWaves plugin;
    private final ArenaSelectGUI arenaGUI;

    public ArenaSelectGUIListener(ZombieWaves plugin, ArenaSelectGUI arenaGUI) {
        this.plugin = plugin;
        this.arenaGUI = arenaGUI;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!ArenaSelectGUI.isArenaGuiTitle(title)) return;

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getCurrentItem() == null) return;

        int slot = event.getRawSlot();
        int page = ArenaSelectGUI.parsePageFromTitle(title);

        if (ArenaSelectGUI.isPrevPageButton(slot)) {
            arenaGUI.open(player, page - 1);
            return;
        }
        if (ArenaSelectGUI.isNextPageButton(slot)) {
            arenaGUI.open(player, page + 1);
            return;
        }

        if (ArenaSelectGUI.isRandomButton(slot)) {
            player.closeInventory();
            Arena best = arenaGUI.findBestArenaForRandomJoin();
            if (best == null) {
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§cNo arena available for now.");
                return;
            }
            plugin.getLobbyManager().joinArena(player, best.getName());
            return;
        }

        String arenaName = arenaGUI.getArenaNameAt(page, slot);
        if (arenaName == null) return; // Empty/filler slot

        if (plugin.getArenaManager().getArena(arenaName) == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis arena no longer exists.");
            player.closeInventory();
            return;
        }

        player.closeInventory();

        if (arenaGUI.isSpectatable(arenaName)) {
            plugin.getSpectatorManager().addSpectator(player, arenaName);
            return;
        }

        if (arenaGUI.isJoinable(arenaName)) {
            plugin.getLobbyManager().joinArena(player, arenaName);
            return;
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() + "§cThis arena is not available right now.");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (ArenaSelectGUI.isArenaGuiTitle(event.getView().getTitle())) {
            event.setCancelled(true);
        }
    }
}
