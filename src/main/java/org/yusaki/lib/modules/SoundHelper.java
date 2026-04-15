package org.yusaki.lib.modules;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sound helper module providing config-based sound loading with caching,
 * preset sounds for common UI feedback, and per-plugin sound pools.
 *
 * Supports two config formats:
 * - Compact: "SOUND_NAME:volume:pitch" (e.g., "ENTITY_PLAYER_LEVELUP:1.0:1.2")
 * - Verbose: YAML map with sound, volume, pitch keys
 */
public class SoundHelper {

    public record SoundEntry(Sound sound, float volume, float pitch) {
        public void play(Player player) {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }

        public void playAt(Location location) {
            if (location.getWorld() != null) {
                location.getWorld().playSound(location, sound, volume, pitch);
            }
        }
    }

    private static class PluginSounds {
        final Map<String, SoundEntry> sounds = new ConcurrentHashMap<>();
        final Map<String, Map<String, SoundEntry>> moduleSounds = new ConcurrentHashMap<>();
    }

    private final YskLib lib;
    private final Map<String, PluginSounds> pluginRegistry = new ConcurrentHashMap<>();

    private SoundEntry presetSuccess;
    private SoundEntry presetError;
    private SoundEntry presetClick;
    private SoundEntry presetDing;

    public SoundHelper(YskLib lib) {
        this.lib = lib;
        loadPresets();
        lib.getLogger().info("SoundHelper initialized");
    }

    private void loadPresets() {
        ConfigurationSection presets = lib.getConfig().getConfigurationSection("modules.sounds.presets");

        presetSuccess = parseSound(presets, "success", new SoundEntry(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f));
        presetError = parseSound(presets, "error", new SoundEntry(Sound.ENTITY_VILLAGER_NO, 1.0f, 0.8f));
        presetClick = parseSound(presets, "click", new SoundEntry(Sound.UI_BUTTON_CLICK, 0.5f, 1.0f));
        presetDing = parseSound(presets, "ding", new SoundEntry(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f));
    }

    private SoundEntry parseSound(ConfigurationSection section, String key, SoundEntry defaultValue) {
        if (section == null || !section.contains(key)) {
            return defaultValue;
        }

        Object value = section.get(key);

        if (value instanceof String str) {
            return parseSoundString(str, defaultValue);
        }

        if (value instanceof ConfigurationSection cs) {
            return parseSoundSection(cs, defaultValue);
        }

        return defaultValue;
    }

    private SoundEntry parseSoundString(String value, SoundEntry defaultValue) {
        String[] parts = value.split(":");
        if (parts.length < 1) {
            return defaultValue;
        }

        try {
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            return new SoundEntry(sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            lib.getLogger().warning("Invalid sound in config: " + value);
            return defaultValue;
        }
    }

    private SoundEntry parseSoundSection(ConfigurationSection section, SoundEntry defaultValue) {
        String soundName = section.getString("sound");
        if (soundName == null) {
            return defaultValue;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            float volume = (float) section.getDouble("volume", 1.0);
            float pitch = (float) section.getDouble("pitch", 1.0);
            return new SoundEntry(sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            lib.getLogger().warning("Invalid sound in config section: " + soundName);
            return defaultValue;
        }
    }

    public void reloadPresets() {
        loadPresets();
    }

    private PluginSounds getOrCreatePluginSounds(String pluginName) {
        return pluginRegistry.computeIfAbsent(pluginName, k -> new PluginSounds());
    }

    public void loadSounds(JavaPlugin plugin, ConfigurationSection soundsSection) {
        if (soundsSection == null) return;

        PluginSounds ps = getOrCreatePluginSounds(plugin.getName());
        ps.sounds.clear();

        for (String key : soundsSection.getKeys(false)) {
            SoundEntry entry = parseSound(soundsSection, key, null);
            if (entry != null) {
                ps.sounds.put(key, entry);
            }
        }

        lib.logDebug(plugin, "Loaded " + ps.sounds.size() + " sounds for " + plugin.getName());
    }

    public void loadModuleSounds(JavaPlugin plugin, String moduleName, ConfigurationSection soundsSection) {
        if (soundsSection == null) return;

        PluginSounds ps = getOrCreatePluginSounds(plugin.getName());
        Map<String, SoundEntry> moduleMap = ps.moduleSounds.computeIfAbsent(moduleName, k -> new ConcurrentHashMap<>());
        moduleMap.clear();

        for (String key : soundsSection.getKeys(false)) {
            SoundEntry entry = parseSound(soundsSection, key, null);
            if (entry != null) {
                moduleMap.put(key, entry);
            }
        }

        lib.logDebug(plugin, "Loaded " + moduleMap.size() + " sounds for module " + moduleName);
    }

    public SoundEntry getSound(JavaPlugin plugin, String key) {
        PluginSounds ps = pluginRegistry.get(plugin.getName());
        return ps != null ? ps.sounds.get(key) : null;
    }

    public SoundEntry getModuleSound(JavaPlugin plugin, String moduleName, String key) {
        PluginSounds ps = pluginRegistry.get(plugin.getName());
        if (ps == null) return null;
        Map<String, SoundEntry> moduleMap = ps.moduleSounds.get(moduleName);
        return moduleMap != null ? moduleMap.get(key) : null;
    }

    public void play(JavaPlugin plugin, Player player, String key) {
        SoundEntry entry = getSound(plugin, key);
        if (entry != null) {
            entry.play(player);
        }
    }

    public void playAt(JavaPlugin plugin, Location location, String key) {
        SoundEntry entry = getSound(plugin, key);
        if (entry != null) {
            entry.playAt(location);
        }
    }

    public void playModule(JavaPlugin plugin, String moduleName, Player player, String key) {
        SoundEntry entry = getModuleSound(plugin, moduleName, key);
        if (entry != null) {
            entry.play(player);
        }
    }

    public void playModuleAt(JavaPlugin plugin, String moduleName, Location location, String key) {
        SoundEntry entry = getModuleSound(plugin, moduleName, key);
        if (entry != null) {
            entry.playAt(location);
        }
    }

    public void play(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    public void playAt(Location location, Sound sound, float volume, float pitch) {
        if (location.getWorld() != null) {
            location.getWorld().playSound(location, sound, volume, pitch);
        }
    }

    public void playSuccess(Player player) {
        presetSuccess.play(player);
    }

    public void playError(Player player) {
        presetError.play(player);
    }

    public void playClick(Player player) {
        presetClick.play(player);
    }

    public void playDing(Player player) {
        presetDing.play(player);
    }

    public SoundEntry getPresetSuccess() {
        return presetSuccess;
    }

    public SoundEntry getPresetError() {
        return presetError;
    }

    public SoundEntry getPresetClick() {
        return presetClick;
    }

    public SoundEntry getPresetDing() {
        return presetDing;
    }
}
