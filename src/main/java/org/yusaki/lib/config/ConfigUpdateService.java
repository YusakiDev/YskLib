package org.yusaki.lib.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.error.MarkedYAMLException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Applies comprehensive configuration updates: backups, migrations, merging defaults, and reload hooks.
 */
public final class ConfigUpdateService {
    private ConfigUpdateService() {
    }

    public static void updateAll(JavaPlugin plugin, Collection<ConfigUpdateOptions> optionsCollection) {
        if (optionsCollection == null || optionsCollection.isEmpty()) {
            return;
        }

        Set<File> visited = new HashSet<>();
        for (ConfigUpdateOptions options : optionsCollection) {
            update(plugin, options, visited);
        }
    }

    public static void update(JavaPlugin plugin, ConfigUpdateOptions options) {
        update(plugin, options, new HashSet<>());
    }

    private static void update(JavaPlugin plugin, ConfigUpdateOptions options, Set<File> visited) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(options, "options");

        File directory = resolveDirectory(plugin, options);
        File file = new File(directory, options.fileName());
        ensureParentExists(file);

        if (!visited.add(file)) {
            return; // Prevent processing the same file multiple times in updateAll
        }

        ensureFileExists(plugin, file, options);

        YamlConfiguration defaults = loadDefaults(plugin, options.resourcePath());
        if (defaults == null) {
            plugin.getLogger().warning("Unable to locate default resource for " + options.resourcePath() + ".");
            return;
        }

        YamlConfiguration configuration = loadConfiguration(plugin, file, options, options.backupEnabled());
        if (configuration == null) {
            plugin.getLogger().severe("Skipping update for " + file.getName() + " due to load errors.");
            return;
        }

        double defaultVersion = readVersion(defaults, options.versionPath());
        double currentVersion = readVersion(configuration, options.versionPath());

        boolean migrationsApplied = applyMigrations(plugin, file, configuration, options, currentVersion);
        currentVersion = readVersion(configuration, options.versionPath());

        List<String> ignored = options.ignoredSectionsSupplier().apply(configuration);
        Set<String> ignoredSet = normalizeIgnored(ignored);

        boolean shouldMerge = !options.skipMergeIfVersionMatches()
                || Double.isNaN(defaultVersion)
                || Double.isNaN(currentVersion)
                || defaultVersion > currentVersion;

        boolean merged = false;
        if (shouldMerge) {
            if (options.preserveExistingValues()) {
                merged = surgicalMerge(configuration, defaults, ignoredSet);
            } else {
                merged = mergeMissingDefaults(configuration, defaults, ignoredSet);
            }
        } else {
            plugin.getLogger().info(file.getName() + " is already up to date.");
        }

        // Apply reordering if requested
        boolean reordered = false;
        if (options.reorderToTemplate() && needsReordering(configuration, defaults)) {
            configuration = reorderConfig(configuration, defaults);
            reordered = true;
            plugin.getLogger().info("Reordered " + file.getName() + " to match template structure.");
        }

        boolean versionUpdated = false;
        if (!Double.isNaN(defaultVersion) && defaultVersion != currentVersion) {
            configuration.set(options.versionPath(), defaults.get(options.versionPath()));
            versionUpdated = true;
        }

        boolean changesMade = migrationsApplied || merged || versionUpdated || reordered;

        if (changesMade) {
            try {
                configuration.save(file);
                plugin.getLogger().info("Saved updated configuration: " + file.getName());
            } catch (IOException exception) {
                plugin.getLogger().severe("Failed to save " + file.getName() + ": " + exception.getMessage());
            }
        }

        ConsumerInvoker.accept(options.reloadAction(), file);
    }

    private static File resolveDirectory(JavaPlugin plugin, ConfigUpdateOptions options) {
        File directory = options.directory();
        return directory != null ? directory : plugin.getDataFolder();
    }

    private static void ensureParentExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private static void ensureFileExists(JavaPlugin plugin, File file, ConfigUpdateOptions options) {
        if (file.exists()) {
            return;
        }

        if (options.resetAction() != null) {
            ConsumerInvoker.accept(options.resetAction(), file);
        } else if (options.resourcePath() != null) {
            saveResourceQuietly(plugin, options.resourcePath());
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe("Unable to create file " + file.getAbsolutePath() + ": " + exception.getMessage());
            }
        }
    }

    private static void saveResourceQuietly(JavaPlugin plugin, String resourcePath) {
        try {
            plugin.saveResource(resourcePath, false);
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("Resource " + resourcePath + " not found inside the plugin JAR.");
        }
    }

    private static YamlConfiguration loadDefaults(JavaPlugin plugin, String resourcePath) {
        if (resourcePath == null) {
            return null;
        }

        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                return null;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return YamlConfiguration.loadConfiguration(reader);
            }
        } catch (IOException exception) {
            plugin.getLogger().severe("Failed to read default resource " + resourcePath + ": " + exception.getMessage());
            return null;
        }
    }

    private static YamlConfiguration loadConfiguration(JavaPlugin plugin,
                                                        File file,
                                                        ConfigUpdateOptions options,
                                                        boolean createBackup) {
        File backup = null;
        try {
            if (createBackup && file.exists()) {
                backup = createBackup(file);
                plugin.getLogger().info("Created backup " + backup.getName() + " for " + file.getName());
            }

            YamlConfiguration configuration = new YamlConfiguration();
            configuration.load(file);

            if (backup != null) {
                Files.deleteIfExists(backup.toPath());
            }

            return configuration;
        } catch (IOException | InvalidConfigurationException exception) {
            return handleLoadFailure(plugin, file, options, backup, exception);
        }
    }

    private static YamlConfiguration handleLoadFailure(JavaPlugin plugin,
                                                       File file,
                                                       ConfigUpdateOptions options,
                                                       File backup,
                                                       Exception exception) {
        plugin.getLogger().severe("An error occurred while reading " + file.getName() + ".");
        if (exception instanceof InvalidConfigurationException invalid) {
            Throwable cause = invalid.getCause();
            if (cause instanceof MarkedYAMLException marked) {
                int line = marked.getProblemMark() != null ? marked.getProblemMark().getLine() + 1 : -1;
                if (line > 0) {
                    plugin.getLogger().severe("YAML parsing problem near line " + line + ".");
                }
            }
        }
        plugin.getLogger().severe(exception.getMessage());

        if (backup != null) {
            plugin.getLogger().severe("A backup was left at " + backup.getName() + ".");
        }

        if (options.resetAction() == null) {
            return null;
        }

        if (file.exists() && !file.delete()) {
            plugin.getLogger().severe("Unable to delete invalid file " + file.getAbsolutePath());
        }

        ConsumerInvoker.accept(options.resetAction(), file);
        ensureFileExists(plugin, file, options);
        return loadConfiguration(plugin, file, options, false);
    }

    private static File createBackup(File file) throws IOException {
        String baseName = file.getName();
        String prefix;
        int dot = baseName.lastIndexOf('.');
        if (dot > 0) {
            prefix = baseName.substring(0, dot);
        } else {
            prefix = baseName;
        }
        String time = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.ROOT).format(new Date());
        File backup = new File(file.getParentFile(), prefix + "_" + time + ".bak");
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return backup;
    }

    private static boolean applyMigrations(JavaPlugin plugin,
                                           File file,
                                           FileConfiguration configuration,
                                           ConfigUpdateOptions options,
                                           double currentVersion) {
        List<ConfigMigration> migrations = options.migrations();
        if (migrations.isEmpty()) {
            return false;
        }

        double effectiveVersion = Double.isNaN(currentVersion) ? Double.NEGATIVE_INFINITY : currentVersion;
        boolean hasVersionPath = options.versionPath() != null && !options.versionPath().isEmpty();

        List<ConfigMigration> sorted = new ArrayList<>(migrations);
        sorted.sort(Comparator.comparingDouble(ConfigMigration::targetVersion));

        boolean applied = false;
        for (ConfigMigration migration : sorted) {
            double targetVersion = migration.targetVersion();
            boolean versionAllows = !hasVersionPath
                    || Double.isNaN(targetVersion)
                    || effectiveVersion < targetVersion;

            if (!versionAllows) {
                continue;
            }

            if (!migration.predicate().test(configuration)) {
                continue;
            }

            migration.action().accept(configuration);
            applied = true;

            if (hasVersionPath && !Double.isNaN(targetVersion)) {
                configuration.set(options.versionPath(), targetVersion);
                effectiveVersion = targetVersion;
            }

            String description = migration.description();
            if (description == null || description.isEmpty()) {
                description = "version " + targetVersion;
            }
            plugin.getLogger().info("Applied migration for " + file.getName() + " -> " + description);
        }

        return applied;
    }

    private static Set<String> normalizeIgnored(List<String> ignored) {
        Set<String> set = new HashSet<>();
        if (ignored == null) {
            return set;
        }
        for (String entry : ignored) {
            if (entry == null) {
                continue;
            }
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed);
            }
        }
        return set;
    }

    private static boolean mergeMissingDefaults(FileConfiguration target,
                                                FileConfiguration defaults,
                                                Set<String> ignoredPaths) {
        return mergeSection(defaults, defaults, target, "", ignoredPaths);
    }

    /**
     * Surgical merge - only add missing keys, never modify existing values.
     */
    private static boolean surgicalMerge(FileConfiguration target,
                                        FileConfiguration defaults,
                                        Set<String> ignoredPaths) {
        boolean changed = false;
        for (String key : defaults.getKeys(true)) {
            if (shouldIgnore(key, ignoredPaths)) {
                continue;
            }
            if (!target.contains(key)) {
                target.set(key, defaults.get(key));
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Check if config needs reordering by comparing key positions with template.
     */
    private static boolean needsReordering(FileConfiguration current, FileConfiguration template) {
        List<String> currentTopKeys = new ArrayList<>(current.getKeys(false));
        List<String> templateTopKeys = new ArrayList<>(template.getKeys(false));

        // Filter to only keys that exist in both
        List<String> commonKeys = new ArrayList<>();
        for (String key : templateTopKeys) {
            if (current.contains(key)) {
                commonKeys.add(key);
            }
        }

        // Check if common keys are in the same order
        int currentIndex = 0;
        for (String templateKey : commonKeys) {
            while (currentIndex < currentTopKeys.size() && !currentTopKeys.get(currentIndex).equals(templateKey)) {
                currentIndex++;
            }
            if (currentIndex >= currentTopKeys.size()) {
                return true; // Key not found in expected position
            }
            currentIndex++;
        }

        return false;
    }

    /**
     * Reorder config to match template structure while preserving all values.
     */
    private static YamlConfiguration reorderConfig(FileConfiguration current, FileConfiguration template) {
        YamlConfiguration reordered = new YamlConfiguration();

        // Copy values in template order
        for (String key : template.getKeys(false)) {
            if (current.contains(key)) {
                copyConfigSection(current, reordered, key);
            }
        }

        // Add any additional keys not in template (preserve custom additions)
        for (String key : current.getKeys(false)) {
            if (!template.contains(key)) {
                copyConfigSection(current, reordered, key);
            }
        }

        return reordered;
    }

    /**
     * Copy a config section (including nested values) from source to destination.
     */
    private static void copyConfigSection(FileConfiguration source, FileConfiguration destination, String key) {
        Object value = source.get(key);
        if (value != null) {
            destination.set(key, value);
        }
    }

    private static boolean mergeSection(FileConfiguration rootDefaults,
                                        ConfigurationSection defaultsSection,
                                        FileConfiguration target,
                                        String path,
                                        Set<String> ignoredPaths) {
        boolean changed = false;
        for (String key : defaultsSection.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (shouldIgnore(fullPath, ignoredPaths)) {
                continue;
            }

            if (defaultsSection.isConfigurationSection(key)) {
                ConfigurationSection nestedDefaults = defaultsSection.getConfigurationSection(key);
                if (nestedDefaults == null) {
                    continue;
                }

                boolean targetHasSection = target.isConfigurationSection(fullPath);
                if (!targetHasSection) {
                    if (target.isSet(fullPath)) {
                        continue; // Don't override existing values of different types
                    }
                    target.createSection(fullPath);
                    changed = true;
                }

                if (mergeSection(rootDefaults, nestedDefaults, target, fullPath, ignoredPaths)) {
                    changed = true;
                }
            } else if (!target.isSet(fullPath)) {
                Object value = rootDefaults.get(fullPath);
                target.set(fullPath, value);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean shouldIgnore(String path, Set<String> ignoredPaths) {
        for (String ignored : ignoredPaths) {
            if (path.equalsIgnoreCase(ignored)) {
                return true;
            }

            if (path.length() > ignored.length()
                    && path.regionMatches(true, 0, ignored, 0, ignored.length())
                    && path.charAt(ignored.length()) == '.') {
                return true;
            }
        }
        return false;
    }

    private static double readVersion(FileConfiguration configuration, String versionPath) {
        if (configuration == null || versionPath == null || versionPath.isEmpty()) {
            return Double.NaN;
        }

        Object value = configuration.get(versionPath);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private static final class ConsumerInvoker {
        private ConsumerInvoker() {
        }

        private static void accept(java.util.function.Consumer<File> consumer, File file) {
            if (consumer != null) {
                consumer.accept(file);
            }
        }
    }
}
