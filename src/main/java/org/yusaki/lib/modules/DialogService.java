package org.yusaki.lib.modules;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.Nullable;
import org.yusaki.lib.YskLib;
import org.yusaki.lib.dialog.ConfirmationPrompt;
import org.yusaki.lib.dialog.NoticePrompt;
import org.yusaki.lib.dialog.TextInputPrompt;
import io.papermc.paper.dialog.Dialog;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class DialogService implements Listener {
    private static final int DEFAULT_BUTTON_WIDTH = 140;

    private final YskLib lib;
    private final boolean preferNativeDialogs;
    private final boolean chatFallbackEnabled;
    private final ClickCallback.Options callbackOptions;
    private final Set<String> cancelKeywords;
    private final Map<UUID, PendingTextPrompt> pendingTextPrompts = new ConcurrentHashMap<>();

    public DialogService(YskLib lib) {
        this.lib = lib;

        FileConfiguration config = lib.getConfig();
        this.preferNativeDialogs = config.getBoolean("modules.dialogs.prefer-native", true);
        this.chatFallbackEnabled = config.getBoolean("modules.dialogs.chat-fallback", true);

        int callbackUses = Math.max(1, config.getInt("modules.dialogs.callback-uses", 1));
        long callbackLifetimeSeconds = Math.max(10L, config.getLong("modules.dialogs.callback-lifetime-seconds", 120L));
        this.callbackOptions = ClickCallback.Options.builder()
                .uses(callbackUses)
                .lifetime(Duration.ofSeconds(callbackLifetimeSeconds))
                .build();

        List<String> configuredKeywords = config.getStringList("modules.dialogs.text-input.cancel-keywords");
        if (configuredKeywords.isEmpty()) {
            configuredKeywords = List.of("cancel");
        }
        this.cancelKeywords = configuredKeywords.stream()
                .map(keyword -> keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT))
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toUnmodifiableSet());

        lib.getServer().getPluginManager().registerEvents(this, lib);
    }

    public void showConfirmation(Player player, ConfirmationPrompt prompt) {
        if (preferNativeDialogs) {
            try {
                showNativeConfirmation(player, prompt);
                return;
            } catch (RuntimeException exception) {
                lib.logWarn(lib, "Falling back to chat confirmation: " + exception.getMessage());
            }
        }
        if (chatFallbackEnabled) {
            showChatConfirmation(player, prompt);
            return;
        }
        sendChatPrompt(prompt.title(), prompt.body(), player);
    }

    public void showNotice(Player player, NoticePrompt prompt) {
        if (preferNativeDialogs) {
            try {
                showNativeNotice(player, prompt);
                return;
            } catch (RuntimeException exception) {
                lib.logWarn(lib, "Falling back to chat notice: " + exception.getMessage());
            }
        }
        if (chatFallbackEnabled) {
            showChatNotice(player, prompt);
            return;
        }
        sendChatPrompt(prompt.title(), prompt.body(), player);
    }

    public void showTextInput(Player player, TextInputPrompt prompt) {
        clearPendingTextPrompt(player.getUniqueId());

        if (preferNativeDialogs) {
            try {
                showNativeTextInput(player, prompt);
                return;
            } catch (RuntimeException exception) {
                lib.logWarn(lib, "Falling back to chat input prompt: " + exception.getMessage());
            }
        }
        if (chatFallbackEnabled) {
            showChatTextInput(player, prompt);
            return;
        }

        sendChatPrompt(prompt.title(), prompt.body(), player);
        player.sendMessage(Component.text("Prompt input is disabled."));
    }

    public void clearPendingTextPrompt(UUID playerId) {
        pendingTextPrompts.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        PendingTextPrompt pending = pendingTextPrompts.get(event.getPlayer().getUniqueId());
        if (pending == null) {
            return;
        }

        event.setCancelled(true);

        String plainText = lib.colorizePlain(event.originalMessage()).trim();
        String normalized = plainText.toLowerCase(Locale.ROOT);

        if (cancelKeywords.contains(normalized)) {
            pendingTextPrompts.remove(event.getPlayer().getUniqueId());
            runForPlayer(event.getPlayer(), pending.prompt().onCancel());
            return;
        }

        if (!pending.prompt().allowBlank() && plainText.isBlank()) {
            runForPlayer(event.getPlayer(), player -> {
                player.sendMessage(pending.prompt().invalidInputMessage());
                player.sendMessage(chatInputInstruction());
            });
            return;
        }

        pendingTextPrompts.remove(event.getPlayer().getUniqueId());
        runForPlayer(event.getPlayer(), player -> pending.prompt().onSubmit().accept(player, plainText));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPendingTextPrompt(event.getPlayer().getUniqueId());
    }

    private void showNativeConfirmation(Player player, ConfirmationPrompt prompt) {
        DialogBase base = dialogBase(prompt.title(), prompt.body(), prompt.canCloseWithEscape(), List.of());
        ActionButton confirm = actionButton(prompt.confirmLabel(), prompt.confirmTooltip(),
                DialogAction.customClick((response, audience) -> runPromptAction(playerFromAudience(audience, player), prompt.onConfirm()), callbackOptions));
        ActionButton cancel = actionButton(prompt.cancelLabel(), prompt.cancelTooltip(),
                DialogAction.customClick((response, audience) -> runPromptAction(playerFromAudience(audience, player), prompt.onCancel()), callbackOptions));

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.confirmation(confirm, cancel)));
        player.showDialog(dialog);
    }

    private void showNativeNotice(Player player, NoticePrompt prompt) {
        DialogBase base = dialogBase(prompt.title(), prompt.body(), prompt.canCloseWithEscape(), List.of());
        ActionButton action = actionButton(prompt.actionLabel(), prompt.actionTooltip(),
                DialogAction.customClick((response, audience) -> runPromptAction(playerFromAudience(audience, player), prompt.onAcknowledge()), callbackOptions));

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.notice(action)));
        player.showDialog(dialog);
    }

    private void showNativeTextInput(Player player, TextInputPrompt prompt) {
        TextDialogInput.Builder inputBuilder = DialogInput.text(prompt.fieldKey(), prompt.label())
                .width(prompt.width())
                .labelVisible(true)
                .initial(prompt.initialValue())
                .maxLength(prompt.maxLength());

        if (prompt.multilineMaxLines() != null || prompt.multilineHeight() != null) {
            inputBuilder.multiline(TextDialogInput.MultilineOptions.create(
                    prompt.multilineMaxLines(),
                    prompt.multilineHeight()));
        }

        TextDialogInput input = inputBuilder.build();
        DialogBase base = dialogBase(prompt.title(), prompt.body(), prompt.canCloseWithEscape(), List.of(input));

        ActionButton submit = actionButton(prompt.submitLabel(), prompt.submitTooltip(),
                DialogAction.customClick((response, audience) -> handleNativeTextSubmit(playerFromAudience(audience, player), prompt, response.getText(prompt.fieldKey())), callbackOptions));
        ActionButton cancel = actionButton(prompt.cancelLabel(), prompt.cancelTooltip(),
                DialogAction.customClick((response, audience) -> runPromptAction(playerFromAudience(audience, player), prompt.onCancel()), callbackOptions));

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.confirmation(submit, cancel)));
        player.showDialog(dialog);
    }

    private void handleNativeTextSubmit(Player player, TextInputPrompt prompt, @Nullable String value) {
        String resolved = value == null ? "" : value;
        if (!prompt.allowBlank() && resolved.isBlank()) {
            runForPlayer(player, current -> {
                current.sendMessage(prompt.invalidInputMessage());
                showTextInput(current, prompt);
            });
            return;
        }

        runForPlayer(player, current -> prompt.onSubmit().accept(current, resolved));
    }

    private void showChatConfirmation(Player player, ConfirmationPrompt prompt) {
        sendChatPrompt(prompt.title(), prompt.body(), player);
        player.sendMessage(actionComponent(prompt.confirmLabel(), prompt.confirmTooltip(),
                audience -> runPromptAction(playerFromAudience(audience, player), prompt.onConfirm()))
                .append(Component.space())
                .append(actionComponent(prompt.cancelLabel(), prompt.cancelTooltip(),
                        audience -> runPromptAction(playerFromAudience(audience, player), prompt.onCancel()))));
    }

    private void showChatNotice(Player player, NoticePrompt prompt) {
        sendChatPrompt(prompt.title(), prompt.body(), player);
        player.sendMessage(actionComponent(prompt.actionLabel(), prompt.actionTooltip(),
                audience -> runPromptAction(playerFromAudience(audience, player), prompt.onAcknowledge())));
    }

    private void showChatTextInput(Player player, TextInputPrompt prompt) {
        pendingTextPrompts.put(player.getUniqueId(), new PendingTextPrompt(prompt));
        sendChatPrompt(prompt.title(), prompt.body(), player);
        player.sendMessage(Component.text("Input: ").append(prompt.label()));
        if (!prompt.initialValue().isEmpty()) {
            player.sendMessage(Component.text("Current value: " + prompt.initialValue()));
        }
        player.sendMessage(chatInputInstruction());
    }

    private Component chatInputInstruction() {
        String cancelWord = cancelKeywords.stream().findFirst().orElse("cancel");
        return Component.text("Type your response in chat. Type \"" + cancelWord + "\" to abort.");
    }

    private void sendChatPrompt(Component title, List<Component> body, Player player) {
        player.sendMessage(title);
        for (Component line : body) {
            player.sendMessage(line);
        }
    }

    private Component actionComponent(Component label, @Nullable Component tooltip, Consumer<Audience> action) {
        Component component = label.clickEvent(ClickEvent.callback(action::accept, callbackOptions));
        if (tooltip != null) {
            component = component.hoverEvent(tooltip);
        }
        return component;
    }

    private ActionButton actionButton(Component label, @Nullable Component tooltip, DialogAction action) {
        ActionButton.Builder builder = ActionButton.builder(label)
                .width(DEFAULT_BUTTON_WIDTH)
                .action(action);
        if (tooltip != null) {
            builder.tooltip(tooltip);
        }
        return builder.build();
    }

    private DialogBase dialogBase(Component title,
                                  List<Component> body,
                                  boolean canCloseWithEscape,
                                  List<? extends io.papermc.paper.registry.data.dialog.input.DialogInput> inputs) {
        List<DialogBody> dialogBodies = body.stream()
                .map(line -> (DialogBody) DialogBody.plainMessage(line))
                .toList();

        return DialogBase.builder(title)
                .canCloseWithEscape(canCloseWithEscape)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .body(dialogBodies)
                .inputs(inputs)
                .build();
    }

    private void runPromptAction(Player player, Consumer<Player> action) {
        runForPlayer(player, action);
    }

    private void runForPlayer(Player player, Consumer<Player> action) {
        lib.getFoliaLib().getScheduler().runAtEntity(player, task -> action.accept(player));
    }

    private Player playerFromAudience(Audience audience, Player fallback) {
        return audience instanceof Player player ? player : fallback;
    }

    private record PendingTextPrompt(TextInputPrompt prompt) {
    }
}
