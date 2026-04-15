package org.yusaki.lib;

import com.tcoded.folialib.FoliaLib;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.config.ConfigUpdateOptions;
import org.yusaki.lib.config.ConfigUpdateService;
import org.yusaki.lib.gui.GUIManager;
import org.yusaki.lib.modules.ItemLibrary;
import org.yusaki.lib.modules.MessageManager;
import org.yusaki.lib.modules.ItemEditManager;
import org.yusaki.lib.modules.CustomItemManager;
import org.yusaki.lib.modules.SoundHelper;
import org.yusaki.lib.text.ColorHelper;

import io.sentry.Sentry;
import org.bukkit.command.Command;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class YskLib extends JavaPlugin {
    private FoliaLib foliaLib;
    private ItemLibrary itemLibrary;
    private GUIManager guiManager;
    private MessageManager messageManager;
    private ItemEditManager itemEditManager;
    private CustomItemManager customItemManager;
    private SoundHelper soundHelper;
    private final Map<String, PluginInfo> sentryRegistry = new ConcurrentHashMap<>();
    private record PluginInfo(String name, String version, boolean consent) {}

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

        // Initialize MessageManager
        messageManager = new MessageManager(this);
        getLogger().info("MessageManager module enabled!");

        // Initialize ItemEditManager (optional, soft dependency)
        if (getConfig().getBoolean("modules.itemedit.enabled", true)) {
            itemEditManager = new ItemEditManager(this);
            if (itemEditManager.isInitialized()) {
                getLogger().info("ItemEdit integration module enabled!");
            }
        }

        // Initialize CustomItemManager (unified item API)
        if (getConfig().getBoolean("modules.custom-items.enabled", true)) {
            customItemManager = new CustomItemManager(this);
            getLogger().info("CustomItemManager module enabled!");
        }

        // Initialize SoundHelper
        if (getConfig().getBoolean("modules.sounds.enabled", true)) {
            soundHelper = new SoundHelper(this);
            getLogger().info("SoundHelper module enabled!");
        }

        initSentry();
        getLogger().info("YskLib enabled!");
    }

    private void initSentry() {
        try {
            Sentry.init(options -> {
                options.setDsn("https://2a89e45e9b904a80b0745f934bdb9ef0@glitchtip.yusakidev.com/3");
                options.setEnvironment("production");
                options.setTag("plugin.family", "folia");
                options.setRelease(getDescription().getVersion());

                // Server context
                options.setTag("server.software", getServer().getName());
                options.setTag("server.version", getServer().getVersion());
                options.setTag("server.bukkit_version", getServer().getBukkitVersion());
                options.setTag("server.online_mode", String.valueOf(getServer().getOnlineMode()));
                options.setTag("server.max_players", String.valueOf(getServer().getMaxPlayers()));

                // JVM context
                options.setTag("jvm.version", System.getProperty("java.version"));
                options.setTag("jvm.vendor", System.getProperty("java.vendor"));
                options.setTag("jvm.vm_name", System.getProperty("java.vm.name"));

                // OS context
                options.setTag("os.name", System.getProperty("os.name"));
                options.setTag("os.version", System.getProperty("os.version"));
                options.setTag("os.arch", System.getProperty("os.arch"));

                // Memory
                Runtime rt = Runtime.getRuntime();
                options.setTag("memory.max_mb", String.valueOf(rt.maxMemory() / 1024 / 1024));
                options.setTag("memory.processors", String.valueOf(rt.availableProcessors()));

                // JVM flags
                java.lang.management.RuntimeMXBean runtimeBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                options.setTag("jvm.flags", String.join(" ", runtimeBean.getInputArguments()));
            });
            getLogger().info("Sentry error reporting initialized");
        } catch (Exception e) {
            getLogger().warning("Failed to initialize Sentry: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        Sentry.close();
        getLogger().info("YskLib disabled!");
    }

    public void registerPlugin(JavaPlugin plugin) {
        boolean consent = plugin.getConfig().getBoolean("error-reporting", true);
        sentryRegistry.put(plugin.getName(), new PluginInfo(
            plugin.getName(), plugin.getDescription().getVersion(), consent
        ));
    }

    public void captureException(String pluginName, Throwable throwable) {
        PluginInfo info = sentryRegistry.get(pluginName);
        if (info == null || !info.consent()) return;
        Sentry.withScope(scope -> {
            scope.setTag("plugin.name", info.name());
            scope.setTag("plugin.version", info.version());
            scope.setTag("plugin.family", "folia");
            Sentry.captureException(throwable);
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("ysklib")) return false;
        if (args.length == 1 && args[0].equalsIgnoreCase("test-sentry")) {
            try {
                throw new RuntimeException("YskLib test exception - verifying Sentry integration");
            } catch (Exception e) {
                Sentry.withScope(scope -> {
                    scope.setTag("plugin.name", "YskLib");
                    scope.setTag("plugin.version", getDescription().getVersion());
                    scope.setTag("plugin.family", "folia");
                    Sentry.captureException(e);
                });
                sender.sendMessage("Test exception sent to GlitchTip!");
                getLogger().info("Test exception sent to Sentry/GlitchTip");
            }
            return true;
        }
        sender.sendMessage("Usage: /ysklib test-sentry");
        return true;
    }

    public boolean canExecuteInWorld(JavaPlugin plugin, World world) {
        // Get a reference to your plugin's configuration.
        // How you do this will likely be different based on your plugin's structure.
        FileConfiguration config = plugin.getConfig();

        // Load the enabled worlds from the config into a list.
        List<String> enabledWorlds = config.getStringList("enabled-worlds");

        // Check if wildcard is present (enables all worlds)
        if (enabledWorlds.contains("*")) {
            return true;
        }

        // Check if the current world's name is in the list of enabled worlds.
        return enabledWorlds.contains(world.getName());
    }

    /**
     * @deprecated Use messageManager.sendMessage() or the new enhanced messaging methods instead
     */
    @Deprecated
    public void sendMessage(JavaPlugin plugin, CommandSender sender, String key, Object... args) {
        sendMessage(plugin, plugin.getConfig(), sender, key, args);
    }

    /**
     * Send a message from a specific configuration file
     * @deprecated Use messageManager.sendMessage() instead
     * @param plugin The plugin instance
     * @param config The configuration to read from
     * @param sender The command sender
     * @param key The message key
     * @param args The replacement arguments in pairs (placeholder, value)
     */
    @Deprecated
    public void sendMessage(JavaPlugin plugin, FileConfiguration config, CommandSender sender, String key, Object... args) {
        // Convert args to Map for new MessageManager
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    placeholders.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
                }
            }
        }

        // Use new MessageManager
        messageManager.sendMessage(plugin, sender, key, placeholders);
    }

    /**
     * @deprecated Use messageManager.sendActionBar() instead
     */
    @Deprecated
    public void sendActionBar(JavaPlugin plugin, Player player, String key, Object... args) {
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    placeholders.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
                }
            }
        }
        messageManager.sendActionBar(plugin, player, key, placeholders);
    }

    /**
     * @deprecated Use messageManager.sendTitle() instead
     */
    @Deprecated
    public void sendTitle(JavaPlugin plugin, Player player, String titleKey, String subtitleKey, int fadeIn, int stay, int fadeOut, Object... args) {
        Map<String, String> placeholders = MessageManager.createPlaceholders();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i += 2) {
                if (i + 1 < args.length) {
                    placeholders.put(String.valueOf(args[i]), String.valueOf(args[i + 1]));
                }
            }
        }
        messageManager.sendTitle(plugin, player, titleKey, subtitleKey, fadeIn, stay, fadeOut, placeholders);
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

    public Component colorizeComponent(String input) {
        return ColorHelper.toComponent(input);
    }

    public String colorizeLegacy(String input) {
        return ColorHelper.toLegacy(input);
    }

    public String colorizeLegacy(Component component) {
        return ColorHelper.toLegacy(component);
    }

    public String colorizePlain(String input) {
        return ColorHelper.toPlain(input);
    }

    public String colorizePlain(Component component) {
        return ColorHelper.toPlain(component);
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

    public MessageManager getMessageManager() {
        return messageManager;
    }

    /**
     * Load messages for a plugin (should be called in onEnable)
     * @param plugin The plugin to load messages for
     */
    public void loadMessages(JavaPlugin plugin) {
        if (messageManager != null) {
            messageManager.loadMessages(plugin);
        }
    }
    
    /**
     * Load messages from a custom configuration file
     * @param plugin The plugin to load messages for
     * @param config The configuration file to load from
     * @param sectionPath The path to messages section (e.g., "messages")
     */
    public void loadMessages(JavaPlugin plugin, org.bukkit.configuration.file.FileConfiguration config, String sectionPath) {
        if (messageManager != null) {
            messageManager.loadMessages(plugin, config, sectionPath);
        }
    }
    
    /**
     * Load messages from a custom configuration file with merge option
     * @param plugin The plugin to load messages for
     * @param config The configuration file to load from
     * @param sectionPath The path to messages section (e.g., "messages")
     * @param clearExisting If true, clear existing messages; if false, merge with existing
     */
    public void loadMessages(JavaPlugin plugin, org.bukkit.configuration.file.FileConfiguration config, String sectionPath, boolean clearExisting) {
        if (messageManager != null) {
            messageManager.loadMessages(plugin, config, sectionPath, clearExisting);
        }
    }
    
    /**
     * Get the ItemEditManager
     * @return ItemEditManager instance or null if not initialized
     */
    public ItemEditManager getItemEditManager() {
        return itemEditManager;
    }
    
    /**
     * Get the CustomItemManager for unified item handling
     * @return CustomItemManager instance or null if not initialized
     */
    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    /**
     * Get the SoundHelper for playing sounds
     * @return SoundHelper instance or null if not initialized
     */
    public SoundHelper getSoundHelper() {
        return soundHelper;
    }
}
