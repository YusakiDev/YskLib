# YskLib

YskLib is a library plugin for Yusaki's plugins. It provides a set of utility functions and features that can be used across multiple plugins.

## Features

### Message Management
- **Cached messaging system** - Load messages once at startup, reuse from memory for better performance
- **Dual placeholder support** - Supports both `{placeholder}` and `%placeholder%` formats for backward compatibility
- **Multi-line messages** - Support for List-based messages
- **Placeholder replacement** - Easy key-value placeholder system using Maps
- **Action bar & title helpers** - Built-in methods for sending action bars and titles
- **Centralized management** - One place to handle all plugin messaging

Example usage:
```java
// Load messages in onEnable
YskLib yskLib = (YskLib) getServer().getPluginManager().getPlugin("YskLib");
yskLib.loadMessages(this);
MessageManager messageManager = yskLib.getMessageManager();

// Send message with placeholders (supports both {player} and %player%)
messageManager.sendMessage(this, player, "welcome-message",
    MessageManager.placeholders("player", player.getName(), "server", "MyServer"));
```

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
