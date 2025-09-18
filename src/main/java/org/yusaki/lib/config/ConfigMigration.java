package org.yusaki.lib.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a data migration step for a configuration file.
 */
public final class ConfigMigration {
    private final double targetVersion;
    private final Predicate<FileConfiguration> predicate;
    private final Consumer<FileConfiguration> action;
    private final String description;

    private ConfigMigration(double targetVersion,
                            Predicate<FileConfiguration> predicate,
                            Consumer<FileConfiguration> action,
                            String description) {
        this.targetVersion = targetVersion;
        this.predicate = Objects.requireNonNull(predicate, "predicate");
        this.action = Objects.requireNonNull(action, "action");
        this.description = description == null ? "" : description;
    }

    public double targetVersion() {
        return targetVersion;
    }

    public Predicate<FileConfiguration> predicate() {
        return predicate;
    }

    public Consumer<FileConfiguration> action() {
        return action;
    }

    public String description() {
        return description;
    }

    /**
     * Creates a migration that always runs when the stored version is below the target version.
     */
    public static ConfigMigration of(double targetVersion, Consumer<FileConfiguration> action) {
        return new ConfigMigration(targetVersion, config -> true, action, "");
    }

    /**
     * Creates a migration with an optional description used for logging.
     */
    public static ConfigMigration of(double targetVersion,
                                     Consumer<FileConfiguration> action,
                                     String description) {
        return new ConfigMigration(targetVersion, config -> true, action, description);
    }

    /**
     * Creates a migration guarded by a predicate that determines whether it should run.
     */
    public static ConfigMigration guarded(double targetVersion,
                                          Predicate<FileConfiguration> predicate,
                                          Consumer<FileConfiguration> action,
                                          String description) {
        return new ConfigMigration(targetVersion, predicate, action, description);
    }
}
