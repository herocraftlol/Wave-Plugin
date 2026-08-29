package com.zombiewaves.gui;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.utils.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inventory GUI listing every ZombieWaves arena so a player can join one (or
 * jump into a random one) or spectate the arena that currently has a game
 * running. Loosely modeled on HikaBrain's ArenaGUI, adapted to ZombieWaves:
 * there is only ever ONE game running server-wide (GameManager is global, not
 * per-arena), so at most one arena ever shows as "in progress" at a time.
 *
 * Layout of one page (54 slots = 6 rows x 9 columns):
 *   - Rows 1-5 (slots 0-44): one icon per arena, up to 45 per page
 *   - Row 6: slot 45 = previous page, slots 46-52 = "random arena" button
 *     (works across ALL arenas regardless of the page shown), slot 53 = next page
 */
public class ArenaSelectGUI {

    public static final String GUI_TITLE_BASE = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "\u2620 ZombieWaves Arenas";

    private static final int GUI_SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private static final int SLOT_PREV_PAGE = 45;
    private static final int SLOT_RANDOM_START = 46;
    private static final int SLOT_RANDOM_END = 52;
    private static final int SLOT_NEXT_PAGE = 53;

    private static final Pattern PAGE_TITLE_PATTERN = Pattern.compile("\\((\\d+)/(\\d+)\\)");

    private final ZombieWaves plugin;

    public ArenaSelectGUI(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int page) {
        player.openInventory(buildInventory(page));
    }

    // ── Construction ───────────────────────────────────────────────────────

    private List<Arena> getSortedArenas() {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getAllArenas());
        arenas.sort(Comparator.comparing(Arena::getName, String.CASE_INSENSITIVE_ORDER));
        return arenas;
    }

    private int getTotalPages(List<Arena> arenas) {
        return Math.max(1, (int) Math.ceil(arenas.size() / (double) PAGE_SIZE));
    }

    private String titleFor(int page, int totalPages) {
        if (totalPages <= 1) return GUI_TITLE_BASE;
        return GUI_TITLE_BASE + ChatColor.RESET + ChatColor.GRAY + " (" + (page + 1) + "/" + totalPages + ")";
    }

    public Inventory buildInventory(int page) {
        List<Arena> arenas = getSortedArenas();
        int totalPages = getTotalPages(arenas);
        if (page < 0) page = 0;
        if (page > totalPages - 1) page = totalPages - 1;

        Inventory inv = Bukkit.createInventory(null, GUI_SIZE, titleFor(page, totalPages));

        int start = page * PAGE_SIZE;
        int end = Math.min(arenas.size(), start + PAGE_SIZE);
        for (int i = start; i < end; i++) {
            inv.setItem(i - start, buildArenaItem(arenas.get(i)));
        }

        ItemStack filler = buildFiller();
        for (int i = end - start; i < PAGE_SIZE; i++) {
            inv.setItem(i, filler);
        }

        inv.setItem(SLOT_PREV_PAGE, page > 0 ? buildPageButton(false) : filler);

        ItemStack randomBtn = buildRandomButton(arenas);
        for (int i = SLOT_RANDOM_START; i <= SLOT_RANDOM_END; i++) {
            inv.setItem(i, randomBtn);
        }

        inv.setItem(SLOT_NEXT_PAGE, page < totalPages - 1 ? buildPageButton(true) : filler);

        return inv;
    }

    /**
     * True if this arena currently has a game running (the one and only
     * ZombieWaves game happening server-wide right now).
     */
    private boolean isCurrentlyPlaying(Arena arena) {
        return plugin.getGameManager().isGameRunning()
                && arena.getName().equalsIgnoreCase(plugin.getGameManager().getSelectedArena());
    }

    private boolean isJoinable(Arena arena) {
        return arena.isComplete()
                && !isCurrentlyPlaying(arena)
                && !plugin.getArenaManager().isArenaFull(arena.getName());
    }

    private boolean isSpectatable(Arena arena) {
        return isCurrentlyPlaying(arena)
                && plugin.getSpectatorManager().resolveSpectateLocation(arena) != null;
    }

    private ItemStack buildArenaItem(Arena arena) {
        String name = arena.getName();
        int current = plugin.getArenaManager().getPlayerCountInArena(name);
        int max = plugin.getArenaManager().getMaxPlayersPerArena();
        int spectators = plugin.getSpectatorManager().getSpectatorCount(name);

        Material mat;
        String displayName;
        String statusLine;
        ChatColor statusColor;

        if (!arena.isComplete()) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            displayName = ChatColor.GRAY + "" + ChatColor.BOLD + "\u2716 " + capitalize(name);
            statusLine = ChatColor.GRAY + "Not configured";
            statusColor = ChatColor.GRAY;
        } else if (isCurrentlyPlaying(arena)) {
            mat = Material.RED_STAINED_GLASS_PANE;
            displayName = ChatColor.RED + "" + ChatColor.BOLD + "\u2694 " + capitalize(name);
            statusLine = ChatColor.RED + "Game in progress (wave " + plugin.getGameManager().getCurrentWave() + ")";
            statusColor = ChatColor.RED;
        } else if (plugin.getArenaManager().isArenaFull(name)) {
            mat = Material.ORANGE_STAINED_GLASS_PANE;
            displayName = ChatColor.GOLD + "" + ChatColor.BOLD + "\u26A0 " + capitalize(name);
            statusLine = ChatColor.GOLD + "Full";
            statusColor = ChatColor.GOLD;
        } else {
            mat = Material.LIME_STAINED_GLASS_PANE;
            displayName = ChatColor.GREEN + "" + ChatColor.BOLD + "\u2714 " + capitalize(name);
            statusLine = ChatColor.GREEN + "Waiting for players";
            statusColor = ChatColor.GREEN;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "Players: " + statusColor + current + ChatColor.DARK_GRAY + "/" + ChatColor.GRAY + max);
        lore.add(ChatColor.GRAY + "Status : " + statusLine);
        if (spectators > 0 || isCurrentlyPlaying(arena)) {
            lore.add(ChatColor.GRAY + "Spectators: " + ChatColor.AQUA + spectators);
        }
        lore.add("");

        if (isJoinable(arena)) {
            lore.add(ChatColor.YELLOW + "\u25B6 Click to join!");
        } else if (isSpectatable(arena)) {
            lore.add(ChatColor.AQUA + "\uD83D\uDC41 Click to spectate!");
        } else {
            lore.add(ChatColor.RED + "\u2716 Unavailable");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildRandomButton(List<Arena> arenas) {
        long joinableCount = arenas.stream().filter(this::isJoinable).count();

        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "\u2726 Join a random arena");

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.GRAY + "You'll be sent to an available arena,");
        lore.add(ChatColor.GRAY + "prioritising ones that already have players.");
        lore.add("");
        if (joinableCount > 0) {
            lore.add(ChatColor.GREEN + "" + joinableCount + " arena(s) available");
            lore.add("");
            lore.add(ChatColor.YELLOW + "\u25B6 Click to play!");
        } else {
            lore.add(ChatColor.RED + "No arena available right now.");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildPageButton(boolean next) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(next
                ? ChatColor.YELLOW + "" + ChatColor.BOLD + "Next page \u25B6"
                : ChatColor.YELLOW + "" + ChatColor.BOLD + "\u25C0 Previous page");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            item.setItemMeta(meta);
        }
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Best arena to send a player to for the "random" button: prioritises
     * joinable arenas that already have players waiting (fills lobbies faster),
     * falls back to any joinable arena, or null if none are available.
     */
    public Arena findBestArenaForRandomJoin() {
        List<Arena> joinable = getSortedArenas().stream().filter(this::isJoinable).toList();
        if (joinable.isEmpty()) return null;

        return joinable.stream()
                .max(Comparator.comparingInt(a -> plugin.getArenaManager().getPlayerCountInArena(a.getName())))
                .orElse(joinable.get(0));
    }

    // ── Helpers pour le listener ───────────────────────────────────────────

    public String getArenaNameAt(int page, int slot) {
        if (slot < 0 || slot >= PAGE_SIZE) return null;
        List<Arena> arenas = getSortedArenas();
        int index = page * PAGE_SIZE + slot;
        if (index < 0 || index >= arenas.size()) return null;
        return arenas.get(index).getName();
    }

    public boolean isJoinable(String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        return arena != null && isJoinable(arena);
    }

    public boolean isSpectatable(String arenaName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        return arena != null && isSpectatable(arena);
    }

    public static boolean isRandomButton(int slot) {
        return slot >= SLOT_RANDOM_START && slot <= SLOT_RANDOM_END;
    }

    public static boolean isPrevPageButton(int slot) {
        return slot == SLOT_PREV_PAGE;
    }

    public static boolean isNextPageButton(int slot) {
        return slot == SLOT_NEXT_PAGE;
    }

    public static boolean isArenaGuiTitle(String title) {
        return title != null && title.startsWith(GUI_TITLE_BASE);
    }

    public static int parsePageFromTitle(String title) {
        if (title == null) return 0;
        Matcher matcher = PAGE_TITLE_PATTERN.matcher(title);
        if (!matcher.find()) return 0;
        try {
            return Math.max(0, Integer.parseInt(matcher.group(1)) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
