package com.zombiewaves.commands;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.Arena;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WaveCommand implements CommandExecutor, TabCompleter {

    private final ZombieWaves plugin;

    public WaveCommand(ZombieWaves plugin) {
        this.plugin = plugin;
        plugin.getCommand("wave").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "join" -> handleJoin(sender, args);
            case "leave" -> handleLeave(sender);
            case "stop" -> {
                if (!sender.hasPermission("zombiewaves.admin")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                if (!plugin.getGameManager().isGameRunning()) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("no-wave-running"));
                    return true;
                }
                plugin.getGameManager().stopGame();
                plugin.getScoreboardManager().onGameEnd();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§cGame stopped!");
            }
            case "status" -> {
                if (sender instanceof Player player && plugin.getLobbyManager().isInArena(player)) {
                    sendLobbyStatus(player);
                } else if (plugin.getGameManager().isGameRunning()) {
                    sendStatus(sender);
                } else {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cNo game in progress. Use /wave join <arena> to play!");
                }
            }
            case "shop" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cThis command can only be used by players!");
                    return true;
                }
                if (!sender.hasPermission("zombiewaves.shop")) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        plugin.getConfigManager().getMessage("no-permission"));
                    return true;
                }
                plugin.getShopManager().openShop(player);
            }
            case "gold" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                        "§cThis command can only be used by players!");
                    return true;
                }
                int gold = plugin.getGameManager().getPlayerGold(player);
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§6Your gold: §e" + gold);
            }
            // Arena commands
            case "setpos1" -> handleSetPos1(sender, args);
            case "setpos2" -> handleSetPos2(sender, args);
            case "addspawn" -> handleAddSpawn(sender, args);
            case "removespawn" -> handleRemoveSpawn(sender, args);
            case "createarena" -> handleCreateArena(sender, args);
            case "deletearena" -> handleDeleteArena(sender, args);
            case "arenas" -> handleListArenas(sender);
            case "selectarena" -> handleSelectArena(sender, args);
            case "infoarena" -> handleInfoArena(sender, args);
            case "setlobby" -> handleSetLobby(sender, args);
            case "setspawn" -> handleSetSpawn(sender, args);
            case "setexit" -> handleSetExit(sender);
            case "spectate" -> handleSpectate(sender, args);
            case "unspectate" -> handleUnspectate(sender);
            // Merged from the former /zwaveadmin command
            case "reload" -> handleReload(sender);
            case "setwave" -> handleSetWave(sender, args);
            case "forcewave" -> handleForceWave(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void handleJoin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave join <arenaName>");
            sender.sendMessage("§7Available arenas:");
            for (var arena : plugin.getArenaManager().getAllArenas()) {
                String status = arena.isComplete() ? "§a✓" : "§c✗";
                int players = plugin.getArenaManager().getPlayerCountInArena(arena.getName());
                int max = plugin.getArenaManager().getMaxPlayersPerArena();
                sender.sendMessage("  " + status + " §f" + arena.getName() + " §7(" + players + "/" + max + ")");
            }
            return;
        }
        
        String arenaName = args[1];
        plugin.getLobbyManager().joinArena(player, arenaName);
    }

    private void handleLeave(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        
        plugin.getLobbyManager().leaveArena(player);
    }

    private void handleSetLobby(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        
        if (args.length < 2) {
            // Set global lobby
            plugin.getArenaManager().setGlobalLobbyLocation(player.getLocation());
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§aGlobal lobby set at your location!");
        } else {
            // Set arena-specific lobby
            String arenaName = args[1].toLowerCase();
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cArena '" + arenaName + "' does not exist!");
                return;
            }
            plugin.getArenaManager().setArenaLobby(arenaName, player.getLocation());
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§aLobby set for arena '" + arena.getName() + "'!");
        }
    }

    private void handleSetSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave setspawn <arenaName>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        plugin.getArenaManager().setArenaGameSpawn(arenaName, player.getLocation());
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aGame spawn set for arena '" + arena.getName() + "'!");
    }

    private void handleSetExit(CommandSender sender) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        
        plugin.getArenaManager().setGlobalExitLocation(player.getLocation());
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aExit location set at your location!");
    }

    private void sendLobbyStatus(Player player) {
        String arenaName = plugin.getLobbyManager().getPlayerArenaName(player);
        
        player.sendMessage("§6§l=== Lobby: " + arenaName + " ===");
        player.sendMessage("§ePlayers: §f" + plugin.getLobbyManager().getPlayerCount(arenaName) + 
            "§e/§f" + plugin.getLobbyManager().getMaxPlayers());
        player.sendMessage("§eWaiting for players... (need 2 to start)");
    }

    private void handleSetPos1(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave setpos1 <arenaName>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        Location target = getTargetBlock(player);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cCannot find target block! Look at a block.");
            return;
        }
        
        plugin.getArenaManager().setArenaPos1(arenaName, target);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aPosition 1 set for arena '" + arena.getName() + "' at " + 
            formatLocation(target));
    }

    private void handleSetPos2(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave setpos2 <arenaName>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        Location target = getTargetBlock(player);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cCannot find target block! Look at a block.");
            return;
        }
        
        plugin.getArenaManager().setArenaPos2(arenaName, target);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aPosition 2 set for arena '" + arena.getName() + "' at " + 
            formatLocation(target));
    }

    private void handleAddSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave addspawn <arenaName>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        Location target = getTargetBlock(player);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cCannot find target block! Look at a block.");
            return;
        }
        
        plugin.getArenaManager().addArenaSpawnPoint(arenaName, target);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aAdded spawn point to arena '" + arena.getName() + "' at " + 
            formatLocation(target));
    }

    private void handleRemoveSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave removespawn <arenaName>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        Location target = getTargetBlock(player);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cCannot find target block! Look at a block.");
            return;
        }
        
        plugin.getArenaManager().removeArenaSpawnPoint(arenaName, target);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aRemoved spawn point from arena '" + arena.getName() + "'");
    }

    private void handleCreateArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave createarena <name>");
            return;
        }
        
        String arenaName = args[1];
        if (plugin.getArenaManager().arenaExists(arenaName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' already exists!");
            return;
        }
        
        Arena arena = plugin.getArenaManager().createArena(arenaName);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aArena '" + arena.getName() + "' created! Use:");
        sender.sendMessage("§e  /wave setpos1 " + arenaName + " §7- Set first corner");
        sender.sendMessage("§e  /wave setpos2 " + arenaName + " §7- Set second corner");
        sender.sendMessage("§e  /wave addspawn " + arenaName + " §7- Add spawn points");
    }

    private void handleDeleteArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave deletearena <name>");
            return;
        }
        
        String arenaName = args[1];
        if (!plugin.getArenaManager().arenaExists(arenaName)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        plugin.getArenaManager().deleteArena(arenaName);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§cArena '" + arenaName + "' deleted!");
    }

    private void handleListArenas(CommandSender sender) {
        // Players get the visual GUI (join/spectate); console falls back to a chat list.
        if (sender instanceof Player player) {
            plugin.getArenaSelectGUI().open(player);
            return;
        }

        var arenas = plugin.getArenaManager().getAllArenas();
        if (arenas.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cNo arenas exist. Create one with §e/wave createarena <name>");
            return;
        }
        
        sender.sendMessage("§6§l=== Available Arenas ===");
        for (Arena arena : arenas) {
            String status = arena.isComplete() ? "§a✓" : "§c✗";
            int players = plugin.getArenaManager().getPlayerCountInArena(arena.getName());
            int max = plugin.getArenaManager().getMaxPlayersPerArena();
            sender.sendMessage(status + " §f" + arena.getName() + 
                " §7(" + players + "/" + max + " players, " + arena.getSpawnPoints().size() + " spawns)");
        }
    }

    private void handleSpectate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave spectate <arenaName>");
            return;
        }

        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }

        boolean gameHereRunning = plugin.getGameManager().isGameRunning()
            && arenaName.equalsIgnoreCase(plugin.getGameManager().getSelectedArena());
        if (!gameHereRunning) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("nothing-to-spectate"));
            return;
        }

        plugin.getSpectatorManager().addSpectator(player, arenaName);
    }

    private void handleUnspectate(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cThis command can only be used by players!");
            return;
        }

        if (!plugin.getSpectatorManager().isSpectating(player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cYou are not spectating any arena!");
            return;
        }

        plugin.getSpectatorManager().removeSpectator(player);
    }

    private void handleSelectArena(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave selectarena <name>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        if (!arena.isComplete()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' is not complete! Set pos1, pos2, and spawn points.");
            return;
        }
        
        plugin.getArenaManager().setActiveArena(arenaName);
        plugin.getGameManager().setSelectedArena(arenaName);
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aArena '" + arena.getName() + "' selected! Mobs will spawn here.");
    }

    private void handleInfoArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave infoarena <name>");
            return;
        }
        
        String arenaName = args[1].toLowerCase();
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        if (arena == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cArena '" + arenaName + "' does not exist!");
            return;
        }
        
        int players = plugin.getArenaManager().getPlayerCountInArena(arenaName);
        int max = plugin.getArenaManager().getMaxPlayersPerArena();
        
        sender.sendMessage("§6§l=== Arena: " + arena.getName() + " ===");
        sender.sendMessage("§ePlayers: §f" + players + "/" + max);
        sender.sendMessage("§eStatus: §f" + (arena.isComplete() ? "§aReady" : "§cIncomplete"));
        
        if (arena.getLobbyLocation() != null) {
            sender.sendMessage("§eLobby: §f" + formatLocation(arena.getLobbyLocation()));
        } else {
            sender.sendMessage("§eLobby: §cNot set");
        }
        
        if (arena.getGameSpawnLocation() != null) {
            sender.sendMessage("§eGame Spawn: §f" + formatLocation(arena.getGameSpawnLocation()));
        } else {
            sender.sendMessage("§eGame Spawn: §cNot set");
        }
        
        if (arena.getPos1() != null) {
            sender.sendMessage("§eBoundary 1: §f" + formatLocation(arena.getPos1()));
        } else {
            sender.sendMessage("§eBoundary 1: §cNot set");
        }
        
        if (arena.getPos2() != null) {
            sender.sendMessage("§eBoundary 2: §f" + formatLocation(arena.getPos2()));
        } else {
            sender.sendMessage("§eBoundary 2: §cNot set");
        }
        
        sender.sendMessage("§eSpawn Points: §f" + arena.getSpawnPoints().size());
        for (int i = 0; i < arena.getSpawnPoints().size(); i++) {
            sender.sendMessage("§e  " + (i + 1) + ". §f" + formatLocation(arena.getSpawnPoints().get(i)));
        }
    }

    private Location getTargetBlock(Player player) {
        RayTraceResult result = player.rayTraceBlocks(100);
        if (result != null && result.getHitBlock() != null) {
            return result.getHitBlock().getLocation();
        }
        return null;
    }

    private String formatLocation(Location loc) {
        if (loc == null) return "null";
        return String.format("§e%s §f[§e%d, %d, %d§f]",
            loc.getWorld().getName(),
            loc.getBlockX(),
            loc.getBlockY(),
            loc.getBlockZ());
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        plugin.reloadPlugin();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§aConfiguration reloaded!");
    }

    private void handleSetWave(CommandSender sender, String[] args) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cUsage: /wave setwave <number>");
            return;
        }
        try {
            int wave = Integer.parseInt(args[1]);
            if (wave < 1) {
                sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                    "§cWave number must be at least 1!");
                return;
            }
            plugin.getGameManager().setCurrentWave(wave);
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§aCurrent wave set to " + wave + "!");
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cInvalid wave number!");
        }
    }

    private void handleForceWave(CommandSender sender) {
        if (!sender.hasPermission("zombiewaves.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("no-permission"));
            return;
        }
        if (!plugin.getGameManager().isGameRunning()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + 
                "§cNo game is running!");
            return;
        }
        plugin.getWaveManager().clearAllMobs();
        plugin.getGameManager().nextWave();
        sender.sendMessage(plugin.getConfigManager().getPrefix() + 
            "§aForce starting next wave!");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§eZombie Waves Commands:");
        sender.sendMessage("§e/wave join <arena> §7- Join an arena");
        sender.sendMessage("§e/wave leave §7- Leave the arena");
        sender.sendMessage("§e/wave status §7- Show game status");
        sender.sendMessage("§e/wave shop §7- Open the shop");
        sender.sendMessage("§e/wave gold §7- Check your gold");
        sender.sendMessage("§e/wave arenas §7- Open the arena GUI (join / spectate)");
        sender.sendMessage("§e/wave spectate <arena> §7- Spectate an ongoing game");
        sender.sendMessage("§e/wave unspectate §7- Leave spectator mode");
        sender.sendMessage("§6§l=== Admin Commands ===");
        sender.sendMessage("§e/wave createarena <name> §7- Create new arena");
        sender.sendMessage("§e/wave setlobby [arena] §7- Set lobby location");
        sender.sendMessage("§e/wave setspawn <arena> §7- Set game spawn");
        sender.sendMessage("§e/wave setexit §7- Set exit location");
        sender.sendMessage("§e/wave setpos1 <arena> §7- Set corner 1");
        sender.sendMessage("§e/wave setpos2 <arena> §7- Set corner 2");
        sender.sendMessage("§e/wave addspawn <arena> §7- Add spawn point");
        sender.sendMessage("§e/wave selectarena <name> §7- Select arena");
        sender.sendMessage("§e/wave reload §7- Reload configuration");
        sender.sendMessage("§e/wave setwave <n> §7- Check current wave");
        sender.sendMessage("§e/wave forcewave §7- Force next wave");
        sender.sendMessage("§e/wave stop §7- Stop the game");
    }

    private void sendStatus(CommandSender sender) {
        sender.sendMessage("§6§l=== Zombie Waves Status ===");
        sender.sendMessage("§eWave: §f" + plugin.getGameManager().getCurrentWave() + 
            "§e/§f" + plugin.getGameManager().getMaxWave());
        sender.sendMessage("§eMobs remaining: §f" + plugin.getGameManager().getRemainingMobs());
        sender.sendMessage("§eNext wave in: §f" + plugin.getGameManager().getCountdownSeconds() + "s");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("join");
            completions.add("leave");
            completions.add("status");
            completions.add("shop");
            completions.add("gold");
            completions.add("arenas");
            completions.add("spectate");
            completions.add("unspectate");
            if (sender.hasPermission("zombiewaves.admin")) {
                completions.add("createarena");
                completions.add("deletearena");
                completions.add("selectarena");
                completions.add("setpos1");
                completions.add("setpos2");
                completions.add("addspawn");
                completions.add("removespawn");
                completions.add("setlobby");
                completions.add("setspawn");
                completions.add("setexit");
                completions.add("stop");
                completions.add("reload");
                completions.add("setwave");
                completions.add("forcewave");
            }
            
            return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
        }
        
        if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("join") || subCmd.equals("setpos1") || subCmd.equals("setpos2") || 
                subCmd.equals("addspawn") || subCmd.equals("removespawn") ||
                subCmd.equals("selectarena") || subCmd.equals("infoarena") ||
                subCmd.equals("deletearena") || subCmd.equals("setlobby") ||
                subCmd.equals("setspawn") || subCmd.equals("spectate")) {
                return plugin.getArenaManager().getAllArenas().stream()
                    .map(Arena::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            }
        }
        
        return completions;
    }
}