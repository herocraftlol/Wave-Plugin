package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Handles spectator mode for ZombieWaves arenas: a player can watch an ongoing
 * (or waiting) arena without interfering with it, then leave the same way they
 * came in. Since ZombieWaves only ever runs ONE wave game at a time (unlike
 * per-arena game modes), spectating simply teleports the player near the arena
 * and puts them in vanilla spectator gamemode - no special "ghost" logic needed.
 */
public class SpectatorManager implements Listener {

    private final ZombieWaves plugin;
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Location> preSpectateLocations = new HashMap<>();
    private final Map<UUID, String> spectatingArena = new HashMap<>();
    private final NamespacedKey leaveItemKey;

    public SpectatorManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.leaveItemKey = new NamespacedKey(plugin, "spectator_leave_item");
    }

    // ── État ───────────────────────────────────────────────────────────────

    public boolean isSpectating(Player player) {
        return spectators.contains(player.getUniqueId());
    }

    public int getSpectatorCount() {
        return spectators.size();
    }

    public int getSpectatorCount(String arenaName) {
        int count = 0;
        for (String arena : spectatingArena.values()) {
            if (arena.equalsIgnoreCase(arenaName)) count++;
        }
        return count;
    }

    /**
     * Meilleur point disponible pour observer une arène : sa zone de jeu si elle
     * est définie, sinon son lobby. Peut renvoyer null si l'arène n'a encore
     * aucun des deux de configuré.
     */
    public Location resolveSpectateLocation(Arena arena) {
        if (arena.getGameSpawnLocation() != null) {
            return arena.getGameSpawnLocation().clone();
        }
        if (arena.getLobbyLocation() != null) {
            return arena.getLobbyLocation().clone();
        }
        return null;
    }

    // ── Entrée / sortie du mode spectateur ────────────────────────────────

    public boolean addSpectator(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§cArena '" + arenaName + "' does not exist!");
            return false;
        }
        if (plugin.getLobbyManager().isInArena(player)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("cannot-spectate-own-game"));
            return false;
        }
        if (isSpectating(player)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("already-spectating"));
            return false;
        }

        Location teleportTo = resolveSpectateLocation(arena);
        if (teleportTo == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("no-spectate-location"));
            return false;
        }

        preSpectateLocations.put(player.getUniqueId(), player.getLocation().clone());
        spectators.add(player.getUniqueId());
        spectatingArena.put(player.getUniqueId(), arena.getName());

        player.teleport(teleportTo);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().clear();
        player.getInventory().setItem(8, createLeaveItem());

        plugin.getScoreboardManager().showGameScoreboard(player);

        player.sendMessage(plugin.getConfigManager().getPrefix() +
            plugin.getConfigManager().getMessage("now-spectating").replace("{arena}", arena.getName()));
        return true;
    }

    public void removeSpectator(Player player) {
        UUID id = player.getUniqueId();
        if (!spectators.remove(id)) return;

        spectatingArena.remove(id);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        plugin.getScoreboardManager().removeScoreboard(player);

        Location back = preSpectateLocations.remove(id);
        if (back != null) {
            player.teleport(back);
        } else {
            Location exit = plugin.getArenaManager().getGlobalExitLocation();
            player.teleport(exit != null ? exit : Bukkit.getWorlds().get(0).getSpawnLocation());
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() +
            plugin.getConfigManager().getMessage("stopped-spectating"));
    }

    /** Fait sortir tous les spectateurs (arrêt du plugin, arène supprimée, etc.). */
    public void removeAllSpectators() {
        for (UUID id : new ArrayList<>(spectators)) {
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                removeSpectator(player);
            } else {
                spectators.remove(id);
                spectatingArena.remove(id);
                preSpectateLocations.remove(id);
            }
        }
    }

    /** Nettoyage silencieux (pas de téléportation/message) quand le joueur se déconnecte. */
    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();
        spectators.remove(id);
        spectatingArena.remove(id);
        preSpectateLocations.remove(id);
    }

    // ── Item "quitter le mode spectateur" ─────────────────────────────────

    private ItemStack createLeaveItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lLeave Spectator Mode");
            List<String> lore = new ArrayList<>();
            lore.add("§7Right-click to stop watching");
            lore.add("§7the game (/wave unspectate)");
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(leaveItemKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isLeaveItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte tag = item.getItemMeta().getPersistentDataContainer().get(leaveItemKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }

    // ── Listeners ──────────────────────────────────────────────────────────

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEvent event) {
        if (!isSpectating(event.getPlayer())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!isLeaveItem(event.getItem())) return;

        event.setCancelled(true);
        removeSpectator(event.getPlayer());
    }

    /**
     * Garde les spectateurs à proximité raisonnable du point qu'ils observent,
     * pour éviter qu'ils ne s'envolent (mode spectateur = vol libre) à travers
     * toute la carte. Utilise "spectator.max-distance" du config.yml.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!isSpectating(player)) return;
        if (event.getTo() == null) return;

        String arenaName = spectatingArena.get(player.getUniqueId());
        if (arenaName == null) return;
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;

        Location center = resolveSpectateLocation(arena);
        if (center == null || center.getWorld() == null) return;
        Location to = event.getTo();
        if (to.getWorld() == null || !to.getWorld().equals(center.getWorld())) return;

        double maxDistance = plugin.getConfigManager().getSpectatorMaxDistance();
        if (center.distanceSquared(to) > maxDistance * maxDistance) {
            player.teleport(center);
        }
    }
}
