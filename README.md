# YskLib

YskLib is a library plugin for Yusaki's plugins. It provides a set of utility functions and features that can be used across multiple plugins.

## Features

### World Management
- `YskLib#canExecuteInWorld(JavaPlugin plugin, World world)` checks if a plugin is enabled in a specific world based on the `enabled-worlds` configuration list.
- Supports wildcard `*` to enable all worlds.

### Configuration Management
- `YskLib#updateConfig(JavaPlugin plugin)` now delegates to a comprehensive updater that creates timestamped backups, runs optional migrations, and merges missing defaults from the bundled resources before reloading the file.
- `ConfigUpdateOptions` lets you customise which file to target, where to find the default resource, which sections to ignore during merges, and which migrations to run.
- `ConfigUpdateService.update(plugin, options)` can be invoked directly if you need to update several YAML files; use `ConfigUpdateOptions.builder()` to configure reload/reset callbacks per file.
- Define migrations with `ConfigMigration` to mutate legacy data while automatically bumping the tracked version number.
- Ignored sections are respected during the merge so administrator overrides (e.g. GUI layouts) survive updates.
- See [`docs/config-updater.md`](docs/config-updater.md) for an in-depth guide and usage examples.
