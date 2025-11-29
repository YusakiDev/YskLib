package org.yusaki.lib.modules;

import emanondev.itemedit.ItemEdit;
import emanondev.itemedit.storage.ServerStorage;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

/**
 * Manager for ItemEdit plugin integration
 * Provides access to custom items stored in ItemEdit
 */
public class ItemEditManager {
    private final YskLib lib;
    private ServerStorage serverStorage = null;
    private boolean initialized = false;

    public ItemEditManager(YskLib lib) {
        this.lib = lib;
        initialize();
    }

    /**
     * Initialize ItemEdit integration
     */
    private void initialize() {
        try {
            // Get ItemEdit plugin instance
            ItemEdit plugin = (ItemEdit) lib.getServer().getPluginManager().getPlugin("ItemEdit");
            if (plugin == null) {
                lib.getLogger().info("ItemEdit plugin not found - integration disabled");
                return;
            }

            // Check if ItemEdit is enabled
            if (!plugin.isEnabled()) {
                lib.getLogger().warning("ItemEdit plugin is not enabled yet");
                return;
            }

            // Get server storage
            this.serverStorage = plugin.getServerStorage();

            if (this.serverStorage == null) {
                lib.getLogger().warning("ItemEdit server storage is not available");
                return;
            }

            this.initialized = true;
            lib.getLogger().info("ItemEdit integration initialized successfully!");
            lib.getLogger().info("Available ItemEdit items: " + serverStorage.getIds().size());
        } catch (Exception e) {
            lib.getLogger().warning("Failed to initialize ItemEdit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get an item from ItemEdit by ID
     * @param id The ItemEdit item ID
     * @return ItemStack or null if not found
     */
    public ItemStack getItem(String id) {
        if (!initialized || serverStorage == null) {
            return null;
        }

        try {
            // Try direct access first
            ItemStack item = serverStorage.getItem(id);
            if (item != null) {
                return item.clone();
            }

            // If not found, try with .yml extension
            item = serverStorage.getItem(id + ".yml");
            if (item != null) {
                return item.clone();
            }

            return null;
        } catch (Exception e) {
            lib.getLogger().warning("Failed to get ItemEdit item: " + id);
            return null;
        }
    }

    /**
     * Check if an ItemEdit item exists
     * @param id The ItemEdit item ID
     * @return true if the item exists
     */
    public boolean hasItem(String id) {
        if (!initialized || serverStorage == null) {
            return false;
        }
        return serverStorage.getIds().contains(id) || serverStorage.getIds().contains(id + ".yml");
    }

    /**
     * Check if ItemEdit is initialized
     * @return true if ItemEdit integration is active
     */
    public boolean isInitialized() {
        return initialized && serverStorage != null;
    }

    /**
     * Check if an ItemStack matches a specific ItemEdit item by ID
     * @param item The ItemStack to check
     * @param id The ItemEdit ID to match
     * @return true if the item matches the ItemEdit item
     */
    public boolean isItem(ItemStack item, String id) {
        if (!initialized || serverStorage == null || item == null) {
            return false;
        }

        try {
            // Get the reference item from ItemEdit storage
            ItemStack editItem = serverStorage.getItem(id);
            if (editItem == null) {
                // Try with .yml extension if not found
                editItem = serverStorage.getItem(id + ".yml");
                if (editItem == null) {
                    return false;
                }
            }

            // Basic type check first
            if (item.getType() != editItem.getType()) {
                return false;
            }

            // Try to use ItemEdit's static API for direct comparison
            try {
                Class<?> itemEditClass = Class.forName("emanondev.itemedit.ItemEdit");
                Object instance = itemEditClass.getMethod("get").invoke(null);
                boolean directMatch = (boolean) instance.getClass()
                    .getMethod("isId", ItemStack.class, String.class)
                    .invoke(instance, item, id);

                if (directMatch) {
                    return true;
                }
            } catch (Exception e) {
                // Silently fall through to fallback methods
            }

            // Fallback to display name comparison
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                editItem.hasItemMeta() && editItem.getItemMeta().hasDisplayName()) {

                String name1 = item.getItemMeta().getDisplayName();
                String name2 = editItem.getItemMeta().getDisplayName();

                if (name1.equals(name2)) {
                    return true;
                }
            }

            // Last resort: isSimilar fallback
            return item.isSimilar(editItem);

        } catch (Exception e) {
            lib.getLogger().warning("Failed to check ItemEdit item: " + id + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all available ItemEdit item IDs
     * @return Set of item IDs
     */
    public java.util.Set<String> getItemIds() {
        if (!initialized || serverStorage == null) {
            return java.util.Collections.emptySet();
        }
        return serverStorage.getIds();
    }

    /**
     * Load an ItemEdit item for a plugin
     * This method integrates with YskLib's ItemLibrary by providing ItemEdit items
     * @param plugin The plugin requesting the item
     * @param id The ItemEdit item ID
     * @return ItemStack or null if not found
     */
    public ItemStack loadItemForPlugin(JavaPlugin plugin, String id) {
        ItemStack item = getItem(id);
        if (item != null) {
            lib.logDebug(plugin, "Loaded ItemEdit item: " + id);
        }
        return item;
    }
}
