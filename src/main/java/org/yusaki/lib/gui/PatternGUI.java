package org.yusaki.lib.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Base class for pattern-based GUIs.
 * Handles pattern rendering, dynamic content, and event routing.
 */
public class PatternGUI implements InventoryHolder {
    protected final YskLib lib;
    protected final JavaPlugin plugin;
    protected final GUIConfig config;
    protected final PatternParser parser;
    protected Inventory inventory;
    protected final Map<Character, Consumer<InventoryClickEvent>> clickHandlers;
    protected final Map<Character, Supplier<ItemStack>> dynamicContentProviders;
    protected final Map<Character, Supplier<ItemStack[]>> dynamicArrayProviders;
    protected final Map<String, Object> context;
    protected final GUIManager guiManager;
    
    public PatternGUI(YskLib lib, JavaPlugin plugin, GUIConfig config, GUIManager guiManager) {
        this.lib = lib;
        this.plugin = plugin;
        this.config = config;
        this.guiManager = guiManager;
        this.parser = new PatternParser(config.getPattern(), config.getItemsSection(), config.getInventoryType());
        this.clickHandlers = new HashMap<>();
        this.dynamicContentProviders = new HashMap<>();
        this.dynamicArrayProviders = new HashMap<>();
        this.context = new HashMap<>();
        
        // Create inventory with placeholder processing and inventory type
        String title = ChatColor.translateAlternateColorCodes('&', config.getTitle());
        if (config.getInventoryType() == InventoryType.CHEST) {
            this.inventory = Bukkit.createInventory(this, parser.getInventorySize(), title);
        } else {
            this.inventory = Bukkit.createInventory(this, config.getInventoryType(), title);
        }
        
        // Apply the pattern
        applyPattern();
    }
    
    /**
     * Apply the ASCII pattern to the inventory
     */
    protected void applyPattern() {
        for (int slot = 0; slot < parser.getInventorySize(); slot++) {
            if (!parser.isReservedSlot(slot)) {
                char character = parser.getCharacterAtSlot(slot);
                if (character != ' ') { // Skip empty slots
                    ItemStack item = parser.createItemForCharacter(character);
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Fill dynamic slots
        updateDynamicContent();
    }
    
    /**
     * Update dynamic content in reserved slots
     */
    public void updateDynamicContent() {
        // Single item providers
        for (Map.Entry<Character, Supplier<ItemStack>> entry : dynamicContentProviders.entrySet()) {
            char character = entry.getKey();
            Supplier<ItemStack> provider = entry.getValue();
            
            for (int slot : parser.getPatternSlots(character)) {
                if (parser.isReservedSlot(slot)) {
                    ItemStack item = provider.get();
                    inventory.setItem(slot, item);
                }
            }
        }
        
        // Array providers (for filling multiple slots)
        for (Map.Entry<Character, Supplier<ItemStack[]>> entry : dynamicArrayProviders.entrySet()) {
            char character = entry.getKey();
            Supplier<ItemStack[]> provider = entry.getValue();
            
            ItemStack[] items = provider.get();
            var slots = parser.getPatternSlots(character);
            
            for (int i = 0; i < slots.size() && i < items.length; i++) {
                int slot = slots.get(i);
                if (parser.isReservedSlot(slot)) {
                    inventory.setItem(slot, items[i]);
                }
            }
        }
    }
    
    /**
     * Set a click handler for a pattern character
     */
    public PatternGUI onClick(char character, Consumer<InventoryClickEvent> handler) {
        clickHandlers.put(character, handler);
        return this;
    }
    
    /**
     * Set dynamic content provider for a pattern character
     */
    public PatternGUI setDynamicContent(char character, Supplier<ItemStack> provider) {
        dynamicContentProviders.put(character, provider);
        return this;
    }
    
    /**
     * Set dynamic content provider for multiple slots of the same character
     */
    public PatternGUI setDynamicContentArray(char character, Supplier<ItemStack[]> provider) {
        dynamicArrayProviders.put(character, provider);
        return this;
    }
    
    /**
     * Set context data for the GUI
     */
    public PatternGUI setContext(String key, Object value) {
        context.put(key, value);
        return this;
    }
    
    /**
     * Process placeholders in a string using the current context
     */
    private String processPlaceholders(String text) {
        if (text == null || !text.contains("{")) return text;
        
        String processed = text;
        lib.logDebug(plugin, "Processing placeholders in text: " + text);
        lib.logDebug(plugin, "Context size: " + context.size());
        
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            lib.logDebug(plugin, "Replacing " + placeholder + " with " + value);
            processed = processed.replace(placeholder, value);
        }
        
        lib.logDebug(plugin, "Final processed text: " + processed);
        return processed;
    }
    
    /**
     * Get context data from the GUI
     */
    @SuppressWarnings("unchecked")
    public <T> T getContext(String key, Class<T> type) {
        Object value = context.get(key);
        return type.isInstance(value) ? (T) value : null;
    }
    
    /**
     * Open the GUI for a player
     */
    public void open(Player player) {
        // Recreate inventory with processed title and current context
        String processedTitle = processPlaceholders(config.getTitle());
        processedTitle = ChatColor.translateAlternateColorCodes('&', processedTitle);
        
        // Create new inventory with processed title and inventory type
        if (config.getInventoryType() == InventoryType.CHEST) {
            inventory = Bukkit.createInventory(this, parser.getInventorySize(), processedTitle);
        } else {
            inventory = Bukkit.createInventory(this, config.getInventoryType(), processedTitle);
        }
        
        // Apply pattern and update dynamic content
        applyPattern();
        updateDynamicContent();
        
        guiManager.registerGUI(player.getUniqueId(), this);
        player.openInventory(inventory);
        
        if (config.hasSoundEffects()) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        }
    }
    
    /**
     * Handle inventory click events
     */
    public void handleClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        char character = parser.getCharacterAtSlot(slot);
        
        // Check if this character should close the GUI
        if (config.getCloseOnClick().contains(character)) {
            event.getWhoClicked().closeInventory();
            return;
        }
        
        // Check for command configuration first
        if (config.getItemsSection() != null) {
            ConfigurationSection itemConfig = config.getItemsSection().getConfigurationSection(String.valueOf(character));
            if (itemConfig != null) {
                GUICommandConfig commandConfig = guiManager.getCommandHandler().parseCommandConfig(itemConfig);
                if (commandConfig != null) {
                    guiManager.getCommandHandler().handleCommandClick(event, commandConfig, context, plugin);
                    
                    // If command config handled the click and no custom handler exists, return
                    if (!clickHandlers.containsKey(character)) {
                        // Update dynamic content after command execution
                        updateDynamicContent();
                        return;
                    }
                }
            }
        }
        
        // Execute click handler if one exists
        Consumer<InventoryClickEvent> handler = clickHandlers.get(character);
        if (handler != null) {
            try {
                handler.accept(event);
                
                // Play click sound
                if (config.hasSoundEffects() && event.getWhoClicked() instanceof Player) {
                    Player player = (Player) event.getWhoClicked();
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                }
            } catch (Exception e) {
                lib.logWarn(plugin, "Error handling GUI click: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Update dynamic content after click in case handlers changed data
        updateDynamicContent();
    }
    
    /**
     * Handle inventory close events
     */
    public void handleClose(InventoryCloseEvent event) {
        // Override in subclasses for custom close handling
        onClose(event);
    }
    
    /**
     * Called when the GUI is closed. Override for custom behavior.
     */
    protected void onClose(InventoryCloseEvent event) {
        // Default: do nothing
    }
    
    /**
     * Refresh the entire GUI by re-applying the pattern and updating dynamic content
     */
    public void refresh() {
        inventory.clear();
        applyPattern();
    }
    
    /**
     * Close the GUI for all viewers
     */
    public void close() {
        inventory.getViewers().forEach(viewer -> viewer.closeInventory());
    }
    
    // Getters
    @Override
    public Inventory getInventory() {
        return inventory;
    }
    
    public GUIConfig getConfig() {
        return config;
    }
    
    public PatternParser getParser() {
        return parser;
    }
    
    public Map<String, Object> getContext() {
        return new HashMap<>(context);
    }
    
    public JavaPlugin getPlugin() {
        return plugin;
    }
}