package org.yusaki.lib.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Describes how a configuration file should be updated.
 */
public final class ConfigUpdateOptions {
    private final String fileName;
    private final String resourcePath;
    private final File directory;
    private final String versionPath;
    private final Consumer<File> reloadAction;
    private final Consumer<File> resetAction;
    private final Function<FileConfiguration, List<String>> ignoredSectionsSupplier;
    private final List<ConfigMigration> migrations;
    private final boolean backupEnabled;
    private final boolean skipMergeIfVersionMatches;
    private final boolean preserveExistingValues;
    private final boolean reorderToTemplate;

    private ConfigUpdateOptions(Builder builder) {
        this.fileName = builder.fileName;
        this.resourcePath = builder.resourcePath != null ? builder.resourcePath : builder.fileName;
        this.directory = builder.directory;
        this.versionPath = builder.versionPath;
        this.reloadAction = builder.reloadAction;
        this.resetAction = builder.resetAction;
        this.ignoredSectionsSupplier = builder.ignoredSectionsSupplier;
        this.migrations = List.copyOf(builder.migrations);
        this.backupEnabled = builder.backupEnabled;
        this.skipMergeIfVersionMatches = builder.skipMergeIfVersionMatches;
        this.preserveExistingValues = builder.preserveExistingValues;
        this.reorderToTemplate = builder.reorderToTemplate;
    }

    public String fileName() {
        return fileName;
    }

    public String resourcePath() {
        return resourcePath;
    }

    public File directory() {
        return directory;
    }

    public String versionPath() {
        return versionPath;
    }

    public Consumer<File> reloadAction() {
        return reloadAction;
    }

    public Consumer<File> resetAction() {
        return resetAction;
    }

    public Function<FileConfiguration, List<String>> ignoredSectionsSupplier() {
        return ignoredSectionsSupplier;
    }

    public List<ConfigMigration> migrations() {
        return migrations;
    }

    public boolean backupEnabled() {
        return backupEnabled;
    }

    public boolean skipMergeIfVersionMatches() {
        return skipMergeIfVersionMatches;
    }

    public boolean preserveExistingValues() {
        return preserveExistingValues;
    }

    public boolean reorderToTemplate() {
        return reorderToTemplate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String fileName = "config.yml";
        private String resourcePath;
        private File directory;
        private String versionPath = "version";
        private Consumer<File> reloadAction;
        private Consumer<File> resetAction;
        private Function<FileConfiguration, List<String>> ignoredSectionsSupplier = config -> Collections.emptyList();
        private final List<ConfigMigration> migrations = new ArrayList<>();
        private boolean backupEnabled = true;
        private boolean skipMergeIfVersionMatches = false;
        private boolean preserveExistingValues = false;
        private boolean reorderToTemplate = false;

        private Builder() {
        }

        public Builder fileName(String fileName) {
            this.fileName = Objects.requireNonNull(fileName, "fileName");
            return this;
        }

        public Builder resourcePath(String resourcePath) {
            this.resourcePath = resourcePath;
            return this;
        }

        public Builder directory(File directory) {
            this.directory = directory;
            return this;
        }

        public Builder versionPath(String versionPath) {
            this.versionPath = versionPath;
            return this;
        }

        public Builder reloadAction(Consumer<File> reloadAction) {
            this.reloadAction = reloadAction;
            return this;
        }

        public Builder resetAction(Consumer<File> resetAction) {
            this.resetAction = resetAction;
            return this;
        }

        public Builder ignoredSectionsSupplier(Function<FileConfiguration, List<String>> supplier) {
            this.ignoredSectionsSupplier = Objects.requireNonNull(supplier, "ignoredSectionsSupplier");
            return this;
        }

        public Builder addMigration(ConfigMigration migration) {
            this.migrations.add(Objects.requireNonNull(migration, "migration"));
            return this;
        }

        public Builder migrations(Collection<ConfigMigration> migrations) {
            if (migrations != null) {
                migrations.forEach(this::addMigration);
            }
            return this;
        }

        public Builder backupEnabled(boolean backupEnabled) {
            this.backupEnabled = backupEnabled;
            return this;
        }

        public Builder skipMergeIfVersionMatches(boolean skipMergeIfVersionMatches) {
            this.skipMergeIfVersionMatches = skipMergeIfVersionMatches;
            return this;
        }

        public Builder preserveExistingValues(boolean preserveExistingValues) {
            this.preserveExistingValues = preserveExistingValues;
            return this;
        }

        public Builder reorderToTemplate(boolean reorderToTemplate) {
            this.reorderToTemplate = reorderToTemplate;
            return this;
        }

        public ConfigUpdateOptions build() {
            return new ConfigUpdateOptions(this);
        }
    }
}
