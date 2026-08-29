package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LobbyManager {

    private final ZombieWaves plugin;
    private final Set<UUID> playersInLobby;
    private final Map<UUID, Location> playerPreviousLocations;
    private final Map<String, BukkitRunnable> arenaCountdowns;

    // Lobby item slots (5th slot = index 4, matches HikaBrain's convention)
    public static final int LEAVE_SLOT = 4;
    public static final int FORCE_START_SLOT = 0;

    private final NamespacedKey leaveItemKey;
    private final NamespacedKey forceStartItemKey;
    
    private static final int LOBBY_COUNTDOWN = 10; // seconds before game starts

    public LobbyManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.playersInLobby = new HashSet<>();
        this.playerPreviousLocations = new HashMap<>();
        this.arenaCountdowns = new HashMap<>();
        this.leaveItemKey = new NamespacedKey(plugin, "lobby_leave_item");
        this.forceStartItemKey = new NamespacedKey(plugin, "lobby_force_start_item");
    }

    public boolean joinArena(Player player, String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        
        if (arena == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return false;
        }
        
        if (!arena.isComplete()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena is not complete! Missing: " + getMissingRequirements(arena));
            return false;
        }
        
        if (plugin.getArenaManager().isArenaFull(arenaName)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena is full! Maximum " + plugin.getArenaManager().getMaxPlayersPerArena(arenaName) + " players.");
            return false;
        }
        
        if (playersInLobby.contains(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cYou are already in an arena!");
            return false;
        }
        
        // Save player's previous location
        playerPreviousLocations.put(player.getUniqueId(), player.getLocation());
        
        // Teleport to lobby
        Location lobbyLoc = arena.getLobbyLocation();
        if (lobbyLoc == null) {
            lobbyLoc = plugin.getArenaManager().getGlobalLobbyLocation();
        }
        
        if (lobbyLoc == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cNo lobby location set! Ask an admin to set one.");
            return false;
        }
        
        // Save inventory and clear it
        // Note: Could implement inventory saving here
        
        player.teleport(lobbyLoc);
        player.setGameMode(GameMode.ADVENTURE);
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aYou joined arena '" + arena.getName() + "'!");
        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§eUse §f/wave leave §e to exit the arena.");
        
        playersInLobby.add(player.getUniqueId());
        plugin.getArenaManager().setPlayerArena(player.getUniqueId(), arenaName);
        
        // Clear inventory and give lobby items
        player.getInventory().clear();
        giveLobbyItems(player);
        
        // Update scoreboard
        plugin.getScoreboardManager().showLobbyScoreboard(player, arenaName);
        
        // Start countdown (1+ players now)
        checkAndStartCountdown(arenaName);
        
        return true;
    }

    private void giveLobbyItems(Player player) {
        // Barrier for everyone: leave the lobby
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (barrierMeta != null) {
            barrierMeta.setDisplayName(plugin.getConfigManager().colorize("§c§lLeave Arena"));
            barrierMeta.setLore(List.of(plugin.getConfigManager().colorize("§7Click to leave (/wave leave)")));
            barrierMeta.getPersistentDataContainer().set(leaveItemKey, PersistentDataType.BYTE, (byte) 1);
            barrier.setItemMeta(barrierMeta);
        }
        player.getInventory().setItem(LEAVE_SLOT, barrier);

        // Diamond for admins: force start immediately
        if (player.hasPermission("zombiewaves.admin")) {
            ItemStack diamond = new ItemStack(Material.DIAMOND);
            ItemMeta meta = diamond.getItemMeta();
            meta.setDisplayName(plugin.getConfigManager().colorize("§b§lFORCE START"));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().colorize("§7Click to start the game"));
            lore.add(plugin.getConfigManager().colorize("§7immediately (admin only)"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(forceStartItemKey, PersistentDataType.BYTE, (byte) 1);
            diamond.setItemMeta(meta);
            player.getInventory().setItem(FORCE_START_SLOT, diamond);
        }
    }

    public boolean isLeaveItem(ItemStack item) {
        if (item == null || item.getType() != Material.BARRIER || !item.hasItemMeta()) return false;
        Byte tag = item.getItemMeta().getPersistentDataContainer().get(leaveItemKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }

    public boolean isForceStartItem(ItemStack item) {
        if (item == null || item.getType() != Material.DIAMOND || !item.hasItemMeta()) return false;
        Byte tag = item.getItemMeta().getPersistentDataContainer().get(forceStartItemKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }

    public boolean leaveArena(Player player) {
        // Case 1: still waiting in the lobby (not yet in-game)
        if (playersInLobby.contains(player.getUniqueId())) {
            String arenaName = plugin.getArenaManager().getPlayerArena(player.getUniqueId());
            
            // Remove from lobby
            playersInLobby.remove(player.getUniqueId());
            plugin.getArenaManager().removePlayerArena(player.getUniqueId());
            
            teleportBackAndReset(player);
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§aYou left the arena.");
            plugin.getScoreboardManager().clearScoreboard(player);

            if (arenaName != null) {
                checkAndStopCountdown(arenaName);
            }
            return true;
        }

        // Case 2: actively playing a running wave game
        if (plugin.getGameManager().isActivePlayer(player)) {
            // Removing them may auto-stop/reset the game if they were the last one -
            // see GameManager#removePlayer().
            plugin.getGameManager().removePlayer(player);
            plugin.getArenaManager().removePlayerArena(player.getUniqueId());

            teleportBackAndReset(player);
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§aYou left the arena.");
            plugin.getScoreboardManager().clearScoreboard(player);
            return true;
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§cYou are not in an arena!");
        return false;
    }

    /** Teleports a leaving player back to where they were before joining, restores survival mode. */
    private void teleportBackAndReset(Player player) {
        Location returnLoc = playerPreviousLocations.remove(player.getUniqueId());
        if (returnLoc == null) {
            returnLoc = plugin.getArenaManager().getGlobalExitLocation();
        }
        if (returnLoc == null) {
            returnLoc = Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        
        player.teleport(returnLoc);
        player.setGameMode(GameMode.SURVIVAL);
    }

    public boolean isInArena(Player player) {
        return playersInLobby.contains(player.getUniqueId());
    }

    public String getPlayerArenaName(Player player) {
        return plugin.getArenaManager().getPlayerArena(player.getUniqueId());
    }

    public int getPlayerCount(String arenaName) {
        return plugin.getArenaManager().getPlayerCountInArena(arenaName);
    }

    public int getMaxPlayers() {
        return plugin.getArenaManager().getMaxPlayersPerArena();
    }

    public int getMaxPlayers(String arenaName) {
        return plugin.getArenaManager().getMaxPlayersPerArena(arenaName);
    }

    public int getMinPlayers(String arenaName) {
        return plugin.getArenaManager().getMinPlayersPerArena(arenaName);
    }

    private void checkAndStartCountdown(String arenaName) {
        int playerCount = getPlayerCount(arenaName);
        int minPlayers = getMinPlayers(arenaName);
        
        if (playerCount >= minPlayers && !arenaCountdowns.containsKey(arenaName)) {
            startArenaCountdown(arenaName);
        }
    }

    private void checkAndStopCountdown(String arenaName) {
        int playerCount = getPlayerCount(arenaName);
        int minPlayers = getMinPlayers(arenaName);
        
        if (playerCount < minPlayers && arenaCountdowns.containsKey(arenaName)) {
            stopArenaCountdown(arenaName);
            broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
                "§cNot enough players! Waiting for more (need " + minPlayers + ")...");
        }
    }

    private void startArenaCountdown(String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;
        
        int playerCount = getPlayerCount(arenaName);
        int minPlayers = getMinPlayers(arenaName);
        
        BukkitRunnable countdown = new BukkitRunnable() {
            int seconds = LOBBY_COUNTDOWN;
            
            @Override
            public void run() {
                if (seconds <= 0) {
                    // Start the game
                    startGame(arenaName);
                    arenaCountdowns.remove(arenaName);
                    cancel();
                    return;
                }
                
                // Check if we still meet this arena's minimum player requirement
                if (getPlayerCount(arenaName) < minPlayers) {
                    stopArenaCountdown(arenaName);
                    broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
                        "§cNot enough players left! Countdown stopped.");
                    cancel();
                    return;
                }
                
                // Broadcast countdown every 5 seconds and at 3, 2, 1
                if (seconds <= 5 || seconds % 5 == 0) {
                    broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
                        "§eGame starting in §f" + seconds + " §eseconds!");
                }
                
                // Update scoreboards
                updateLobbyScoreboards(arenaName, seconds);
                
                seconds--;
            }
        };
        
        arenaCountdowns.put(arenaName, countdown);
        countdown.runTaskTimer(plugin, 20L, 20L);
        
        if (playerCount == 1 && minPlayers == 1) {
            broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
                "§eSolo play enabled! Starting countdown...");
        } else {
            broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
                "§aEnough players (" + playerCount + "/" + minPlayers + " min)! Starting countdown...");
        }
    }

    private void stopArenaCountdown(String arenaName) {
        BukkitRunnable countdown = arenaCountdowns.remove(arenaName);
        if (countdown != null) {
            countdown.cancel();
        }
    }

    public void stopArenaCountdownForAdmin(String arenaName) {
        stopArenaCountdown(arenaName);
    }

    public void startGameNow(String arenaName) {
        startGame(arenaName);
    }

    private void startGame(String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) return;

        // ZombieWaves ne fait tourner qu'UNE SEULE partie à la fois sur tout le
        // serveur (GameManager est global, pas par arène). Sans ce garde-fou,
        // le countdown de cette arène retombait à 0 puis ne se passait
        // silencieusement RIEN si une autre arène avait déjà une partie en
        // cours (GameManager#startGame() l'ignore). On prévient les joueurs et
        // on réessaie automatiquement dès que la partie en cours se termine.
        if (plugin.getGameManager().isGameRunning()) {
            broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() +
                "§eAnother game is already in progress on the server. Yours will start as soon as it ends...");
            BukkitRunnable retry = new BukkitRunnable() {
                @Override
                public void run() {
                    if (getPlayerCount(arenaName) < 1) {
                        // Plus personne en attente, inutile de continuer à réessayer
                        cancel();
                        return;
                    }
                    if (!plugin.getGameManager().isGameRunning()) {
                        cancel();
                        startGame(arenaName);
                    }
                }
            };
            retry.runTaskTimer(plugin, 100L, 100L); // vérifie toutes les 5 secondes
            return;
        }

        broadcastToArena(arenaName, plugin.getConfigManager().getPrefix() + 
            "§6§lGAME STARTING! GET READY!");
        
        // Teleport all players to game spawn
        Location gameSpawn = arena.getGameSpawnLocation();
        if (gameSpawn == null) {
            gameSpawn = arena.getLobbyLocation();
        }
        
        Set<UUID> enteringPlayers = new HashSet<>();
        for (UUID playerId : playersInLobby) {
            if (arenaName.equals(plugin.getArenaManager().getPlayerArena(playerId))) {
                enteringPlayers.add(playerId);
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.teleport(gameSpawn);
                    player.getInventory().clear();
                    plugin.getScoreboardManager().showGameScoreboard(player);
                }
            }
        }
        
        // Remove from lobby (they're now in game)
        playersInLobby.removeAll(enteringPlayers);
        
        // Start the game with exactly this arena's players
        plugin.getArenaManager().setActiveArena(arenaName);
        plugin.getGameManager().setSelectedArena(arenaName);
        plugin.getGameManager().startGame(enteringPlayers);
    }

    private void broadcastToArena(String arenaName, String message) {
        for (UUID playerId : playersInLobby) {
            if (arenaName.equals(plugin.getArenaManager().getPlayerArena(playerId))) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    player.sendMessage(message);
                }
            }
        }
    }

    private void updateLobbyScoreboards(String arenaName, int seconds) {
        for (UUID playerId : playersInLobby) {
            if (arenaName.equals(plugin.getArenaManager().getPlayerArena(playerId))) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.isOnline()) {
                    plugin.getScoreboardManager().updateLobbyScoreboard(player, arenaName, seconds);
                }
            }
        }
    }

    private String getMissingRequirements(Arena arena) {
        List<String> missing = new ArrayList<>();
        if (arena.getPos1() == null) missing.add("pos1");
        if (arena.getPos2() == null) missing.add("pos2");
        if (arena.getSpawnPoints().isEmpty()) missing.add("spawn points");
        if (arena.getLobbyLocation() == null) missing.add("lobby");
        return String.join(", ", missing);
    }

    public void removeAllPlayers() {
        for (UUID playerId : new HashSet<>(playersInLobby)) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                leaveArena(player);
            }
        }
        
        // Cancel all countdowns
        for (BukkitRunnable runnable : arenaCountdowns.values()) {
            runnable.cancel();
        }
        arenaCountdowns.clear();
    }
    
    public int getLobbyPlayerCount() {
        return playersInLobby.size();
    }

    /**
     * Nettoyage silencieux (pas de téléportation ni message) quand un joueur
     * en lobby OU en pleine partie se déconnecte. Sans cela, playersInLobby/
     * playerArenas et la position pré-lobby du joueur restaient bloqués
     * indéfiniment, et une partie pouvait rester "active" pour toujours avec
     * un dernier joueur fantôme.
     */
    public void handleQuit(Player player) {
        UUID id = player.getUniqueId();

        if (playersInLobby.remove(id)) {
            String arenaName = plugin.getArenaManager().getPlayerArena(id);
            plugin.getArenaManager().removePlayerArena(id);
            playerPreviousLocations.remove(id);

            if (arenaName != null) {
                checkAndStopCountdown(arenaName);
            }
            return;
        }

        if (plugin.getGameManager().isActivePlayer(player)) {
            // May auto-stop/reset the game if this was the last active player.
            plugin.getGameManager().removePlayer(player);
            plugin.getArenaManager().removePlayerArena(id);
            playerPreviousLocations.remove(id);
        }
    }
}
