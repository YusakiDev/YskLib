package org.yusaki.lib.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Fluent builder for creating paginated GUIs.
 * Takes a {@link GUIConfig} directly (not a GUI type string)
 * because {@link GUIManager#createGUI} returns {@link GUIBuilder}
 * wrapping a plain {@link PatternGUI}, which cannot be downcast.
 */
public class PagedGUIBuilder {

    private final PagedGUI gui;

    public PagedGUIBuilder(YskLib lib, JavaPlugin plugin, GUIConfig config, GUIManager guiManager) {
        this.gui = new PagedGUI(lib, plugin, config, guiManager);
    }

    /**
     * Static factory method.
     */
    public static PagedGUIBuilder create(YskLib lib, JavaPlugin plugin,
                                          GUIConfig config, GUIManager guiManager) {
        return new PagedGUIBuilder(lib, plugin, config, guiManager);
    }

    // ── Paged-specific methods ───────────────────────────────────

    public PagedGUIBuilder setPagedContent(char character,
                                            Supplier<List<ItemStack>> supplier,
                                            BiConsumer<InventoryClickEvent, Integer> clickHandler) {
        gui.setPagedContent(character, supplier, clickHandler);
        return this;
    }

    public PagedGUIBuilder setPageNavigation(char prevChar, char nextChar) {
        gui.setPageNavigation(prevChar, nextChar);
        return this;
    }

    // ── Delegated PatternGUI methods ─────────────────────────────

    public PagedGUIBuilder onClick(char character, Consumer<InventoryClickEvent> handler) {
        gui.onClick(character, handler);
        return this;
    }

    public PagedGUIBuilder setDynamicContent(char character, Supplier<ItemStack> provider) {
        gui.setDynamicContent(character, provider);
        return this;
    }

    public PagedGUIBuilder setContext(String key, Object value) {
        gui.setContext(key, value);
        return this;
    }

    // ── Build / Open ─────────────────────────────────────────────

    public PagedGUI build() {
        return gui;
    }

    public PagedGUI open(Player player) {
        gui.open(player);
        return gui;
    }
}
