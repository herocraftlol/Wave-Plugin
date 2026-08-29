package com.zombiewaves;

import com.zombiewaves.commands.WaveCommand;
import com.zombiewaves.gui.ArenaSelectGUI;
import com.zombiewaves.listeners.EntityDeathListener;
import com.zombiewaves.listeners.EntitySpawnListener;
import com.zombiewaves.listeners.PlayerJoinListener;
import com.zombiewaves.listeners.PlayerQuitListener;
import com.zombiewaves.listeners.PlayerDeathListener;
import com.zombiewaves.listeners.LobbyListener;
import com.zombiewaves.listeners.ShopClickListener;
import com.zombiewaves.listeners.ArenaSelectGUIListener;
import com.zombiewaves.managers.ArenaManager;
import com.zombiewaves.managers.GameManager;
import com.zombiewaves.managers.LobbyManager;
import com.zombiewaves.managers.ShopManager;
import com.zombiewaves.managers.ScoreboardManager;
import com.zombiewaves.managers.SpectatorManager;
import com.zombiewaves.managers.WaveManager;
import com.zombiewaves.utils.Arena;
import com.zombiewaves.utils.ConfigManager;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

public class ZombieWaves extends JavaPlugin {

    private static ZombieWaves instance;
    private ConfigManager configManager;
    private ArenaManager arenaManager;
    private GameManager gameManager;
    private WaveManager waveManager;
    private ShopManager shopManager;
    private ScoreboardManager scoreboardManager;
    private LobbyManager lobbyManager;
    private SpectatorManager spectatorManager;
    private ArenaSelectGUI arenaSelectGUI;

    @Override
    public void onEnable() {
        instance = this;
        
        // Register Arena serialization
        ConfigurationSerialization.registerClass(Arena.class, "Arena");
        
        // Save default configs
        saveDefaultConfig();
        saveResource("arenas.yml", false);
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        
        // Initialize managers
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this);
        waveManager = new WaveManager(this);
        shopManager = new ShopManager(this);
        scoreboardManager = new ScoreboardManager(this);
        lobbyManager = new LobbyManager(this);
        spectatorManager = new SpectatorManager(this);
        arenaSelectGUI = new ArenaSelectGUI(this);
        
        // Register commands
        getCommand("wave").setExecutor(new WaveCommand(this));
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new EntityDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new EntitySpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new LobbyListener(this), this);
        getServer().getPluginManager().registerEvents(new ShopClickListener(this), this);
        getServer().getPluginManager().registerEvents(spectatorManager, this);
        getServer().getPluginManager().registerEvents(new ArenaSelectGUIListener(this, arenaSelectGUI), this);
        
        getLogger().info("ZombieWaves has been enabled!");
    }

    @Override
    public void onDisable() {
        // Stop any running game
        if (gameManager != null && gameManager.isGameRunning()) {
            gameManager.stopGame();
        }
        
        // Remove all players from lobby
        if (lobbyManager != null) {
            lobbyManager.removeAllPlayers();
        }
        
        // Remove all spectators
        if (spectatorManager != null) {
            spectatorManager.removeAllSpectators();
        }
        
        // Save arenas
        if (arenaManager != null) {
            arenaManager.saveArenas();
        }
        
        getLogger().info("ZombieWaves has been disabled!");
    }

    public void reloadPlugin() {
        reloadConfig();
        configManager.reload();
        arenaManager.loadArenas();
        shopManager.reloadShopItems();
        if (gameManager != null) {
            gameManager.reload();
        }
    }

    public static ZombieWaves getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public WaveManager getWaveManager() {
        return waveManager;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public LobbyManager getLobbyManager() {
        return lobbyManager;
    }

    public SpectatorManager getSpectatorManager() {
        return spectatorManager;
    }

    public ArenaSelectGUI getArenaSelectGUI() {
        return arenaSelectGUI;
    }
}