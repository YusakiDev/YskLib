package org.yusaki.lib.gui;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles command execution from GUI clicks
 */
public class GUICommandHandler {
    private final YskLib lib;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    
    public GUICommandHandler(YskLib lib) {
        this.lib = lib;
    }
    
    /**
     * Parse command configuration from an item's configuration section
     */
    public GUICommandConfig parseCommandConfig(ConfigurationSection itemSection) {
        if (itemSection == null || !itemSection.contains("commands")) {
            return null;
        }
        
        List<String> commands = itemSection.getStringList("commands");
        if (commands.isEmpty()) {
            // Support single command string
            String singleCommand = itemSection.getString("command");
            if (singleCommand != null) {
                commands = new ArrayList<>();
                commands.add(singleCommand);
            }
        }
        
        if (commands.isEmpty()) {
            return null;
        }
        
        String executeAs = itemSection.getString("execute_as", "player");
        String permission = itemSection.getString("permission");
        boolean closeOnClick = itemSection.getBoolean("close_on_click", false);
        String sound = itemSection.getString("sound");
        int cooldown = itemSection.getInt("cooldown", 0);
        
        return new GUICommandConfig(commands, executeAs, permission, closeOnClick, sound, cooldown);
    }
    
    /**
     * Handle command execution from a GUI click
     */
    public void handleCommandClick(InventoryClickEvent event, GUICommandConfig commandConfig, 
                                  Map<String, Object> context, JavaPlugin plugin) {
        if (commandConfig == null || commandConfig.getCommands().isEmpty()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        
        // Check cooldown
        if (commandConfig.getCooldown() > 0) {
            String cooldownKey = player.getUniqueId() + ":" + event.getSlot();
            Long lastClick = cooldowns.get(cooldownKey);
            if (lastClick != null) {
                long timeSince = System.currentTimeMillis() - lastClick;
                if (timeSince < commandConfig.getCooldown()) {
                    return; // Still on cooldown
                }
            }
            cooldowns.put(cooldownKey, System.currentTimeMillis());
        }
        
        // Check permission
        if (commandConfig.getPermission() != null && !player.hasPermission(commandConfig.getPermission())) {
            lib.sendMessage(plugin, player, "no_permission");
            return;
        }
        
        // Execute commands directly - handle threading within executeCommands
        executeCommands(commandConfig, player, context, plugin);
        
        // Play sound
        if (commandConfig.getSound() != null) {
            try {
                Sound sound = Sound.valueOf(commandConfig.getSound());
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                lib.logWarn(plugin, "Invalid sound: " + commandConfig.getSound());
            }
        }
        
        // Close GUI if configured
        if (commandConfig.shouldCloseOnClick()) {
            player.closeInventory();
        }
    }
    
    /**
     * Execute the commands (extracted method for scheduler compatibility)
     */
    private void executeCommands(GUICommandConfig commandConfig, Player player, 
                                Map<String, Object> context, JavaPlugin plugin) {
        for (String command : commandConfig.getCommands()) {
            String processedCommand = processPlaceholders(command, player, context);
            
            if (commandConfig.isConsoleExecution()) {
                // Console commands need to be dispatched on the global scheduler for Folia
                lib.getFoliaLib().getScheduler().runNextTick((task) -> {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                });
            } else {
                // Player commands need to be executed on the entity scheduler for Folia
                lib.getFoliaLib().getScheduler().runAtEntity(player, (task) -> {
                    player.performCommand(processedCommand);
                });
            }
            
            lib.logDebug(plugin, "Executed GUI command: " + processedCommand + " as " + commandConfig.getExecuteAs());
        }
    }
    
    /**
     * Process placeholders in command string
     */
    private String processPlaceholders(String command, Player player, Map<String, Object> context) {
        // Player placeholders
        command = command.replace("{player}", player.getName())
                        .replace("{uuid}", player.getUniqueId().toString())
                        .replace("{world}", player.getWorld().getName())
                        .replace("{x}", String.valueOf(player.getLocation().getBlockX()))
                        .replace("{y}", String.valueOf(player.getLocation().getBlockY()))
                        .replace("{z}", String.valueOf(player.getLocation().getBlockZ()));
        
        // Context placeholders
        if (context != null) {
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                command = command.replace(placeholder, String.valueOf(entry.getValue()));
            }
        }
        
        return command;
    }
    
    /**
     * Clean up old cooldown entries
     */
    public void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 3600000); // Remove entries older than 1 hour
    }
}