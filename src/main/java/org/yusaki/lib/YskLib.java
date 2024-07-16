package org.yusaki.lib;

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

    public void logDebug(JavaPlugin plugin, String message) {
        if (plugin.getConfig().getBoolean("debug")) {
            getLogger().info(message);
        }
    }

    public void logDebugPlayer(JavaPlugin plugin, Player player, String message) {
        if (plugin.getConfig().getBoolean("debug")) {
            sendMessage(plugin ,player, message);
        }
    }

    public void updateConfig(JavaPlugin plugin) {
        plugin.reloadConfig();
        // Load the default configuration from the JAR file
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource("config.yml"))));

        // Get the version of the default configuration
        double defaultVersion = defaultConfig.getDouble("version");
        logDebug(plugin, "Plugin config version: " + defaultVersion);


        // Get the version of the configuration on the file system
        double currentVersion = plugin.getConfig().getDouble("version");
        logDebug(plugin,"Current config version: " + currentVersion);
        // If the default configuration is newer
        if (defaultVersion > currentVersion) {

            logDebug(plugin, "Config Version Mismatched, Updating config file...");

            for (String key : defaultConfig.getKeys(true)) {
                logDebug(plugin ,"Checking key: " + key);
                if (!getConfig().isSet(key)) {
                    logDebug(plugin,"Missing Config, Adding new config value: " + key);

                    getConfig().set(key, defaultConfig.get(key));
                } else {
                    logDebug(plugin,"Config value already exists: " + key);
                }
                // change the version to the default version
                getConfig().set("version", defaultVersion);
            }
            // Save the configuration file
            plugin.saveConfig();
        }
        else {
            logDebug(plugin,"Config file is up to date.");

        }

        // Reload the configuration file to get any changes
        plugin.reloadConfig();
    }
}
