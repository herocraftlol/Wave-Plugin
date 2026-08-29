package com.zombiewaves.managers;

import com.zombiewaves.ZombieWaves;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {

    private final ZombieWaves plugin;
    private final Map<String, ShopItem> shopItems;

    public ShopManager(ZombieWaves plugin) {
        this.plugin = plugin;
        this.shopItems = new HashMap<>();
        loadShopItems();
    }

    private void loadShopItems() {
        shopItems.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("shop.items");
        if (section == null) return;

        // Balances shop prices against how much gold mobs actually give: an item worth
        // "kills: 20" in config.yml costs roughly 20 average kills, whatever the current
        // mob-types gold-per-kill values are. Recomputed on every reload/plugin start.
        double avgGoldPerKill = plugin.getConfigManager().getAverageGoldPerKill(null);

        for (String key : section.getKeys(false)) {
            String typeStr = section.getString(key + ".type");
            String name = plugin.getConfigManager().colorize(section.getString(key + ".name", key));
            int price;
            if (section.contains(key + ".kills")) {
                double kills = section.getDouble(key + ".kills");
                price = (int) Math.round(kills * avgGoldPerKill);
            } else {
                price = section.getInt(key + ".price", 100);
            }
            List<String> lore = section.getStringList(key + ".description");
            int amount = section.getInt(key + ".amount", 1);
            Map<Enchantment, Integer> enchantments = parseEnchantments(section.getStringList(key + ".enchantments"));

            Material material;
            try {
                material = Material.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material in shop: " + typeStr);
                continue;
            }

            ShopItem item = new ShopItem(key, material, name, Math.max(1, price), lore, amount, enchantments);
            shopItems.put(key, item);
        }
    }

    /** Reloads shop items/prices from config.yml - called from ZombieWaves#reloadPlugin(). */
    public void reloadShopItems() {
        loadShopItems();
    }

    private Map<Enchantment, Integer> parseEnchantments(List<String> enchantmentStrings) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        if (enchantmentStrings == null) return enchantments;

        for (String enchantStr : enchantmentStrings) {
            String[] parts = enchantStr.split(":");
            if (parts.length == 2) {
                try {
                    Enchantment enchant = Enchantment.getByName(parts[0].toUpperCase());
                    int level = Integer.parseInt(parts[1]);
                    if (enchant != null) {
                        enchantments.put(enchant, level);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Invalid enchantment: " + enchantStr);
                }
            }
        }
        return enchantments;
    }

    public void openShop(Player player) {
        // Create a simple inventory GUI for the shop
        ShopInventory shopInventory = new ShopInventory(plugin, player);
        shopInventory.open();
    }

    public boolean purchaseItem(Player player, String itemKey) {
        ShopItem item = shopItems.get(itemKey);
        if (item == null) return false;

        int playerGold = plugin.getGameManager().getPlayerGold(player);
        if (playerGold < item.getPrice()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("not-enough-gold"));
            return false;
        }

        if (!plugin.getGameManager().removeGold(player, item.getPrice())) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + 
                plugin.getConfigManager().getMessage("purchase-failed"));
            return false;
        }

        // Give item to player
        ItemStack itemStack = item.createItemStack();
        
        // Check if inventory has space
        if (player.getInventory().firstEmpty() == -1) {
            // Drop item at player's location
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
        } else {
            player.getInventory().addItem(itemStack);
        }

        player.sendMessage(plugin.getConfigManager().getPrefix() + 
            plugin.getConfigManager().getMessage("purchase-success"));
        
        return true;
    }

    public Map<String, ShopItem> getShopItems() {
        return shopItems;
    }

    public ShopItem getShopItem(String key) {
        return shopItems.get(key);
    }

    // Inner class for shop items
    public static class ShopItem {
        private final String key;
        private final Material material;
        private final String name;
        private final int price;
        private final List<String> lore;
        private final int amount;
        private final Map<Enchantment, Integer> enchantments;

        public ShopItem(String key, Material material, String name, int price, 
                       List<String> lore, int amount, Map<Enchantment, Integer> enchantments) {
            this.key = key;
            this.material = material;
            this.name = name;
            this.price = price;
            this.lore = lore != null ? lore : new ArrayList<>();
            this.amount = amount;
            this.enchantments = enchantments != null ? enchantments : new HashMap<>();
        }

        public String getKey() { return key; }
        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public int getPrice() { return price; }
        public List<String> getLore() { return lore; }
        public int getAmount() { return amount; }
        public Map<Enchantment, Integer> getEnchantments() { return enchantments; }

        public ItemStack createItemStack() {
            ItemStack item = new ItemStack(material, amount);
            ItemMeta meta = item.getItemMeta();
            
            if (meta != null) {
                meta.setDisplayName(name);
                
                List<String> loreComponents = new ArrayList<>();
                for (String line : lore) {
                    loreComponents.add(line);
                }
                // Add price to lore
                loreComponents.add("");
                loreComponents.add("§6Price: §e" + price + " gold");
                
                meta.setLore(loreComponents);
                
                // Add enchantments
                if (!enchantments.isEmpty()) {
                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        meta.addEnchant(entry.getKey(), entry.getValue(), true);
                    }
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
                
                item.setItemMeta(meta);
            }
            
            return item;
        }
    }

    // Holder used to identify shop inventories in the click listener,
    // and to map each clicked slot back to the item key that was bought.
    public static class ShopHolder implements org.bukkit.inventory.InventoryHolder {
        private final Map<Integer, String> slotToKey = new HashMap<>();
        private org.bukkit.inventory.Inventory inventory;

        @Override
        public org.bukkit.inventory.Inventory getInventory() {
            return inventory;
        }

        public String getItemKey(int slot) {
            return slotToKey.get(slot);
        }
    }

    // Simple shop inventory GUI
    private static class ShopInventory {
        private final ZombieWaves plugin;
        private final Player player;

        public ShopInventory(ZombieWaves plugin, Player player) {
            this.plugin = plugin;
            this.player = player;
        }

        public void open() {
            Map<String, ShopItem> items = plugin.getShopManager().getShopItems();
            int size = Math.min(54, ((items.size() / 9) + 1) * 9);

            ShopHolder holder = new ShopHolder();
            org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(holder, size, "§6§lZombie Shop");
            holder.inventory = inventory;

            int slot = 0;
            for (ShopItem item : items.values()) {
                if (slot >= size) break;
                inventory.setItem(slot, item.createItemStack());
                holder.slotToKey.put(slot, item.getKey());
                slot++;
            }

            player.openInventory(inventory);
        }
    }
}