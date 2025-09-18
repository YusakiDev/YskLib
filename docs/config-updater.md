# Configuration Updater Guide

This guide explains how to use the comprehensive configuration updater included in YskLib. The updater helps keep YAML files aligned with the defaults bundled in your plugin JAR, while protecting administrator changes and running safe data migrations.

## Overview

- **ConfigUpdateService** orchestrates updates for one or many files.
- **ConfigUpdateOptions** describes how a single file should be processed (location, version field, callbacks, migrations).
- **ConfigMigration** models a single versioned mutation, allowing you to move or normalise data between releases.

YskLib still exposes `YskLib#updateConfig(JavaPlugin plugin)` for the common case of refreshing `config.yml`. Internally it now builds suitable options and delegates to the service.

## Quick Start

```java
// Inside your plugin's onEnable or reload command
YskLib yskLib = getServer().getServicesManager().load(YskLib.class);
if (yskLib != null) {
    yskLib.updateConfig(this); // updates config.yml using defaults and reloads it
}
```

The helper above performs the following:
1. Reloads the live `config.yml`.
2. Creates a timestamped backup if the merge might change anything.
3. Fills in any missing keys from the default resource packaged in the JAR.
4. Runs configured migrations (none by default).
5. Updates the stored version to match the default and saves the result.
6. Reloads the configuration again so runtime code sees the latest data.

## Building Custom Update Options

Use the builder to tailor updates for other YAML files:

```java
ConfigUpdateOptions messagesOptions = ConfigUpdateOptions.builder()
        .fileName("messages.yml")
        .resourcePath("messages.yml")
        .versionPath("messages-version")
        .reloadAction(file -> messages = YamlConfiguration.loadConfiguration(file))
        .resetAction(file -> saveResource("messages.yml", false))
        .ignoredSectionsSupplier(cfg -> List.of("placeholders.custom"))
        .addMigration(ConfigMigration.of(2.0, cfg -> cfg.set("gui.title", "&aNew Title"), "Update GUI title"))
        .skipMergeIfVersionMatches(true)
        .build();

YskLib lib = ...;
lib.updateConfig(this, messagesOptions);
```

### Key Builder Methods

- `fileName(String)` — disk filename inside the target directory (defaults to `config.yml`).
- `resourcePath(String)` — path inside the plugin JAR used for defaults and reset actions (defaults to the same as `fileName`).
- `directory(File)` — override the directory if the file is not located directly under `getDataFolder()`.
- `versionPath(String)` — YAML path used to compare versions (set to `null` if you do not track versions).
- `reloadAction(Consumer<File>)` — callback invoked after a successful update so you can re-read the file.
- `resetAction(Consumer<File>)` — invoked when the file is missing or invalid; typically call `saveResource(...)` here.
- `ignoredSectionsSupplier(Function<FileConfiguration, List<String>>)` — supply paths that should not be overwritten during merges (for example, user-defined GUI layouts).
- `addMigration(ConfigMigration)` / `migrations(Collection<ConfigMigration>)` — register migrations to run in order.
- `backupEnabled(boolean)` — disable backup creation if you already manage backups elsewhere.
- `skipMergeIfVersionMatches(boolean)` — skip the merge when the stored and default versions are identical.

## Running Multiple Updates Together

`ConfigUpdateService.updateAll(plugin, optionsCollection)` prevents duplicate processing and ensures all files are handled consistently:

```java
List<ConfigUpdateOptions> options = List.of(configOptions, messagesOptions, skinsOptions);
ConfigUpdateService.updateAll(this, options);
```

The service automatically avoids processing the same file twice even if you pass duplicate entries.

## Creating Migrations

`ConfigMigration` consists of a target version, an optional predicate, and an action to mutate the configuration.

```java
ConfigMigration cleanLegacyKeys = ConfigMigration.of(3.0, cfg -> {
    cfg.set("legacy-path", null);
    cfg.set("new-path", "value");
}, "Remove legacy keys");

ConfigMigration guardedMigration = ConfigMigration.guarded(
        4.0,
        cfg -> cfg.contains("menus"),
        cfg -> cfg.set("menus.default", "starter"),
        "Ensure menu default"
);
```

- Migrations run in ascending `targetVersion` order.
- When a migration runs, the version path is set to the migration's target (if a version path is configured and the target is not `NaN`).
- Returning `false` from the predicate (for guarded migrations) skips the action without affecting the stored version.
- Provide descriptive text so log statements help diagnose which steps ran.

## Ignored Sections

Use the supplier to return absolute YAML paths that should be left untouched. Children of an ignored section are also preserved.

```java
optionsBuilder.ignoredSectionsSupplier(cfg -> List.of(
        "gui.layouts",
        "messages.custom"
));
```

When `gui.layouts.title` is ignored, none of its nested keys will be overwritten by defaults.

## Error Handling and Backups

- On successful load, a timestamped `.bak` file is created before changes and removed afterwards.
- If the YAML cannot be parsed, the backup remains on disk and the reset action is invoked to restore a clean copy.
- When SnakeYAML reports a specific line for invalid YAML, the line number is logged to make manual fixes quicker.

## Tips for Reload Commands

- Run the updater asynchronously if your plugin performs heavy follow-up work; the merge itself is synchronous.
- Chain custom reload callbacks inside `reloadAction` to update caches, rebuild GUI managers, or notify systems of changes.
- Combine migrations with runtime refresh tasks to ensure data and behaviour stay in sync after an update.

## Further Reading

Refer to the source for implementation details:
- `src/main/java/org/yusaki/lib/config/ConfigUpdateService.java`
- `src/main/java/org/yusaki/lib/config/ConfigUpdateOptions.java`
- `src/main/java/org/yusaki/lib/config/ConfigMigration.java`
- `src/main/java/org/yusaki/lib/YskLib.java`
