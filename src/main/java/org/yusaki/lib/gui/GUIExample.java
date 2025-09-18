package org.yusaki.lib.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.Arrays;
import java.util.List;

/**
 * Example class demonstrating how to use the YskLib GUI Framework.
 * Shows various patterns for creating and managing pattern-based GUIs.
 */
public class GUIExample {
    private final JavaPlugin plugin;
    private final YskLib yskLib;
    
    public GUIExample(JavaPlugin plugin, YskLib yskLib) {
        this.plugin = plugin;
        this.yskLib = yskLib;
    }
    
    /**
     * Example 1: Simple terminal GUI with static content
     */
    public void openSimpleTerminalGUI(Player player) {
        yskLib.createGUI(plugin, "terminal")
                .onClick('U', event -> {
                    // Handle upgrade button click
                    player.sendMessage("§eUpgrade button clicked!");
                })
                .onClick('I', event -> {
                    // Handle info button click
                    player.sendMessage("§bFactory Information: Level 1, Processing Speed: 100%");
                })
                .onClick('C', event -> {
                    // Handle craft button click
                    player.sendMessage("§aCrafting started!");
                })
                .open(player);
    }
    
    /**
     * Example 2: Dynamic content GUI with context data
     */
    public void openDynamicFactoryGUI(Player player, String factoryName, int factoryLevel) {
        yskLib.createGUI(plugin, "terminal")
                .setContext("factoryName", factoryName)
                .setContext("factoryLevel", factoryLevel)
                .setDynamicContent('O', () -> createOutputDisplay(factoryName))
                .setDynamicContent('F', () -> createFuelDisplay(75)) // 75% fuel
                .setDynamicContentArray('R', () -> createRequirementsDisplay())
                .onClick('U', (event, gui) -> {
                    String name = gui.getContext("factoryName", String.class);
                    Integer level = gui.getContext("factoryLevel", Integer.class);
                    player.sendMessage("§eUpgrading " + name + " from level " + level + "!");
                })
                .onClick('I', (event, gui) -> {
                    String name = gui.getContext("factoryName", String.class);
                    Integer level = gui.getContext("factoryLevel", Integer.class);
                    openInfoGUI(player, name, level);
                })
                .open(player);
    }
    
    /**
     * Example 3: Browse GUI with pagination
     */
    public void openBrowseGUI(Player player, List<ItemStack> allItems, int page) {
        int itemsPerPage = 35; // 5 rows of 7 items
        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        
        // Create items for current page
        ItemStack[] pageItems = new ItemStack[itemsPerPage];
        for (int i = 0; i < itemsPerPage; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < allItems.size()) {
                pageItems[i] = allItems.get(itemIndex);
            } else {
                pageItems[i] = new ItemStack(Material.AIR);
            }
        }
        
        yskLib.createGUI(plugin, "browse")
                .setContext("page", page)
                .setContext("allItems", allItems)
                .setDynamicContentArray('I', () -> pageItems)
                .onClick('N', (event, gui) -> {
                    // Next page
                    int currentPage = gui.getContext("page", Integer.class);
                    @SuppressWarnings("unchecked")
                    List<ItemStack> items = gui.getContext("allItems", List.class);
                    if ((currentPage + 1) * itemsPerPage < items.size()) {
                        openBrowseGUI(player, items, currentPage + 1);
                    }
                })
                .onClick('P', (event, gui) -> {
                    // Previous page
                    int currentPage = gui.getContext("page", Integer.class);
                    @SuppressWarnings("unchecked")
                    List<ItemStack> items = gui.getContext("allItems", List.class);
                    if (currentPage > 0) {
                        openBrowseGUI(player, items, currentPage - 1);
                    }
                })
                .onClick('I', event -> {
                    // Handle item click
                    ItemStack clickedItem = event.getCurrentItem();
                    if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                        player.sendMessage("§aYou clicked: " + clickedItem.getType().name());
                    }
                })
                .open(player);
    }
    
    /**
     * Example 4: Confirmation dialog
     */
    public void openConfirmationGUI(Player player, String action, Runnable confirmAction) {
        yskLib.createGUI(plugin, "confirm")
                .setContext("action", action)
                .onClick('Y', event -> {
                    player.closeInventory();
                    confirmAction.run();
                    player.sendMessage("§aAction confirmed!");
                })
                // 'N' is automatically handled by close_on_click in config
                .open(player);
    }
    
    /**
     * Example 5: Chained GUI navigation
     */
    public void openMainMenu(Player player) {
        // This would require a "main_menu" GUI configuration
        yskLib.createGUI(plugin, "terminal") // Using terminal as example
                .onClick('U', event -> {
                    // Navigate to upgrade GUI
                    openUpgradeGUI(player);
                })
                .onClick('I', event -> {
                    // Navigate to info GUI
                    openInfoGUI(player, "Main Factory", 1);
                })
                .open(player);
    }
    
    private void openUpgradeGUI(Player player) {
        // Open upgrade-specific GUI
        player.sendMessage("§eOpening upgrade menu...");
        // Would open another GUI type here
    }
    
    private void openInfoGUI(Player player, String factoryName, int level) {
        player.sendMessage("§b=== " + factoryName + " Information ===");
        player.sendMessage("§7Level: " + level);
        player.sendMessage("§7Status: Active");
        player.sendMessage("§7Efficiency: 100%");
    }
    
    // Helper methods for creating dynamic content
    private ItemStack createOutputDisplay(String factoryName) {
        ItemStack item = new ItemStack(Material.DIAMOND);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b" + factoryName + " Output");
            meta.setLore(Arrays.asList(
                    "§7Current output from this factory",
                    "§7Production rate: 1 item/minute"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack createFuelDisplay(int fuelPercent) {
        Material fuelMaterial = fuelPercent > 50 ? Material.COAL : Material.CHARCOAL;
        ItemStack item = new ItemStack(fuelMaterial);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Fuel: " + fuelPercent + "%");
            meta.setLore(Arrays.asList(
                    "§7Current fuel level",
                    fuelPercent > 25 ? "§aFuel level is good" : "§cFuel level is low!"
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    private ItemStack[] createRequirementsDisplay() {
        // Create example requirements
        ItemStack[] requirements = new ItemStack[14]; // 2 rows of 7
        
        requirements[0] = createRequirementItem(Material.IRON_INGOT, 10, true);
        requirements[1] = createRequirementItem(Material.GOLD_INGOT, 5, true);
        requirements[2] = createRequirementItem(Material.DIAMOND, 1, false);
        requirements[3] = createRequirementItem(Material.REDSTONE, 20, true);
        
        // Fill remaining slots with air
        for (int i = 4; i < requirements.length; i++) {
            requirements[i] = new ItemStack(Material.AIR);
        }
        
        return requirements;
    }
    
    private ItemStack createRequirementItem(Material material, int amount, boolean hasEnough) {
        ItemStack item = new ItemStack(material, Math.min(amount, 64));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String status = hasEnough ? "§a✓ Available" : "§c✗ Missing";
            meta.setDisplayName("§f" + material.name().replace("_", " "));
            meta.setLore(Arrays.asList(
                    "§7Required: " + amount,
                    status
            ));
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Initialize GUI configurations for this plugin
     */
    public void initializeGUIConfigurations() {
        yskLib.loadGUIConfigurations(plugin);
    }
}