package org.yusaki.lib.modules;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ItemLibrary {
    private final YskLib lib;
    private final Map<JavaPlugin, Map<String, ItemStack>> pluginItems;
    private final Map<JavaPlugin, File> itemFiles;
    private final Map<JavaPlugin, FileConfiguration> itemConfigs;
    
    // Shared storage
    private final boolean useSharedStorage;
    private final File sharedItemFile;
    private final FileConfiguration sharedItemConfig;
    private final Map<String, ItemStack> sharedItems;

    public ItemLibrary(YskLib lib) {
        this.lib = lib;
        this.pluginItems = new HashMap<>();
        this.itemFiles = new HashMap<>();
        this.itemConfigs = new HashMap<>();
        
        // Initialize shared storage
        this.useSharedStorage = lib.getConfig().getBoolean("modules.item-library.shared-storage", false);
        this.sharedItems = new HashMap<>();
        
        if (useSharedStorage) {
            String sharedFileName = lib.getConfig().getString("modules.item-library.shared-file", "shared_items.yml");
            this.sharedItemFile = new File(lib.getDataFolder(), sharedFileName);
            if (!sharedItemFile.exists()) {
                try {
                    sharedItemFile.getParentFile().mkdirs();
                    sharedItemFile.createNewFile();
                } catch (IOException e) {
                    lib.getLogger().severe("Failed to create shared items file: " + e.getMessage());
                }
            }
            this.sharedItemConfig = YamlConfiguration.loadConfiguration(sharedItemFile);
            loadSharedItems();
        } else {
            this.sharedItemFile = null;
            this.sharedItemConfig = null;
        }
    }

    private void loadSharedItems() {
        if (!useSharedStorage) return;
        
        sharedItems.clear();
        ConfigurationSection itemsSection = sharedItemConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                try {
                    Object data = itemsSection.get(itemId + ".data");
                    if (data instanceof byte[]) {
                        ItemStack item = ItemStack.deserializeBytes((byte[]) data);
                        if (item != null) {
                            sharedItems.put(itemId.toLowerCase(), item);
                            lib.getLogger().info("Loaded shared item: " + itemId.toLowerCase());
                        }
                    }
                } catch (Exception e) {
                    lib.getLogger().warning("Failed to load shared item " + itemId + ": " + e.getMessage());
                }
            }
        }
    }

    public void loadItems(JavaPlugin plugin) {
        if (useSharedStorage) {
            // When using shared storage, we don't need to load plugin-specific items
            return;
        }
        
        // Initialize maps for this plugin if they don't exist
        pluginItems.computeIfAbsent(plugin, k -> new HashMap<>());
        
        // Setup items file
        File itemsFile = new File(plugin.getDataFolder(), "items.yml");
        itemFiles.put(plugin, itemsFile);
        
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }

        FileConfiguration itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        itemConfigs.put(plugin, itemsConfig);
        pluginItems.get(plugin).clear();

        ConfigurationSection itemsSection = itemsConfig.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String itemId : itemsSection.getKeys(false)) {
                try {
                    Object data = itemsSection.get(itemId + ".data");
                    if (data instanceof byte[]) {
                        ItemStack item = ItemStack.deserializeBytes((byte[]) data);
                        if (item != null) {
                            pluginItems.get(plugin).put(itemId.toLowerCase(), item);
                            lib.logDebug(plugin, "Loaded item: " + itemId.toLowerCase());
                        }
                    }
                } catch (Exception e) {
                    lib.logWarn(plugin, "Failed to load item " + itemId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void saveItem(JavaPlugin plugin, String id, ItemStack item) {
        try {
            id = id.toLowerCase();
            
            if (useSharedStorage) {
                byte[] itemData = item.serializeAsBytes();
                sharedItemConfig.set("items." + id + ".data", itemData);
                sharedItemConfig.save(sharedItemFile);
                sharedItems.put(id, item.clone());
                lib.logDebug(plugin, "Saved shared item: " + id);
                return;
            }
            
            pluginItems.computeIfAbsent(plugin, k -> new HashMap<>());
            
            FileConfiguration itemsConfig = itemConfigs.computeIfAbsent(plugin, k -> {
                File file = new File(plugin.getDataFolder(), "items.yml");
                return YamlConfiguration.loadConfiguration(file);
            });
            
            File itemsFile = itemFiles.computeIfAbsent(plugin, k -> 
                new File(plugin.getDataFolder(), "items.yml"));
            
            byte[] itemData = item.serializeAsBytes();
            
            itemsConfig.set("items." + id + ".data", itemData);
            itemsConfig.save(itemsFile);
            
            pluginItems.get(plugin).put(id, item.clone());
            lib.logDebug(plugin, "Saved item: " + id);
        } catch (Exception e) {
            lib.logWarn(plugin, "Failed to save item " + id + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ItemStack getItem(JavaPlugin plugin, String id) {
        if (useSharedStorage) {
            ItemStack item = sharedItems.get(id.toLowerCase());
            return item != null ? item.clone() : null;
        }
        
        Map<String, ItemStack> items = pluginItems.get(plugin);
        if (items != null) {
            ItemStack item = items.get(id.toLowerCase());
            return item != null ? item.clone() : null;
        }
        return null;
    }

    public Set<String> getItemIds(JavaPlugin plugin) {
        if (useSharedStorage) {
            return sharedItems.keySet();
        }
        
        return pluginItems.getOrDefault(plugin, new HashMap<>()).keySet();
    }

    public boolean hasItem(JavaPlugin plugin, String id) {
        if (useSharedStorage) {
            return sharedItems.containsKey(id.toLowerCase());
        }
        
        Map<String, ItemStack> items = pluginItems.get(plugin);
        return items != null && items.containsKey(id.toLowerCase());
    }

    public void removeItem(JavaPlugin plugin, String id) {
        if (useSharedStorage) {
            id = id.toLowerCase();
            sharedItems.remove(id);
            sharedItemConfig.set("items." + id, null);
            try {
                sharedItemConfig.save(sharedItemFile);
                lib.logDebug(plugin, "Removed shared item: " + id);
            } catch (Exception e) {
                lib.logWarn(plugin, "Failed to remove shared item " + id + ": " + e.getMessage());
            }
            return;
        }
        
        Map<String, ItemStack> items = pluginItems.get(plugin);
        FileConfiguration itemsConfig = itemConfigs.get(plugin);
        File itemsFile = itemFiles.get(plugin);
        
        if (items != null && itemsConfig != null && itemsFile != null) {
            items.remove(id.toLowerCase());
            itemsConfig.set("items." + id, null);
            try {
                itemsConfig.save(itemsFile);
                lib.logDebug(plugin, "Removed item: " + id);
            } catch (Exception e) {
                lib.logWarn(plugin, "Failed to remove item " + id + ": " + e.getMessage());
            }
        }
    }
} 