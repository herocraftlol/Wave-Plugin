package com.zombiewaves.listeners;

import com.zombiewaves.ZombieWaves;
import com.zombiewaves.managers.ShopManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Handles clicks inside the Zombie Waves shop GUI opened by ShopManager#openShop.
 * Without this listener, the shop only ever displays items - clicking them did
 * nothing (no purchase, no gold deducted) and, worse, let players freely take
 * the display items out of the inventory.
 */
public class ShopClickListener implements Listener {

    private final ZombieWaves plugin;

    public ShopClickListener(ZombieWaves plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onShopClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ShopManager.ShopHolder shopHolder)) {
            return;
        }

        // Always cancel clicks in the shop GUI: it's a display/purchase menu,
        // not a real inventory - items should never be moved in or out of it.
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        // Ignore clicks in the player's own inventory (bottom half) while the
        // shop is open, and ignore clicks with no slot (e.g. outside the GUI).
        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != shopHolder) {
            return;
        }

        String itemKey = shopHolder.getItemKey(event.getSlot());
        if (itemKey == null) {
            return;
        }

        boolean success = plugin.getShopManager().purchaseItem(player, itemKey);
        player.playSound(player.getLocation(),
            success ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO,
            1.0f, 1.0f);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onShopDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof ShopManager.ShopHolder) {
            // Prevent dragging items into/out of the shop GUI entirely.
            event.setCancelled(true);
        }
    }
}
