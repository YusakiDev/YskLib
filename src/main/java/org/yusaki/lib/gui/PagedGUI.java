package org.yusaki.lib.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.yusaki.lib.YskLib;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Extension of PatternGUI that adds pagination for dynamic-length lists.
 * <p>
 * Bypasses the parent's {@code dynamicArrayProviders} system and writes
 * directly to inventory slots, avoiding the {@code isReservedSlot} gate
 * and explicitly clearing trailing slots on the last page.
 */
public class PagedGUI extends PatternGUI {

    private int page;
    private int pageSize;
    private char listChar;
    private List<Integer> listSlots = Collections.emptyList();
    private Supplier<List<ItemStack>> fullListSupplier;
    private BiConsumer<InventoryClickEvent, Integer> itemClickHandler;

    private char prevChar;
    private char nextChar;
    private boolean navigationConfigured;

    public PagedGUI(YskLib lib, JavaPlugin plugin, GUIConfig config, GUIManager guiManager) {
        super(lib, plugin, config, guiManager);
        this.page = 0;
    }

    /**
     * Register a paginated list area mapped to a pattern character.
     * The number of slots for that character in the pattern determines the page size.
     *
     * @param character    the ASCII pattern character for list item slots
     * @param supplier     provides the full (unpaged) list of items
     * @param clickHandler receives the click event and the absolute index in the full list
     * @return this PagedGUI for chaining
     */
    public PagedGUI setPagedContent(char character,
                                     Supplier<List<ItemStack>> supplier,
                                     BiConsumer<InventoryClickEvent, Integer> clickHandler) {
        this.listChar = character;
        this.fullListSupplier = supplier;
        this.itemClickHandler = clickHandler;
        this.listSlots = parser.getPatternSlots(character);
        this.pageSize = listSlots.size();

        // Register click handler that maps slot position to absolute list index
        onClick(character, event -> {
            int slotIndex = this.listSlots.indexOf(event.getSlot());
            if (slotIndex < 0) return;

            int absoluteIndex = this.page * this.pageSize + slotIndex;
            List<ItemStack> fullList = this.fullListSupplier.get();
            if (absoluteIndex >= 0 && absoluteIndex < fullList.size()) {
                this.itemClickHandler.accept(event, absoluteIndex);
            }
        });

        return this;
    }

    /**
     * Wire previous/next page buttons to pattern characters.
     * Automatically renders arrow or barrier items based on page bounds.
     *
     * @param prevChar pattern character for "previous page" button
     * @param nextChar pattern character for "next page" button
     * @return this PagedGUI for chaining
     */
    public PagedGUI setPageNavigation(char prevChar, char nextChar) {
        this.prevChar = prevChar;
        this.nextChar = nextChar;
        this.navigationConfigured = true;

        // Just change page state — PatternGUI.handleClick() already calls
        // updateDynamicContent() after every click handler, so no need for
        // an explicit refresh() which would double-render.
        onClick(prevChar, event -> prevPage());
        onClick(nextChar, event -> nextPage());

        return this;
    }

    // ── Page Control ─────────────────────────────────────────────

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        int maxPage = Math.max(0, getPageCount() - 1);
        this.page = Math.max(0, Math.min(page, maxPage));
    }

    public int getPageCount() {
        if (fullListSupplier == null || pageSize <= 0) return 1;
        List<ItemStack> fullList = fullListSupplier.get();
        return Math.max(1, (int) Math.ceil((double) fullList.size() / pageSize));
    }

    public void nextPage() {
        setPage(page + 1);
    }

    public void prevPage() {
        setPage(page - 1);
    }

    // ── Rendering ────────────────────────────────────────────────

    @Override
    public void updateDynamicContent() {
        // Let parent handle any non-paged dynamic content
        super.updateDynamicContent();

        if (fullListSupplier == null) return;

        // Clamp page in case the list shrank since last render
        int maxPage = Math.max(0, getPageCount() - 1);
        if (page > maxPage) {
            page = maxPage;
        }

        // Fill list slots for current page
        List<ItemStack> fullList = fullListSupplier.get();
        int startIndex = page * pageSize;

        for (int i = 0; i < listSlots.size(); i++) {
            int slot = listSlots.get(i);
            int dataIndex = startIndex + i;
            if (dataIndex < fullList.size()) {
                inventory.setItem(slot, fullList.get(dataIndex));
            } else {
                inventory.setItem(slot, null); // clear trailing slots
            }
        }

        // Render navigation buttons
        if (navigationConfigured) {
            renderNavButton(prevChar, page > 0,
                    "Previous Page", "No previous page");
            renderNavButton(nextChar, page < getPageCount() - 1,
                    "Next Page", "No next page");
        }
    }

    private void renderNavButton(char navChar, boolean active,
                                  String activeName, String inactiveName) {
        List<Integer> slots = parser.getPatternSlots(navChar);
        ItemStack item;
        if (active) {
            item = new ItemStack(Material.ARROW);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(activeName)
                    .color(NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        } else {
            item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(inactiveName)
                    .color(NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        for (int slot : slots) {
            inventory.setItem(slot, item);
        }
    }
}
