package org.yusaki.lib.modules;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced message manager with caching and multi-line support
 */
public class MessageManager {
    private final YskLib lib;
    private final Map<JavaPlugin, PluginMessages> pluginMessages;

    public MessageManager(YskLib lib) {
        this.lib = lib;
        this.pluginMessages = new ConcurrentHashMap<>();
    }

    /**
     * Load messages for a plugin from its config
     */
    public void loadMessages(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");

        PluginMessages messages = pluginMessages.computeIfAbsent(plugin, k -> new PluginMessages());
        messages.singleMessages.clear();
        messages.multiMessages.clear();

        if (messagesSection == null) {
            lib.logWarn(plugin, "No messages section found in config.yml! Using default messages.");
            return;
        }

        for (String key : messagesSection.getKeys(false)) {
            Object value = messagesSection.get(key);

            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> messageList = (List<String>) value;
                messages.multiMessages.put(key, messageList.stream()
                        .map(msg -> ChatColor.translateAlternateColorCodes('&', msg))
                        .toList());
            } else if (value instanceof String) {
                String message = ChatColor.translateAlternateColorCodes('&', (String) value);
                messages.singleMessages.put(key, message);
            }
        }

        lib.logDebug(plugin, "Loaded " + messages.singleMessages.size() + " single messages and " +
                     messages.multiMessages.size() + " multi-line messages");
    }

    /**
     * Get a single-line message with placeholders
     */
    public String getMessage(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return ChatColor.RED + "Messages not loaded for " + plugin.getName();
        }

        String message = messages.singleMessages.get(key);
        if (message == null) {
            return ChatColor.RED + "Message not found: " + key;
        }

        return replacePlaceholders(message, placeholders);
    }

    /**
     * Get a single-line message without placeholders
     */
    public String getMessage(JavaPlugin plugin, String key) {
        return getMessage(plugin, key, new HashMap<>());
    }

    /**
     * Get a multi-line message list with placeholders
     */
    public List<String> getMessageList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return List.of(ChatColor.RED + "Messages not loaded for " + plugin.getName());
        }

        List<String> messageList = messages.multiMessages.get(key);
        if (messageList == null) {
            return List.of(ChatColor.RED + "Message list not found: " + key);
        }

        return messageList.stream()
                .map(msg -> replacePlaceholders(msg, placeholders))
                .toList();
    }

    /**
     * Get a multi-line message list without placeholders
     */
    public List<String> getMessageList(JavaPlugin plugin, String key) {
        return getMessageList(plugin, key, new HashMap<>());
    }

    /**
     * Replace placeholders in a message
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }

    /**
     * Send a single-line message to a sender
     */
    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(getMessage(plugin, key, placeholders));
    }

    /**
     * Send a single-line message to a sender without placeholders
     */
    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key) {
        sendMessage(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send a multi-line message to a sender
     */
    public void sendMessageList(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        List<String> messages = getMessageList(plugin, key, placeholders);
        for (String message : messages) {
            sender.sendMessage(message);
        }
    }

    /**
     * Send a multi-line message to a sender without placeholders
     */
    public void sendMessageList(JavaPlugin plugin, CommandSender sender, String key) {
        sendMessageList(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send action bar message to player
     */
    public void sendActionBar(JavaPlugin plugin, Player player, String key, Map<String, String> placeholders) {
        String message = getMessage(plugin, key, placeholders);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    /**
     * Send action bar message to player without placeholders
     */
    public void sendActionBar(JavaPlugin plugin, Player player, String key) {
        sendActionBar(plugin, player, key, new HashMap<>());
    }

    /**
     * Send title and subtitle to player
     */
    public void sendTitle(JavaPlugin plugin, Player player, String titleKey, String subtitleKey,
                         int fadeIn, int stay, int fadeOut, Map<String, String> placeholders) {
        String title = getMessage(plugin, titleKey, placeholders);
        String subtitle = getMessage(plugin, subtitleKey, placeholders);
        player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
    }

    /**
     * Create a placeholder map from key-value pairs
     */
    public static Map<String, String> placeholders(String... keyValuePairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            if (i + 1 < keyValuePairs.length) {
                map.put(keyValuePairs[i], keyValuePairs[i + 1]);
            }
        }
        return map;
    }

    /**
     * Create an empty placeholder map
     */
    public static Map<String, String> createPlaceholders() {
        return new HashMap<>();
    }

    /**
     * Clear cached messages for a plugin
     */
    public void clearMessages(JavaPlugin plugin) {
        pluginMessages.remove(plugin);
    }

    /**
     * Storage for a plugin's messages
     */
    private static class PluginMessages {
        final Map<String, String> singleMessages = new ConcurrentHashMap<>();
        final Map<String, List<String>> multiMessages = new ConcurrentHashMap<>();
    }
}
