package org.yusaki.lib;

import com.tcoded.folialib.FoliaLib;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.config.ConfigUpdateOptions;
import org.yusaki.lib.config.ConfigUpdateService;
import org.yusaki.lib.modules.ItemLibrary;
import org.yusaki.lib.gui.GUIManager;

import java.util.List;

public final class YskLib extends JavaPlugin {
    private FoliaLib foliaLib;
    private ItemLibrary itemLibrary;
    private GUIManager guiManager;
    
    @Override
    public void onEnable() {
        // Initialize FoliaLib
        foliaLib = new FoliaLib(this);
        
        // Save default config if it doesn't exist
        saveDefaultConfig();
        
        // Initialize ItemLibrary if enabled
        if (getConfig().getBoolean("modules.item-library.enabled", true)) {
            itemLibrary = new ItemLibrary(this);
            getLogger().info("ItemLibrary module enabled!");
        }
        
        // Initialize GUIManager if enabled
        if (getConfig().getBoolean("modules.gui.enabled", true)) {
            guiManager = new GUIManager(this);
            getLogger().info("GUI Framework module enabled!");
        }
        
        getLogger().info("YskLib enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("YskLib disabled!");
    }

    public boolean canExecuteInWorld(JavaPlugin plugin, World world) {
        // Get a reference to your plugin's configuration.
        // How you do this will likely be different based on your plugin's structure.
        FileConfiguration config = plugin.getConfig();

        // Load the enabled worlds from the config into a list.
        List<String> enabledWorlds = config.getStringList("enabled-worlds");

        // Check if the current world's name is in the list of enabled worlds.
        return enabledWorlds.contains(world.getName());
    }

    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key, Object... args) {
        sendMessage(plugin, plugin.getConfig(), sender, key, args);
    }
    
    /**
     * Send a message from a specific configuration file
     * @param plugin The plugin instance
     * @param config The configuration to read from
     * @param sender The command sender
     * @param key The message key
     * @param args The replacement arguments in pairs (placeholder, value)
     */
    public void sendMessage(JavaPlugin plugin, FileConfiguration config, CommandSender sender, String key, Object... args) {
        // Retrieve the message from the configuration
        String message = config.getString("messages." + key);
        String prefix = config.getString("messages.prefix", "");

        if (message != null) {
            // Replace placeholders {name} with args
            if (args != null && args.length > 0) {
                // Handle pairs of placeholder and value
                for (int i = 0; i < args.length; i += 2) {
                    if (i + 1 < args.length) {
                        String placeholder = "{" + args[i] + "}";
                        String value = String.valueOf(args[i + 1]);
                        message = message.replace(placeholder, value);
                    }
                }
            }

            // Translate color codes
            message = ChatColor.translateAlternateColorCodes('&', message);
            prefix = ChatColor.translateAlternateColorCodes('&', prefix);

            sender.sendMessage(prefix + message);
        } else {
            key = ChatColor.translateAlternateColorCodes('&', key);
            prefix = ChatColor.translateAlternateColorCodes('&', prefix);

            sender.sendMessage(prefix + key);
        }
    }

    public void sendActionBar(JavaPlugin plugin, Player player, String key, Object... args) {
        // Retrieve the message from the configuration
        String message = plugin.getConfig().getString("messages." + key);
        if (message != null) {
            // Format the message with the provided arguments
            message = String.format(message, args);

            // Translate color codes
            message = ChatColor.translateAlternateColorCodes('&', message);

            // Send the action bar message
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } else {
            key = ChatColor.translateAlternateColorCodes('&', key);
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(key));
        }
    }

    public void sendTitle(JavaPlugin plugin, Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut, Object... args) {
        // Retrieve the title and subtitle from the configuration
        String title = plugin.getConfig().getString("messages." + titleKey);
        String subtitle = plugin.getConfig().getString("messages." + subtitleKey);

        if (title != null) {
            // Format the title with the provided arguments
            title = String.format(title, args);

            // Translate color codes
            title = ChatColor.translateAlternateColorCodes('&', title);
        } else {
            title = ChatColor.translateAlternateColorCodes('&', titleKey);
        }

        if (subtitle != null) {
            // Format the subtitle with the provided arguments
            subtitle = String.format(subtitle, args);

            // Translate color codes
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
        } else {
            subtitle = ChatColor.translateAlternateColorCodes('&', subtitleKey);
        }

        // Send the title and subtitle
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    public void logSevere(JavaPlugin plugin, String message) {
        if (plugin.getConfig().getInt("debug") >= 0) {
            plugin.getLogger().severe("Critical Error: " + message);
        }
    }

    public void logWarn(JavaPlugin plugin, String message) {
        if (plugin.getConfig().getInt("debug") >= 1) {
            plugin.getLogger().warning("Warning: " + message);
        }
    }
    
    public void logInfo(JavaPlugin plugin, String message) {
        if (plugin.getConfig().getInt("debug") >= 2) {
            plugin.getLogger().info("Info: " + message);
        }
    }

    public void logDebug(JavaPlugin plugin, String message) {
        if (plugin.getConfig().getInt("debug") >= 3) {
            plugin.getLogger().info("Debug: " + message);
        }
    }
    
    public void logDebugPlayer(JavaPlugin plugin, Player player, String message) {
        if (plugin.getConfig().getInt("debug") >= 2) {
            sendMessage(plugin ,player, message);
        }
    }

    public void updateConfig(JavaPlugin plugin) {
        plugin.reloadConfig();

        ConfigUpdateOptions options = ConfigUpdateOptions.builder()
                .fileName("config.yml")
                .resourcePath("config.yml")
                .versionPath("version")
                .reloadAction(file -> plugin.reloadConfig())
                .resetAction(file -> plugin.saveDefaultConfig())
                .skipMergeIfVersionMatches(true)
                .build();

        updateConfig(plugin, options);
    }

    public void updateConfig(JavaPlugin plugin, ConfigUpdateOptions options) {
        logDebug(plugin, "Starting config update for " + options.fileName());
        ConfigUpdateService.update(plugin, options);
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }
    
    public ItemLibrary getItemLibrary() {
        return itemLibrary;
    }
    
    public GUIManager getGUIManager() {
        return guiManager;
    }
    
    /**
     * Convenience method to create a GUI for a plugin
     * @param plugin The plugin creating the GUI
     * @param guiType The GUI type to create
     * @return A GUIBuilder instance for the specified GUI type
     */
    public org.yusaki.lib.gui.GUIBuilder createGUI(JavaPlugin plugin, String guiType) {
        if (guiManager == null) {
            throw new IllegalStateException("GUI Manager is not enabled. Enable it in YskLib config.yml");
        }
        return guiManager.createGUI(plugin, guiType);
    }
    
    /**
     * Load GUI configurations for a plugin
     * @param plugin The plugin to load GUI configurations for
     */
    public void loadGUIConfigurations(JavaPlugin plugin) {
        if (guiManager != null) {
            guiManager.loadConfigurations(plugin);
        }
    }
}
