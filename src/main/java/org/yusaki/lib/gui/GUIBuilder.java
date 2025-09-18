package org.yusaki.lib.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent API builder for creating pattern-based GUIs.
 * Provides an intuitive interface for GUI creation and configuration.
 */
public class GUIBuilder {
    private final YskLib lib;
    private final JavaPlugin plugin;
    private final GUIConfig config;
    private final GUIManager guiManager;
    private final PatternGUI gui;
    
    public GUIBuilder(YskLib lib, JavaPlugin plugin, GUIConfig config, GUIManager guiManager) {
        this.lib = lib;
        this.plugin = plugin;
        this.config = config;
        this.guiManager = guiManager;
        this.gui = new PatternGUI(lib, plugin, config, guiManager);
    }
    
    /**
     * Process placeholders in a string using the GUI context
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    private String processPlaceholders(String text) {
        if (text == null || !text.contains("{")) return text;
        
        String processed = text;
        for (Map.Entry<String, Object> entry : gui.getContext().entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            String value = String.valueOf(entry.getValue());
            processed = processed.replace(placeholder, value);
        }
        return processed;
    }
    
    /**
     * Set a click handler for a pattern character
     * @param character The pattern character to handle clicks for
     * @param handler The click handler function
     * @return This builder for method chaining
     */
    public GUIBuilder onClick(char character, Consumer<InventoryClickEvent> handler) {
        gui.onClick(character, handler);
        return this;
    }
    
    /**
     * Set a click handler with context access
     * @param character The pattern character to handle clicks for
     * @param handler The click handler with GUI context access
     * @return This builder for method chaining
     */
    public GUIBuilder onClick(char character, ContextClickHandler handler) {
        gui.onClick(character, event -> handler.handle(event, gui));
        return this;
    }
    
    /**
     * Set dynamic content for a pattern character (single item)
     * @param character The pattern character to provide content for
     * @param provider Supplier that provides the ItemStack for this slot
     * @return This builder for method chaining
     */
    public GUIBuilder setDynamicContent(char character, Supplier<ItemStack> provider) {
        gui.setDynamicContent(character, provider);
        return this;
    }
    
    /**
     * Set dynamic content for a pattern character (multiple items)
     * @param character The pattern character to provide content for
     * @param provider Supplier that provides ItemStacks for all slots of this character
     * @return This builder for method chaining
     */
    public GUIBuilder setDynamicContentArray(char character, Supplier<ItemStack[]> provider) {
        gui.setDynamicContentArray(character, provider);
        return this;
    }
    
    /**
     * Set context data for the GUI
     * @param key The context key
     * @param value The context value
     * @return This builder for method chaining
     */
    public GUIBuilder setContext(String key, Object value) {
        gui.setContext(key, value);
        return this;
    }
    
    /**
     * Set multiple context values at once
     * @param contextData Map of context keys and values
     * @return This builder for method chaining
     */
    public GUIBuilder setContext(java.util.Map<String, Object> contextData) {
        contextData.forEach(gui::setContext);
        return this;
    }
    
    /**
     * Build and return the GUI instance
     * @return The configured PatternGUI
     */
    public PatternGUI build() {
        return gui;
    }
    
    /**
     * Build and immediately open the GUI for a player
     * @param player The player to open the GUI for
     * @return The opened PatternGUI
     */
    public PatternGUI open(Player player) {
        gui.open(player);
        return gui;
    }
    
    /**
     * Convenience method to create a simple button click handler
     * @param character The pattern character
     * @param action The action to perform when clicked
     * @return This builder for method chaining
     */
    public GUIBuilder onButtonClick(char character, Runnable action) {
        return onClick(character, event -> {
            try {
                action.run();
            } catch (Exception e) {
                lib.logWarn(plugin, "Error in button click handler: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Convenience method to create a button that closes the GUI when clicked
     * @param character The pattern character
     * @return This builder for method chaining
     */
    public GUIBuilder onCloseButton(char character) {
        return onClick(character, event -> event.getWhoClicked().closeInventory());
    }
    
    /**
     * Create a button that opens another GUI when clicked
     * @param character The pattern character
     * @param targetGUIType The GUI type to open
     * @return This builder for method chaining
     */
    public GUIBuilder onNavigateButton(char character, String targetGUIType) {
        return onClick(character, event -> {
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                try {
                    guiManager.createGUI(plugin, targetGUIType)
                            .setContext(gui.getContext()) // Copy current context
                            .open(player);
                } catch (Exception e) {
                    lib.logWarn(plugin, "Failed to navigate to GUI " + targetGUIType + ": " + e.getMessage());
                }
            }
        });
    }
    
    /**
     * Set up a confirmation dialog for a character
     * @param character The pattern character
     * @param confirmAction The action to perform if confirmed
     * @param confirmMessage The confirmation message
     * @return This builder for method chaining
     */
    public GUIBuilder onConfirmButton(char character, Runnable confirmAction, String confirmMessage) {
        return onClick(character, event -> {
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                // Simple confirmation via chat message - could be enhanced with a confirmation GUI
                lib.sendMessage(plugin, player, confirmMessage);
                // For now, just execute the action - in a real implementation you might want
                // to create a confirmation GUI or use chat confirmation
                confirmAction.run();
            }
        });
    }
    
    /**
     * Static factory method to create a builder from an existing GUI configuration
     * @param lib YskLib instance
     * @param plugin The plugin creating the GUI
     * @param guiType The GUI type to create
     * @param guiManager The GUI manager
     * @return A new GUIBuilder instance
     */
    public static GUIBuilder create(YskLib lib, JavaPlugin plugin, String guiType, GUIManager guiManager) {
        return guiManager.createGUI(plugin, guiType);
    }
    
    /**
     * Functional interface for click handlers that need GUI context access
     */
    @FunctionalInterface
    public interface ContextClickHandler {
        void handle(InventoryClickEvent event, PatternGUI gui);
    }
}