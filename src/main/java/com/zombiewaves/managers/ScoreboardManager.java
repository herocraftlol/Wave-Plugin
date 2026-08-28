package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardManager {

    private final ZombieWaves plugin;
    private final Map<UUID, Scoreboard> playerScoreboards;
    private BukkitTask updateTask;
    private Objective objective;

    public ScoreboardManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.playerScoreboards = new HashMap<>();
    }

    public void startScoreboardUpdates() {
        int updateInterval = plugin.getConfig().getInt("scoreboard.update-interval", 20);
        
        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
            }
        }.runTaskTimer(plugin, 20L, updateInterval);
    }

    public void stopScoreboardUpdates() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }

    public void showLobbyScoreboard(Player player, String arenaName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        
        String title = plugin.getConfigManager().colorize("§6§lZOMBIE WAVES");
        Objective obj = scoreboard.registerNewObjective("zwlobby", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        int score = 15;
        
        // Arena name
        Team arenaTeam = scoreboard.registerNewTeam("arena");
        arenaTeam.addEntry(ChatColor.WHITE.toString());
        arenaTeam.setPrefix(plugin.getConfigManager().colorize("§eArena: §f"));
        arenaTeam.setSuffix(arenaName);
        obj.getScore(ChatColor.WHITE.toString()).setScore(score--);
        
        // Players
        Team playersTeam = scoreboard.registerNewTeam("players");
        playersTeam.addEntry(ChatColor.YELLOW.toString());
        playersTeam.setPrefix(plugin.getConfigManager().colorize("§ePlayers: §f"));
        playersTeam.setSuffix("0/20");
        obj.getScore(ChatColor.YELLOW.toString()).setScore(score--);
        
        // Countdown placeholder
        Team countdownTeam = scoreboard.registerNewTeam("countdown");
        countdownTeam.addEntry(ChatColor.GREEN.toString());
        countdownTeam.setPrefix(plugin.getConfigManager().colorize("§eStarting: §f"));
        countdownTeam.setSuffix("Waiting...");
        obj.getScore(ChatColor.GREEN.toString()).setScore(score--);
        
        // Empty line
        obj.getScore(" ").setScore(score--);
        
        // Info
        Team infoTeam = scoreboard.registerNewTeam("info");
        infoTeam.addEntry(ChatColor.AQUA.toString());
        infoTeam.setPrefix(plugin.getConfigManager().colorize("§b/wave leave §7"));
        infoTeam.setSuffix(plugin.getConfigManager().colorize("to exit"));
        obj.getScore(ChatColor.AQUA.toString()).setScore(score--);
        
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
    }

    public void updateLobbyScoreboard(Player player, String arenaName, int countdown) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            showLobbyScoreboard(player, arenaName);
            scoreboard = playerScoreboards.get(player.getUniqueId());
        }
        
        if (scoreboard == null) return;
        
        // Update player count
        int playerCount = plugin.getLobbyManager().getPlayerCount(arenaName);
        int maxPlayers = plugin.getLobbyManager().getMaxPlayers();
        
        Team playersTeam = scoreboard.getTeam("players");
        if (playersTeam != null) {
            playersTeam.setSuffix(playerCount + "/" + maxPlayers);
        }
        
        // Update countdown
        Team countdownTeam = scoreboard.getTeam("countdown");
        if (countdownTeam != null) {
            if (countdown > 0) {
                countdownTeam.setSuffix(countdown + "s");
            } else {
                countdownTeam.setSuffix("Starting!");
            }
        }
    }

    public void showGameScoreboard(Player player) {
        // Use existing scoreboard logic
        createScoreboard(player);
    }

    public void clearScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        
        // Create objective
        String title = plugin.getConfigManager().colorize(
            plugin.getConfig().getString("scoreboard.title", "§6§lZOMBIE WAVES")
        );
        
        Objective obj = scoreboard.registerNewObjective("zombiewaves", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        // Get lines from config
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        
        // Add entries
        int score = lines.size();
        for (String line : lines) {
            String processedLine = processPlaceholders(player, line);
            // Use a unique dummy entry for each line
            String entry = "§r" + processedLine + "§r";
            
            // Get or create team for this entry to avoid duplicate names
            Team team = scoreboard.registerNewTeam("line_" + score);
            team.addEntry(entry);
            obj.getScore(entry).setScore(score);
            score--;
        }
        
        // Apply scoreboard to player
        player.setScoreboard(scoreboard);
        playerScoreboards.put(player.getUniqueId(), scoreboard);
    }

    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) {
            createScoreboard(player);
            return;
        }
        
        Objective obj = scoreboard.getObjective("zombiewaves");
        if (obj == null) {
            createScoreboard(player);
            return;
        }
        
        // Get lines from config
        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        
        // Update each entry
        Set<org.bukkit.scoreboard.Team> teams = scoreboard.getTeams();
        List<String> teamNames = new ArrayList<>();
        for (org.bukkit.scoreboard.Team team : teams) {
            teamNames.add(team.getName());
        }
        
        int score = lines.size();
        for (String line : lines) {
            String processedLine = processPlaceholders(player, line);
            String entry = "§r" + processedLine + "§r";
            String teamName = "line_" + score;
            
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            
            // Get current entry for this team
            Set<String> entries = team.getEntries();
            if (entries.isEmpty()) {
                team.addEntry(entry);
                obj.getScore(entry).setScore(score);
            } else {
                String currentEntry = entries.iterator().next();
                if (!currentEntry.equals(entry)) {
                    team.removeEntry(currentEntry);
                    obj.getScore(currentEntry).setScore(0); // Remove old entry
                    team.addEntry(entry);
                    obj.getScore(entry).setScore(score);
                }
            }
            score--;
        }
    }

    private void updateAllScoreboards() {
        for (UUID uuid : new ArrayList<>(playerScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                updateScoreboard(player);
            } else {
                // Remove scoreboard for offline players
                playerScoreboards.remove(uuid);
            }
        }
    }

    private String processPlaceholders(Player player, String text) {
        text = text.replace("{wave}", String.valueOf(plugin.getGameManager().getCurrentWave()));
        text = text.replace("{max-wave}", String.valueOf(plugin.getGameManager().getMaxWave()));
        text = text.replace("{kills}", String.valueOf(plugin.getGameManager().getPlayerKills(player)));
        text = text.replace("{gold}", String.valueOf(plugin.getGameManager().getPlayerGold(player)));
        text = text.replace("{remaining}", String.valueOf(plugin.getGameManager().getRemainingMobs()));
        text = text.replace("{next-wave}", String.valueOf(plugin.getGameManager().getCountdownSeconds()));
        
        // Handle French placeholders
        text = text.replace("{manche}", String.valueOf(plugin.getGameManager().getCurrentWave()));
        
        return plugin.getConfigManager().colorize(text);
    }

    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    public void onPlayerJoin(Player player) {
        if (plugin.getGameManager().isGameRunning()) {
            createScoreboard(player);
            if (updateTask == null) {
                startScoreboardUpdates();
            }
        }
    }

    public void onPlayerQuit(Player player) {
        removeScoreboard(player);
    }

    public void onGameStart() {
        startScoreboardUpdates();
        for (Player player : Bukkit.getOnlinePlayers()) {
            createScoreboard(player);
        }
    }

    public void onGameEnd() {
        stopScoreboardUpdates();
        for (UUID uuid : new ArrayList<>(playerScoreboards.keySet())) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                removeScoreboard(player);
            }
        }
        playerScoreboards.clear();
    }
}