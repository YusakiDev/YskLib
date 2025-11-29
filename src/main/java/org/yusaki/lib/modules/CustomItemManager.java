package org.yusaki.lib.modules;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

/**
 * Unified custom item manager that supports multiple item sources
 * Provides a consistent API for accessing items from:
 * - itemedit:<id> - Items from ItemEdit plugin (external)
 * - internal:<id> - Items from YskLib ItemLibrary (internal storage)
 * - <MATERIAL> - Vanilla Minecraft materials
 * 
 * This manager abstracts away the complexity of working with multiple item sources
 * and provides a single, unified interface for all plugins using YskLib.
 */
public class CustomItemManager {
    
    private final YskLib lib;
    private final ItemLibrary itemLibrary;
    private final ItemEditManager itemEditManager;
    
    public CustomItemManager(YskLib lib) {
        this.lib = lib;
        this.itemLibrary = lib.getItemLibrary();
        this.itemEditManager = lib.getItemEditManager();
        
        lib.getLogger().info("CustomItemManager initialized");
        if (itemLibrary != null) {
            lib.getLogger().info("  - Internal items: Available (ItemLibrary)");
        }
        if (itemEditManager != null && itemEditManager.isInitialized()) {
            lib.getLogger().info("  - ItemEdit items: Available (" + 
                itemEditManager.getItemIds().size() + " items loaded)");
        }
    }
    
    /**
     * Get an item from any source
     * @param plugin The plugin requesting the item
     * @param reference Item reference (itemedit:<id>, internal:<id>, or MATERIAL)
     * @return ItemStack or null if not found
     */
    public ItemStack getItem(JavaPlugin plugin, String reference) {
        if (reference == null || reference.isEmpty()) {
            return null;
        }
        
        // ItemEdit items: itemedit:<id>
        if (reference.startsWith("itemedit:")) {
            String itemId = reference.substring(9);
            return getItemEditItem(itemId);
        }
        
        // Internal items: internal:<id>
        if (reference.startsWith("internal:")) {
            String itemId = reference.substring(9);
            return getInternalItem(plugin, itemId);
        }
        
        // Vanilla material
        try {
            Material material = Material.valueOf(reference.toUpperCase());
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            lib.logWarn(plugin, "Unknown item reference: " + reference);
            return null;
        }
    }
    
    /**
     * Check if an ItemStack matches a specific reference
     * @param plugin The plugin checking (needed for internal items)
     * @param item The ItemStack to check
     * @param reference Item reference (itemedit:<id>, internal:<id>, or MATERIAL)
     * @return true if the item matches
     */
    public boolean isItem(JavaPlugin plugin, ItemStack item, String reference) {
        if (item == null || reference == null || reference.isEmpty()) {
            return false;
        }
        
        // ItemEdit items: itemedit:<id>
        if (reference.startsWith("itemedit:")) {
            String itemId = reference.substring(9);
            return isItemEditItem(item, itemId);
        }
        
        // Internal items: internal:<id>
        if (reference.startsWith("internal:")) {
            String itemId = reference.substring(9);
            return isInternalItem(item, itemId, plugin);
        }
        
        // Vanilla material
        try {
            Material material = Material.valueOf(reference.toUpperCase());
            return item.getType() == material;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Check if an item reference exists
     * @param plugin The plugin checking
     * @param reference Item reference (itemedit:<id>, internal:<id>, or MATERIAL)
     * @return true if the item exists
     */
    public boolean hasItem(JavaPlugin plugin, String reference) {
        if (reference == null || reference.isEmpty()) {
            return false;
        }
        
        // ItemEdit items
        if (reference.startsWith("itemedit:")) {
            String itemId = reference.substring(9);
            return itemEditManager != null && 
                   itemEditManager.isInitialized() && 
                   itemEditManager.hasItem(itemId);
        }
        
        // Internal items
        if (reference.startsWith("internal:")) {
            String itemId = reference.substring(9);
            return itemLibrary != null && itemLibrary.hasItem(plugin, itemId);
        }
        
        // Vanilla material
        try {
            Material.valueOf(reference.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Save an item to internal storage
     * @param plugin The plugin saving the item
     * @param id Item ID (without "internal:" prefix)
     * @param item ItemStack to save
     */
    public void saveInternalItem(JavaPlugin plugin, String id, ItemStack item) {
        if (itemLibrary != null) {
            itemLibrary.saveItem(plugin, id, item);
            lib.logInfo(plugin, "Saved internal item: " + id);
        } else {
            lib.logWarn(plugin, "Cannot save internal item - ItemLibrary not available");
        }
    }
    
    /**
     * Get item type description for logging/display
     * @param reference Item reference
     * @return Human-readable description
     */
    public String getItemDescription(String reference) {
        if (reference == null || reference.isEmpty()) {
            return "Unknown";
        }
        
        if (reference.startsWith("itemedit:")) {
            return "ItemEdit: " + reference.substring(9);
        }
        
        if (reference.startsWith("internal:")) {
            return "Internal: " + reference.substring(9);
        }
        
        return "Vanilla: " + reference;
    }
    
    // ==================== Private Helper Methods ====================
    
    private ItemStack getItemEditItem(String itemId) {
        if (itemEditManager == null || !itemEditManager.isInitialized()) {
            return null;
        }
        
        return itemEditManager.getItem(itemId);
    }
    
    private ItemStack getInternalItem(JavaPlugin plugin, String itemId) {
        if (itemLibrary == null) {
            return null;
        }
        
        return itemLibrary.getItem(plugin, itemId);
    }
    
    private boolean isItemEditItem(ItemStack item, String itemId) {
        if (itemEditManager == null || !itemEditManager.isInitialized()) {
            return false;
        }
        
        return itemEditManager.isItem(item, itemId);
    }
    
    private boolean isInternalItem(ItemStack item, String itemId, JavaPlugin plugin) {
        if (itemLibrary == null) {
            return false;
        }
        
        ItemStack referenceItem = itemLibrary.getItem(plugin, itemId);
        if (referenceItem == null) {
            return false;
        }
        
        return isSimilarItem(item, referenceItem);
    }
    
    private boolean isSimilarItem(ItemStack item1, ItemStack item2) {
        if (item1 == null || item2 == null) {
            return false;
        }
        if (item1.getType() != item2.getType()) {
            return false;
        }
        if (item1.hasItemMeta() != item2.hasItemMeta()) {
            return false;
        }
        if (item1.hasItemMeta() && item2.hasItemMeta()) {
            return item1.getItemMeta().equals(item2.getItemMeta());
        }
        return true;
    }
}
