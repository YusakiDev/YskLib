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

### Sound Management
- **Config-based sounds** - Load sounds from config with caching for performance
- **Preset sounds** - Built-in `success`, `error`, `click`, `ding` presets configurable in YskLib's config
- **Per-plugin pools** - Each plugin maintains its own sound registry
- **Module sounds** - Organize sounds by feature/module within plugins
- **Dual format support** - Compact (`SOUND:volume:pitch`) and verbose YAML formats

Example usage:
```java
YskLib yskLib = (YskLib) getServer().getPluginManager().getPlugin("YskLib");
SoundHelper soundHelper = yskLib.getSoundHelper();

// Play preset sounds
soundHelper.playSuccess(player);
soundHelper.playError(player);
soundHelper.playClick(player);

// Load custom sounds from your plugin's config
soundHelper.loadSounds(this, getConfig().getConfigurationSection("sounds"));

// Play a loaded sound by key
soundHelper.play(this, player, "reward");

// Direct play without config
soundHelper.play(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
```

Config format:
```yaml
# In YskLib config.yml - preset customization
modules:
  sounds:
    presets:
      success: "ENTITY_PLAYER_LEVELUP:1.0:1.2"

# In your plugin - custom sounds
sounds:
  reward: "ENTITY_PLAYER_LEVELUP:1.0:1.5"
  deny:
    sound: ENTITY_VILLAGER_NO
    volume: 1.0
    pitch: 0.8
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
