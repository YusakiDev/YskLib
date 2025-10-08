package org.yusaki.lib.modules;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utility to synchronize Bukkit command aliases with values defined in plugin configuration files.
 */
public final class CommandAliasManager {

    private CommandAliasManager() {
    }

    /**
     * Apply aliases to a command using the provided configuration path.
     *
     * @param plugin     owning plugin
     * @param command    plugin command instance
     * @param config     configuration to read aliases from
     * @param aliasPath  path to a string list containing aliases (e.g. {@code settings.command-aliases.base})
     * @return aliases applied to the command (empty when none configured)
     */
    public static List<String> applyAliases(JavaPlugin plugin,
                                            PluginCommand command,
                                            FileConfiguration config,
                                            String aliasPath) {
        if (command == null) {
            plugin.getLogger().warning("Command reference is null; skipping alias application.");
            return Collections.emptyList();
        }

        List<String> aliases = config.getStringList(aliasPath);
        if (aliases == null || aliases.isEmpty()) {
            aliases = Collections.emptyList();
        }

        command.setAliases(aliases);

        SimpleCommandMap commandMap = findCommandMap();
        if (commandMap == null) {
            plugin.getLogger().warning("Unable to access Bukkit command map; aliases may require a server restart to take effect.");
            return aliases;
        }

        try {
            command.unregister(commandMap);
            commandMap.register(plugin.getDescription().getName().toLowerCase(Locale.ROOT), command);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to re-register aliases for /" + command.getName() + ": " + ex.getMessage());
        }

        return aliases;
    }

    /**
     * Apply aliases using a command name rather than an explicit {@link PluginCommand} instance.
     *
     * @param plugin    owning plugin
     * @param command   command name registered in plugin.yml
     * @param config    configuration to read aliases from
     * @param aliasPath path to alias list in the configuration
     * @return aliases applied to the command (empty when none configured)
     */
    public static List<String> applyAliases(JavaPlugin plugin,
                                            String command,
                                            FileConfiguration config,
                                            String aliasPath) {
        PluginCommand pluginCommand = plugin.getCommand(command);
        if (pluginCommand == null) {
            plugin.getLogger().warning("Command '/" + command + "' not found; cannot apply aliases.");
            return Collections.emptyList();
        }
        return applyAliases(plugin, pluginCommand, config, aliasPath);
    }

    private static SimpleCommandMap findCommandMap() {
        CommandMap map;
        try {
            map = (CommandMap) Bukkit.getServer().getClass()
                    .getMethod("getCommandMap")
                    .invoke(Bukkit.getServer());
            if (map instanceof SimpleCommandMap simpleCommandMap) {
                return simpleCommandMap;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
        }

        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            map = (CommandMap) field.get(Bukkit.getServer());
            if (map instanceof SimpleCommandMap simpleCommandMap) {
                return simpleCommandMap;
            }
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
        }

        return null;
    }
}
