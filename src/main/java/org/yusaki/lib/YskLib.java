package org.yusaki.lib;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mineacademy.fo.plugin.SimplePlugin;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

public final class YskLib extends SimplePlugin {
    
    @Override
    public void onPluginStart() {
        getLogger().info("YskLib enabled!");

    }

    @Override
    public void onPluginStop() {
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

    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key , Object... args) {
        // Retrieve the message from the configuration
        String message = plugin.getConfig().getString("messages." + key);
        String prefix = plugin.getConfig().getString("messages.prefix");
        if (prefix == null) {
            prefix = "";
        }
        if (message != null) {
            // Format the message with the provided arguments
            message = String.format(message, args);

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
            plugin.getLogger().fine("Debug: " + message);
        }
    }
    
    public void logDebugPlayer(JavaPlugin plugin, Player player, String message) {
        if (plugin.getConfig().getInt("debug") >= 2) {
            sendMessage(plugin ,player, message);
        }
    }

    public void updateConfig(JavaPlugin plugin) {
        // Reload the plugin's configuration
        plugin.reloadConfig();

        // Load the default configuration from the JAR file
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource("config.yml"))));

        // Get the version of the default configuration
        double defaultVersion = defaultConfig.getDouble("version");
        logDebug(plugin, "Default config version: " + defaultVersion);

        // Get the version of the current configuration
        double currentVersion = plugin.getConfig().getDouble("version");
        logDebug(plugin, "Current config version: " + currentVersion);

        // If the default configuration is newer
        if (defaultVersion > currentVersion) {
            logInfo(plugin, "Config version mismatch, updating config file...");

            // Update the current configuration with missing keys from the default configuration
            for (String key : defaultConfig.getKeys(true)) {
                logDebug(plugin, "Checking key: " + key);
                if (!plugin.getConfig().isSet(key)) {
                    logDebug(plugin, "Missing config, adding new config value: " + key);
                    plugin.getConfig().set(key, defaultConfig.get(key));
                } else {
                    logDebug(plugin, "Config value already exists: " + key);
                }
            }

            // Update the version to the default version
            plugin.getConfig().set("version", defaultVersion);

            // Save the updated configuration
            plugin.saveConfig();
        } else {
            logInfo(plugin, "Config file is up to date.");
        }

        // Reload the configuration to apply changes
        plugin.reloadConfig();
    }
}
