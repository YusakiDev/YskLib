package org.yusaki.lib.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for the GUI framework following YskLib's module pattern.
 * Handles GUI configurations, event routing, and GUI lifecycle management.
 */
public class GUIManager implements Listener {
    private final YskLib lib;
    private final Map<JavaPlugin, Map<String, GUIConfig>> pluginConfigs;
    private final Map<JavaPlugin, File> configFiles;
    private final Map<JavaPlugin, FileConfiguration> configurations;
    private final Map<UUID, PatternGUI> activeGUIs;
    private final Map<UUID, Long> lastClickTimes;
    private final GUICommandHandler commandHandler;
    
    // Shared storage support
    private final boolean useSharedStorage;
    private final File sharedConfigFile;
    private final FileConfiguration sharedConfiguration;
    private final Map<String, GUIConfig> sharedConfigs;
    
    public GUIManager(YskLib lib) {
        this.lib = lib;
        this.pluginConfigs = new ConcurrentHashMap<>();
        this.configFiles = new ConcurrentHashMap<>();
        this.configurations = new ConcurrentHashMap<>();
        this.activeGUIs = new ConcurrentHashMap<>();
        this.lastClickTimes = new ConcurrentHashMap<>();
        this.commandHandler = new GUICommandHandler(lib);
        
        // Initialize shared storage
        this.useSharedStorage = lib.getConfig().getBoolean("modules.gui.shared-storage", false);
        this.sharedConfigs = new ConcurrentHashMap<>();
        
        if (useSharedStorage) {
            String sharedFileName = lib.getConfig().getString("modules.gui.shared-file", "shared_guis.yml");
            this.sharedConfigFile = new File(lib.getDataFolder(), sharedFileName);
            if (!sharedConfigFile.exists()) {
                try {
                    sharedConfigFile.getParentFile().mkdirs();
                    sharedConfigFile.createNewFile();
                } catch (Exception e) {
                    lib.getLogger().severe("Failed to create shared GUI config file: " + e.getMessage());
                }
            }
            this.sharedConfiguration = YamlConfiguration.loadConfiguration(sharedConfigFile);
            loadSharedConfigs();
        } else {
            this.sharedConfigFile = null;
            this.sharedConfiguration = null;
        }
        
        // Register event listener
        Bukkit.getPluginManager().registerEvents(this, lib);
    }
    
    /**
     * Load GUI configurations for a plugin
     */
    public void loadConfigurations(JavaPlugin plugin) {
        if (useSharedStorage) {
            // When using shared storage, we don't need to load plugin-specific configs
            return;
        }
        
        pluginConfigs.computeIfAbsent(plugin, k -> new ConcurrentHashMap<>());
        
        File guiConfigFile = new File(plugin.getDataFolder(), "gui.yml");
        configFiles.put(plugin, guiConfigFile);
        
        if (!guiConfigFile.exists()) {
            try {
                plugin.saveResource("gui.yml", false);
            } catch (IllegalArgumentException e) {
                // gui.yml doesn't exist in plugin resources, create empty one
                lib.logDebug(plugin, "No default gui.yml found, creating empty configuration");
            }
        }
        
        FileConfiguration config = YamlConfiguration.loadConfiguration(guiConfigFile);
        configurations.put(plugin, config);
        
        loadConfigurationsFromFile(plugin, config);
    }
    
    /**
     * Load shared GUI configurations
     */
    private void loadSharedConfigs() {
        if (!useSharedStorage) return;
        
        sharedConfigs.clear();
        ConfigurationSection guiSection = sharedConfiguration.getConfigurationSection("gui");
        if (guiSection != null) {
            for (String guiType : guiSection.getKeys(false)) {
                try {
                    ConfigurationSection typeSection = guiSection.getConfigurationSection(guiType);
                    if (typeSection != null) {
                        GUIConfig config = parseGUIConfig(typeSection);
                        sharedConfigs.put(guiType.toLowerCase(), config);
                        lib.getLogger().info("Loaded shared GUI config: " + guiType);
                    }
                } catch (Exception e) {
                    lib.getLogger().warning("Failed to load shared GUI config " + guiType + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Load GUI configurations from a file configuration
     */
    private void loadConfigurationsFromFile(JavaPlugin plugin, FileConfiguration config) {
        Map<String, GUIConfig> configs = pluginConfigs.get(plugin);
        configs.clear();
        
        ConfigurationSection guiSection = config.getConfigurationSection("gui");
        if (guiSection != null) {
            for (String guiType : guiSection.getKeys(false)) {
                try {
                    ConfigurationSection typeSection = guiSection.getConfigurationSection(guiType);
                    if (typeSection != null) {
                        GUIConfig guiConfig = parseGUIConfig(typeSection);
                        configs.put(guiType.toLowerCase(), guiConfig);
                        lib.logDebug(plugin, "Loaded GUI config: " + guiType);
                    }
                } catch (Exception e) {
                    lib.logWarn(plugin, "Failed to load GUI config " + guiType + ": " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Parse GUI configuration from ConfigurationSection
     */
    private GUIConfig parseGUIConfig(ConfigurationSection section) {
        String title = section.getString("title", "GUI");
        List<String> pattern = section.getStringList("pattern");
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        ConfigurationSection behaviorSection = section.getConfigurationSection("behaviors");
        
        int clickCooldown = 500; // Default 500ms
        List<Character> closeOnClick = new ArrayList<>();
        boolean soundEffects = true;
        InventoryType inventoryType = InventoryType.CHEST; // Default
        boolean allowPlayerInventoryClicks = false; // Default
        
        if (behaviorSection != null) {
            clickCooldown = behaviorSection.getInt("click_cooldown", 500);
            soundEffects = behaviorSection.getBoolean("sound_effects", true);
            allowPlayerInventoryClicks = behaviorSection.getBoolean("allow_player_inventory_clicks", false);
            
            String inventoryTypeStr = behaviorSection.getString("inventory_type", "CHEST");
            try {
                inventoryType = InventoryType.valueOf(inventoryTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                lib.logWarn(null, "Invalid inventory type: " + inventoryTypeStr + ", using CHEST");
                inventoryType = InventoryType.CHEST;
            }
            
            List<String> closeChars = behaviorSection.getStringList("close_on_click");
            for (String charStr : closeChars) {
                if (!charStr.isEmpty()) {
                    closeOnClick.add(charStr.charAt(0));
                }
            }
        }
        
        return new GUIConfig(title, pattern, itemsSection, clickCooldown, closeOnClick, soundEffects, inventoryType, allowPlayerInventoryClicks);
    }
    
    /**
     * Create a GUIBuilder for the specified GUI type
     */
    public GUIBuilder createGUI(JavaPlugin plugin, String guiType) {
        GUIConfig config = getGUIConfig(plugin, guiType);
        if (config == null) {
            throw new IllegalArgumentException("GUI configuration not found: " + guiType);
        }
        
        return new GUIBuilder(lib, plugin, config, this);
    }
    
    /**
     * Get GUI configuration for a specific type
     */
    private GUIConfig getGUIConfig(JavaPlugin plugin, String guiType) {
        if (useSharedStorage) {
            return sharedConfigs.get(guiType.toLowerCase());
        }
        
        Map<String, GUIConfig> configs = pluginConfigs.get(plugin);
        return configs != null ? configs.get(guiType.toLowerCase()) : null;
    }
    
    /**
     * Register an active GUI
     */
    public void registerGUI(UUID playerId, PatternGUI gui) {
        activeGUIs.put(playerId, gui);
    }
    
    /**
     * Unregister an active GUI
     */
    public void unregisterGUI(UUID playerId) {
        activeGUIs.remove(playerId);
        lastClickTimes.remove(playerId);
    }
    
    /**
     * Check if click is within cooldown period
     */
    private boolean isClickOnCooldown(UUID playerId, int cooldownMs) {
        if (cooldownMs <= 0) return false;
        
        Long lastClick = lastClickTimes.get(playerId);
        if (lastClick == null) return false;
        
        return (System.currentTimeMillis() - lastClick) < cooldownMs;
    }
    
    /**
     * Handle inventory click events
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        PatternGUI gui = activeGUIs.get(player.getUniqueId());
        
        if (gui == null || !gui.getInventory().equals(event.getInventory())) {
            return;
        }
        
        // Handle player inventory clicks based on configuration
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(gui.getInventory())) {
            // Always cancel clicks in GUI inventory - will be handled by specific handlers
            event.setCancelled(true);
        } else {
            // Check if player inventory clicks are allowed
            if (!gui.getConfig().allowsPlayerInventoryClicks()) {
                event.setCancelled(true);
            }
            // For player inventory clicks, don't process cooldown/handling
            if (event.getClickedInventory() == player.getInventory()) {
                return;
            }
        }
        
        // Check click cooldown
        if (isClickOnCooldown(player.getUniqueId(), gui.getConfig().getClickCooldown())) {
            return;
        }
        
        lastClickTimes.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Handle the click
        gui.handleClick(event);
    }
    
    /**
     * Handle inventory close events
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        PatternGUI gui = activeGUIs.get(player.getUniqueId());
        
        if (gui != null && gui.getInventory().equals(event.getInventory())) {
            gui.handleClose(event);
            unregisterGUI(player.getUniqueId());
        }
    }
    
    /**
     * Get active GUI for a player
     */
    public PatternGUI getActiveGUI(UUID playerId) {
        return activeGUIs.get(playerId);
    }
    
    /**
     * Get all available GUI types for a plugin
     */
    public Set<String> getAvailableGUITypes(JavaPlugin plugin) {
        if (useSharedStorage) {
            return sharedConfigs.keySet();
        }
        
        Map<String, GUIConfig> configs = pluginConfigs.get(plugin);
        return configs != null ? configs.keySet() : new HashSet<>();
    }
    
    /**
     * Get the command handler
     */
    public GUICommandHandler getCommandHandler() {
        return commandHandler;
    }
}