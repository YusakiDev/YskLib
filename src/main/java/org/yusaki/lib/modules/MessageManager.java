package org.yusaki.lib.modules;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;
import org.yusaki.lib.text.ColorHelper;

import java.time.Duration;
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
    private final Map<String, PluginMessages> moduleMessages; // Key: "pluginName:moduleName"

    public MessageManager(YskLib lib) {
        this.lib = lib;
        this.pluginMessages = new ConcurrentHashMap<>();
        this.moduleMessages = new ConcurrentHashMap<>();
    }

    /**
     * Load messages for a plugin from its config
     */
    public void loadMessages(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        loadMessages(plugin, config, "messages");
    }
    
    /**
     * Load messages for a module with a specific module ID
     * This allows multiple modules within the same plugin to have separate message pools and prefixes
     * @param plugin The plugin instance
     * @param moduleId Unique identifier for the module (e.g., "portal", "weather")
     * @param config The configuration to load from
     * @param sectionPath The path to the messages section (e.g., "messages")
     */
    public void loadModuleMessages(JavaPlugin plugin, String moduleId, FileConfiguration config, String sectionPath) {
        String moduleKey = plugin.getName() + ":" + moduleId;
        ConfigurationSection messagesSection = config.getConfigurationSection(sectionPath);

        PluginMessages messages = moduleMessages.computeIfAbsent(moduleKey, k -> new PluginMessages());
        
        // Always clear for module messages
        messages.singleMessages.clear();
        messages.multiMessages.clear();
        messages.resetPrefix();

        if (messagesSection == null) {
            lib.logWarn(plugin, "No messages section found at '" + sectionPath + "' for module '" + moduleId + "'!");
            return;
        }

        // Load prefix FIRST before other messages
        if (messagesSection.contains("prefix") && messagesSection.isString("prefix")) {
            String prefixValue = messagesSection.getString("prefix");
            if (prefixValue != null) {
                messages.updatePrefix(prefixValue);
                lib.logDebug(plugin, "Updated prefix for module '" + moduleId + "' from " + sectionPath);
            }
        }

        int singleCount = 0;
        int multiCount = 0;
        
        for (String key : messagesSection.getKeys(false)) {
            // Skip prefix since we already loaded it
            if ("prefix".equalsIgnoreCase(key)) {
                continue;
            }
            
            Object value = messagesSection.get(key);

            if (value instanceof List<?> list) {
                List<String> messageList = list.stream()
                        .map(item -> item == null ? "" : item.toString())
                        .toList();
                messages.multiMessages.put(key, messageList);
                multiCount++;
            } else if (value instanceof String str) {
                messages.singleMessages.put(key, str);
                singleCount++;
            }
        }

        lib.logDebug(plugin, "Loaded " + singleCount + " single and " + multiCount + 
                     " multi-line messages for module '" + moduleId + "' from " + sectionPath);
    }
    
    /**
     * Load messages for a plugin from a custom configuration file
     * @param plugin The plugin instance
     * @param config The configuration to load from
     * @param sectionPath The path to the messages section (e.g., "messages")
     */
    public void loadMessages(JavaPlugin plugin, FileConfiguration config, String sectionPath) {
        loadMessages(plugin, config, sectionPath, true);
    }
    
    /**
     * Load messages for a plugin from a custom configuration file
     * @param plugin The plugin instance
     * @param config The configuration to load from
     * @param sectionPath The path to the messages section (e.g., "messages")
     * @param clearExisting If true, clear existing messages before loading; if false, merge with existing
     */
    public void loadMessages(JavaPlugin plugin, FileConfiguration config, String sectionPath, boolean clearExisting) {
        ConfigurationSection messagesSection = config.getConfigurationSection(sectionPath);

        PluginMessages messages = pluginMessages.computeIfAbsent(plugin, k -> new PluginMessages());
        
        // Only clear if requested (allows accumulating messages from multiple sources)
        if (clearExisting) {
            messages.singleMessages.clear();
            messages.multiMessages.clear();
            messages.resetPrefix();
        }

        if (messagesSection == null) {
            lib.logWarn(plugin, "No messages section found at '" + sectionPath + "'! Using default messages.");
            return;
        }

        // Load prefix FIRST before other messages
        if (messagesSection.contains("prefix") && messagesSection.isString("prefix")) {
            String prefixValue = messagesSection.getString("prefix");
            if (prefixValue != null) {
                messages.updatePrefix(prefixValue);
                lib.logDebug(plugin, "Updated prefix from " + sectionPath);
            }
        }

        int singleCount = 0;
        int multiCount = 0;
        
        for (String key : messagesSection.getKeys(false)) {
            // Skip prefix since we already loaded it
            if ("prefix".equalsIgnoreCase(key)) {
                continue;
            }
            
            Object value = messagesSection.get(key);

            if (value instanceof List<?> list) {
                List<String> messageList = list.stream()
                        .map(item -> item == null ? "" : item.toString())
                        .toList();
                messages.multiMessages.put(key, messageList);
                multiCount++;
            } else if (value instanceof String str) {
                messages.singleMessages.put(key, str);
                singleCount++;
            }
        }

        lib.logDebug(plugin, "Loaded " + singleCount + " single and " + multiCount + 
                     " multi-line messages from " + sectionPath + (clearExisting ? " (cleared)" : " (merged)"));
    }

    /**
     * Get a single-line message with placeholders
     */
    public String getMessage(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return resolveMessage(plugin, key, placeholders).legacy();
    }

    /**
     * Get a single-line message as a component with placeholders.
     */
    public Component getMessageComponent(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return resolveMessage(plugin, key, placeholders).component();
    }

    /**
     * Get a single-line message without placeholders
     */
    public String getMessage(JavaPlugin plugin, String key) {
        return getMessage(plugin, key, new HashMap<>());
    }

    /**
     * Get a single-line message component without placeholders.
     */
    public Component getMessageComponent(JavaPlugin plugin, String key) {
        return getMessageComponent(plugin, key, new HashMap<>());
    }

    /**
     * Get a multi-line message list with placeholders
     */
    public List<String> getMessageList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return resolveMessageList(plugin, key, placeholders).stream()
                .map(NormalizedMessage::legacy)
                .toList();
    }

    /**
     * Get a multi-line message component list with placeholders.
     */
    public List<Component> getMessageComponentList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return resolveMessageList(plugin, key, placeholders).stream()
                .map(NormalizedMessage::component)
                .toList();
    }

    /**
     * Get a multi-line message list without placeholders
     */
    public List<String> getMessageList(JavaPlugin plugin, String key) {
        return getMessageList(plugin, key, new HashMap<>());
    }

    /**
     * Get a multi-line message component list without placeholders.
     */
    public List<Component> getMessageComponentList(JavaPlugin plugin, String key) {
        return getMessageComponentList(plugin, key, new HashMap<>());
    }

    /**
     * Replace placeholders in a message
     * Supports both {placeholder} and %placeholder% formats for backward compatibility
     */
    private String replacePlaceholders(String message, Map<String, String> placeholders) {
        String result = message == null ? "" : message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            // Support both formats: {key} and %key%
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    /**
     * Send a single-line message to a sender
     */
    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(resolveMessage(plugin, key, placeholders).component());
    }

    /**
     * Send a single-line message to a sender with the configured prefix applied.
     */
    public void sendPrefixedMessage(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(applyPrefix(plugin, resolveMessage(plugin, key, placeholders)).component());
    }
    
    /**
     * Send a module-specific single-line message with prefix
     * @param plugin The plugin instance
     * @param moduleId The module identifier
     * @param sender The command sender
     * @param key The message key
     * @param placeholders The placeholders to replace
     */
    public void sendModulePrefixedMessage(JavaPlugin plugin, String moduleId, CommandSender sender, String key, Map<String, String> placeholders) {
        String moduleKey = plugin.getName() + ":" + moduleId;
        sender.sendMessage(applyModulePrefix(moduleKey, resolveModuleMessage(moduleKey, key, placeholders)).component());
    }

    /**
     * Send a single-line message to a sender without placeholders
     */
    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key) {
        sendMessage(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send a single-line message with prefix without placeholders.
     */
    public void sendPrefixedMessage(JavaPlugin plugin, CommandSender sender, String key) {
        sendPrefixedMessage(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send a multi-line message to a sender
     */
    public void sendMessageList(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        resolveMessageList(plugin, key, placeholders)
                .forEach(message -> sender.sendMessage(message.component()));
    }

    /**
     * Send a multi-line message to a sender with the prefix applied to each line.
     */
    public void sendPrefixedMessageList(JavaPlugin plugin, CommandSender sender, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        resolveMessageList(plugin, key, placeholders).stream()
                .map(normalized -> applyPrefix(messages, normalized))
                .forEach(normalized -> sender.sendMessage(normalized.component()));
    }

    /**
     * Send a multi-line message to a sender without placeholders
     */
    public void sendMessageList(JavaPlugin plugin, CommandSender sender, String key) {
        sendMessageList(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send a multi-line message with prefix without placeholders.
     */
    public void sendPrefixedMessageList(JavaPlugin plugin, CommandSender sender, String key) {
        sendPrefixedMessageList(plugin, sender, key, new HashMap<>());
    }

    /**
     * Send action bar message to player
     */
    public void sendActionBar(JavaPlugin plugin, Player player, String key, Map<String, String> placeholders) {
        player.sendActionBar(resolveMessage(plugin, key, placeholders).component());
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
        NormalizedMessage title = resolveMessage(plugin, titleKey, placeholders);
        NormalizedMessage subtitle = resolveMessage(plugin, subtitleKey, placeholders);
        Title.Times times = Title.Times.times(ticksToDuration(fadeIn), ticksToDuration(stay), ticksToDuration(fadeOut));
        player.showTitle(Title.title(title.component(), subtitle.component(), times));
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
        volatile Component prefixComponent = Component.empty();
        volatile String legacyPrefix = "";
        volatile String plainPrefix = "";

        void resetPrefix() {
            updatePrefix("");
        }

        void updatePrefix(String rawPrefix) {
            if (rawPrefix == null || rawPrefix.isEmpty()) {
                prefixComponent = Component.empty();
                legacyPrefix = "";
                plainPrefix = "";
                return;
            }

            Component component = ColorHelper.toComponent(rawPrefix);
            prefixComponent = component;
            legacyPrefix = ColorHelper.toLegacy(component);
            plainPrefix = ColorHelper.toPlain(component);
        }
    }

    private record NormalizedMessage(Component component, String legacy, String plain) {
    }

    /**
     * Retrieve the configured prefix for a plugin.
     */
    public String getPrefix(JavaPlugin plugin) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return "";
        }
        return messages.legacyPrefix;
    }

    /**
     * Retrieve the configured prefix as a component for a plugin.
     */
    public Component getPrefixComponent(JavaPlugin plugin) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return Component.empty();
        }
        return messages.prefixComponent;
    }

    /**
     * Get a single-line message with prefix applied.
     */
    public String getPrefixedMessage(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return applyPrefix(plugin, resolveMessage(plugin, key, placeholders)).legacy();
    }

    public String getPrefixedMessage(JavaPlugin plugin, String key) {
        return getPrefixedMessage(plugin, key, new HashMap<>());
    }

    /**
     * Get a single-line message component with prefix applied.
     */
    public Component getPrefixedMessageComponent(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        return applyPrefix(plugin, resolveMessage(plugin, key, placeholders)).component();
    }

    public Component getPrefixedMessageComponent(JavaPlugin plugin, String key) {
        return getPrefixedMessageComponent(plugin, key, new HashMap<>());
    }

    /**
     * Get a multi-line message list with prefix applied to each line.
     */
    public List<String> getPrefixedMessageList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        return resolveMessageList(plugin, key, placeholders).stream()
                .map(normalized -> applyPrefix(messages, normalized).legacy())
                .toList();
    }

    public List<String> getPrefixedMessageList(JavaPlugin plugin, String key) {
        return getPrefixedMessageList(plugin, key, new HashMap<>());
    }

    /**
     * Get a multi-line message component list with prefix applied to each line.
     */
    public List<Component> getPrefixedMessageComponentList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        return resolveMessageList(plugin, key, placeholders).stream()
                .map(normalized -> applyPrefix(messages, normalized).component())
                .toList();
    }

    public List<Component> getPrefixedMessageComponentList(JavaPlugin plugin, String key) {
        return getPrefixedMessageComponentList(plugin, key, new HashMap<>());
    }

    private NormalizedMessage resolveMessage(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return errorMessage("Messages not loaded for " + plugin.getName());
        }

        String lookupKey = sanitizeKey(key);
        String raw = messages.singleMessages.get(lookupKey);
        if (raw == null) {
            return errorMessage("Message not found: " + key);
        }

        return normalise(raw, placeholders);
    }
    
    private NormalizedMessage resolveModuleMessage(String moduleKey, String key, Map<String, String> placeholders) {
        PluginMessages messages = moduleMessages.get(moduleKey);
        if (messages == null) {
            return errorMessage("Module messages not loaded for " + moduleKey);
        }

        String lookupKey = sanitizeKey(key);
        String raw = messages.singleMessages.get(lookupKey);
        if (raw == null) {
            return errorMessage("Module message not found: " + key + " in " + moduleKey);
        }

        return normalise(raw, placeholders);
    }

    private List<NormalizedMessage> resolveMessageList(JavaPlugin plugin, String key, Map<String, String> placeholders) {
        PluginMessages messages = pluginMessages.get(plugin);
        if (messages == null) {
            return List.of(errorMessage("Messages not loaded for " + plugin.getName()));
        }

        String lookupKey = sanitizeKey(key);
        List<String> rawList = messages.multiMessages.get(lookupKey);
        if (rawList == null) {
            return List.of(errorMessage("Message list not found: " + key));
        }

        return rawList.stream()
                .map(raw -> normalise(raw, placeholders))
                .toList();
    }

    private String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        if (key.startsWith("messages.")) {
            return key.substring("messages.".length());
        }
        return key;
    }

    private NormalizedMessage normalise(String raw, Map<String, String> placeholders) {
        String resolved = replacePlaceholders(raw, placeholders);
        Component component = ColorHelper.toComponent(resolved);
        return new NormalizedMessage(component, ColorHelper.toLegacy(component), ColorHelper.toPlain(component));
    }

    private NormalizedMessage errorMessage(String reason) {
        Component component = ColorHelper.toComponent("<red>" + reason);
        return new NormalizedMessage(component, ColorHelper.toLegacy(component), ColorHelper.toPlain(component));
    }

    private NormalizedMessage applyPrefix(JavaPlugin plugin, NormalizedMessage message) {
        return applyPrefix(pluginMessages.get(plugin), message);
    }
    
    private NormalizedMessage applyModulePrefix(String moduleKey, NormalizedMessage message) {
        return applyPrefix(moduleMessages.get(moduleKey), message);
    }

    private NormalizedMessage applyPrefix(PluginMessages messages, NormalizedMessage message) {
        if (messages == null || message == null) {
            return message;
        }
        if (messages.legacyPrefix.isEmpty()) {
            return message;
        }
        if (!messages.plainPrefix.isEmpty() && message.plain().startsWith(messages.plainPrefix)) {
            return message;
        }

        Component component = messages.prefixComponent.append(message.component());
        String legacy = messages.legacyPrefix + message.legacy();
        String plain = messages.plainPrefix + message.plain();
        return new NormalizedMessage(component, legacy, plain);
    }

    private static Duration ticksToDuration(int ticks) {
        if (ticks <= 0) {
            return Duration.ZERO;
        }
        return Duration.ofMillis((long) ticks * 50L);
    }
}
