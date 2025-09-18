package org.yusaki.lib.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryType;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration data structure for GUI definitions.
 * Contains all the information needed to create a pattern-based GUI.
 */
public class GUIConfig {
    private final String title;
    private final List<String> pattern;
    private final ConfigurationSection itemsSection;
    private final int clickCooldown;
    private final List<Character> closeOnClick;
    private final boolean soundEffects;
    private final InventoryType inventoryType;
    private final boolean allowPlayerInventoryClicks;
    
    public GUIConfig(String title, List<String> pattern, ConfigurationSection itemsSection,
                     int clickCooldown, List<Character> closeOnClick, boolean soundEffects) {
        this(title, pattern, itemsSection, clickCooldown, closeOnClick, soundEffects, InventoryType.CHEST, false);
    }
    
    public GUIConfig(String title, List<String> pattern, ConfigurationSection itemsSection,
                     int clickCooldown, List<Character> closeOnClick, boolean soundEffects, 
                     InventoryType inventoryType, boolean allowPlayerInventoryClicks) {
        this.title = title;
        this.pattern = new ArrayList<>(pattern);
        this.itemsSection = itemsSection;
        this.clickCooldown = clickCooldown;
        this.closeOnClick = new ArrayList<>(closeOnClick);
        this.soundEffects = soundEffects;
        this.inventoryType = inventoryType;
        this.allowPlayerInventoryClicks = allowPlayerInventoryClicks;
    }
    
    public String getTitle() {
        return title;
    }
    
    public List<String> getPattern() {
        return new ArrayList<>(pattern);
    }
    
    public ConfigurationSection getItemsSection() {
        return itemsSection;
    }
    
    public int getClickCooldown() {
        return clickCooldown;
    }
    
    public List<Character> getCloseOnClick() {
        return new ArrayList<>(closeOnClick);
    }
    
    public boolean hasSoundEffects() {
        return soundEffects;
    }
    
    public InventoryType getInventoryType() {
        return inventoryType;
    }
    
    public boolean allowsPlayerInventoryClicks() {
        return allowPlayerInventoryClicks;
    }
}