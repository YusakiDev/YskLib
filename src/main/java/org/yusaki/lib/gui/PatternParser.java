package org.yusaki.lib.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Parses ASCII patterns from configuration and converts them to slot assignments.
 * Based on LamCrafting's pattern-based GUI system.
 */
public class PatternParser {
    private final List<String> pattern;
    private final Map<Character, SlotDefinition> slotDefinitions;
    private final int inventorySize;
    private final InventoryType inventoryType;
    
    public PatternParser(List<String> pattern, ConfigurationSection itemsSection) {
        this(pattern, itemsSection, InventoryType.CHEST);
    }
    
    public PatternParser(List<String> pattern, ConfigurationSection itemsSection, InventoryType inventoryType) {
        this.pattern = new ArrayList<>(pattern);
        this.slotDefinitions = new HashMap<>();
        this.inventoryType = inventoryType;
        this.inventorySize = calculateInventorySize();
        
        parseSlotDefinitions(itemsSection);
        validatePattern();
    }
    
    /**
     * Calculate inventory size based on inventory type
     */
    private int calculateInventorySize() {
        if (inventoryType == InventoryType.CHEST) {
            int rows = pattern.size();
            return rows * 9; // Each row is 9 slots
        } else {
            // For other inventory types, use their fixed sizes
            return inventoryType.getDefaultSize();
        }
    }
    
    /**
     * Parse slot definitions from configuration
     */
    private void parseSlotDefinitions(ConfigurationSection itemsSection) {
        if (itemsSection == null) return;
        
        for (String key : itemsSection.getKeys(false)) {
            if (key.length() != 1) continue; // Only single character keys
            
            char character = key.charAt(0);
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            
            if (itemSection != null) {
                SlotDefinition definition = parseSlotDefinition(itemSection);
                slotDefinitions.put(character, definition);
            }
        }
    }
    
    /**
     * Parse individual slot definition from configuration
     */
    private SlotDefinition parseSlotDefinition(ConfigurationSection section) {
        String materialName = section.getString("material", "AIR");
        Material material;
        
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.AIR;
        }
        
        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");
        boolean isDynamic = material == Material.AIR && (name == null || name.isEmpty());
        
        return new SlotDefinition(material, name, lore, isDynamic);
    }
    
    /**
     * Validate pattern based on inventory type
     */
    private void validatePattern() {
        if (inventoryType == InventoryType.CHEST) {
            // Chest inventories require 9-character rows
            for (String row : pattern) {
                if (row.length() != 9) {
                    throw new IllegalArgumentException("Pattern row must be exactly 9 characters: " + row);
                }
            }
        } else {
            // For other inventory types, validate that pattern doesn't exceed slot count
            int totalPatternSlots = pattern.stream().mapToInt(String::length).sum();
            if (totalPatternSlots > inventorySize) {
                throw new IllegalArgumentException("Pattern has " + totalPatternSlots + " characters but " + 
                    inventoryType + " inventory only has " + inventorySize + " slots");
            }
        }
    }
    
    /**
     * Get slot index for specific row and column
     */
    public int getSlotIndex(int row, int col) {
        if (inventoryType == InventoryType.CHEST) {
            return row * 9 + col;
        } else {
            // For other inventory types, calculate based on pattern layout
            int index = 0;
            for (int r = 0; r < row && r < pattern.size(); r++) {
                index += pattern.get(r).length();
            }
            return index + col;
        }
    }
    
    /**
     * Get all slot indices for a specific pattern character
     */
    public List<Integer> getPatternSlots(char character) {
        List<Integer> slots = new ArrayList<>();
        
        for (int row = 0; row < pattern.size(); row++) {
            String rowPattern = pattern.get(row);
            for (int col = 0; col < rowPattern.length(); col++) {
                if (rowPattern.charAt(col) == character) {
                    slots.add(getSlotIndex(row, col));
                }
            }
        }
        
        return slots;
    }
    
    /**
     * Check if a slot is reserved for dynamic content
     */
    public boolean isReservedSlot(int slot) {
        char character = getCharacterAtSlot(slot);
        SlotDefinition definition = slotDefinitions.get(character);
        return definition != null && definition.isDynamic();
    }
    
    /**
     * Get the pattern character at a specific slot index
     */
    public char getCharacterAtSlot(int slot) {
        if (slot < 0) {
            return ' '; // Space character for negative slots
        }
        
        if (inventoryType == InventoryType.CHEST) {
            int row = slot / 9;
            int col = slot % 9;
            
            if (row >= pattern.size() || col >= 9) {
                return ' '; // Space character for invalid slots
            }
            
            return pattern.get(row).charAt(col);
        } else {
            // For other inventory types, calculate position based on pattern layout
            int currentSlot = 0;
            for (int row = 0; row < pattern.size(); row++) {
                String rowPattern = pattern.get(row);
                if (currentSlot + rowPattern.length() > slot) {
                    int col = slot - currentSlot;
                    if (col < rowPattern.length()) {
                        return rowPattern.charAt(col);
                    }
                }
                currentSlot += rowPattern.length();
            }
            return ' '; // Space character for invalid slots
        }
    }
    
    /**
     * Create ItemStack for a pattern character
     */
    public ItemStack createItemForCharacter(char character) {
        SlotDefinition definition = slotDefinitions.get(character);
        if (definition == null) {
            return new ItemStack(Material.AIR);
        }
        
        ItemStack item = new ItemStack(definition.getMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            // Set display name
            if (definition.getName() != null && !definition.getName().isEmpty()) {
                String name = ChatColor.translateAlternateColorCodes('&', definition.getName());
                meta.setDisplayName(name);
            }
            
            // Set lore
            if (definition.getLore() != null && !definition.getLore().isEmpty()) {
                List<String> coloredLore = new ArrayList<>();
                for (String line : definition.getLore()) {
                    coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(coloredLore);
            }
            
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * Get all dynamic slot characters (characters that represent slots for runtime content)
     */
    public Set<Character> getDynamicSlotCharacters() {
        Set<Character> dynamicChars = new HashSet<>();
        
        for (Map.Entry<Character, SlotDefinition> entry : slotDefinitions.entrySet()) {
            if (entry.getValue().isDynamic()) {
                dynamicChars.add(entry.getKey());
            }
        }
        
        return dynamicChars;
    }
    
    // Getters
    public List<String> getPattern() {
        return new ArrayList<>(pattern);
    }
    
    public int getInventorySize() {
        return inventorySize;
    }
    
    public Map<Character, SlotDefinition> getSlotDefinitions() {
        return new HashMap<>(slotDefinitions);
    }
    
    /**
     * Inner class representing a slot definition from configuration
     */
    public static class SlotDefinition {
        private final Material material;
        private final String name;
        private final List<String> lore;
        private final boolean isDynamic;
        
        public SlotDefinition(Material material, String name, List<String> lore, boolean isDynamic) {
            this.material = material;
            this.name = name;
            this.lore = lore != null ? new ArrayList<>(lore) : new ArrayList<>();
            this.isDynamic = isDynamic;
        }
        
        public Material getMaterial() { return material; }
        public String getName() { return name; }
        public List<String> getLore() { return new ArrayList<>(lore); }
        public boolean isDynamic() { return isDynamic; }
    }
}