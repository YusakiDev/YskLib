package org.yusaki.lib.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class TextInputPrompt {
    private final Component title;
    private final List<Component> body;
    private final String fieldKey;
    private final Component label;
    private final int width;
    private final String initialValue;
    private final int maxLength;
    private final Integer multilineMaxLines;
    private final Integer multilineHeight;
    private final boolean allowBlank;
    private final boolean canCloseWithEscape;
    private final Component submitLabel;
    private final Component cancelLabel;
    private final Component submitTooltip;
    private final Component cancelTooltip;
    private final Component invalidInputMessage;
    private final BiConsumer<Player, String> onSubmit;
    private final Consumer<Player> onCancel;

    private TextInputPrompt(Builder builder) {
        this.title = Objects.requireNonNull(builder.title, "title");
        this.body = List.copyOf(builder.body);
        this.fieldKey = Objects.requireNonNull(builder.fieldKey, "fieldKey");
        this.label = Objects.requireNonNull(builder.label, "label");
        this.width = builder.width;
        this.initialValue = builder.initialValue;
        this.maxLength = builder.maxLength;
        this.multilineMaxLines = builder.multilineMaxLines;
        this.multilineHeight = builder.multilineHeight;
        this.allowBlank = builder.allowBlank;
        this.canCloseWithEscape = builder.canCloseWithEscape;
        this.submitLabel = Objects.requireNonNull(builder.submitLabel, "submitLabel");
        this.cancelLabel = Objects.requireNonNull(builder.cancelLabel, "cancelLabel");
        this.submitTooltip = builder.submitTooltip;
        this.cancelTooltip = builder.cancelTooltip;
        this.invalidInputMessage = Objects.requireNonNull(builder.invalidInputMessage, "invalidInputMessage");
        this.onSubmit = builder.onSubmit;
        this.onCancel = builder.onCancel;
    }

    public static Builder builder(Component title, String fieldKey, Component label) {
        return new Builder(title, fieldKey, label);
    }

    public Component title() {
        return title;
    }

    public List<Component> body() {
        return body;
    }

    public String fieldKey() {
        return fieldKey;
    }

    public Component label() {
        return label;
    }

    public int width() {
        return width;
    }

    public String initialValue() {
        return initialValue;
    }

    public int maxLength() {
        return maxLength;
    }

    public Integer multilineMaxLines() {
        return multilineMaxLines;
    }

    public Integer multilineHeight() {
        return multilineHeight;
    }

    public boolean allowBlank() {
        return allowBlank;
    }

    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    public Component submitLabel() {
        return submitLabel;
    }

    public Component cancelLabel() {
        return cancelLabel;
    }

    public Component submitTooltip() {
        return submitTooltip;
    }

    public Component cancelTooltip() {
        return cancelTooltip;
    }

    public Component invalidInputMessage() {
        return invalidInputMessage;
    }

    public BiConsumer<Player, String> onSubmit() {
        return onSubmit;
    }

    public Consumer<Player> onCancel() {
        return onCancel;
    }

    public static final class Builder {
        private final Component title;
        private final String fieldKey;
        private final Component label;
        private final List<Component> body = new ArrayList<>();
        private int width = 320;
        private String initialValue = "";
        private int maxLength = 64;
        private Integer multilineMaxLines;
        private Integer multilineHeight;
        private boolean allowBlank = false;
        private boolean canCloseWithEscape = true;
        private Component submitLabel = Component.text("Submit");
        private Component cancelLabel = Component.text("Cancel");
        private Component submitTooltip;
        private Component cancelTooltip;
        private Component invalidInputMessage = Component.text("Please enter a value or type cancel.");
        private BiConsumer<Player, String> onSubmit = (player, value) -> { };
        private Consumer<Player> onCancel = player -> { };

        private Builder(Component title, String fieldKey, Component label) {
            this.title = title;
            this.fieldKey = fieldKey;
            this.label = label;
        }

        public Builder body(Component... lines) {
            if (lines != null) {
                for (Component line : lines) {
                    if (line != null) {
                        this.body.add(line);
                    }
                }
            }
            return this;
        }

        public Builder body(List<Component> lines) {
            if (lines != null) {
                for (Component line : lines) {
                    if (line != null) {
                        this.body.add(line);
                    }
                }
            }
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder initialValue(String initialValue) {
            this.initialValue = initialValue == null ? "" : initialValue;
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder multiline(Integer maxLines, Integer height) {
            this.multilineMaxLines = maxLines;
            this.multilineHeight = height;
            return this;
        }

        public Builder allowBlank(boolean allowBlank) {
            this.allowBlank = allowBlank;
            return this;
        }

        public Builder canCloseWithEscape(boolean canCloseWithEscape) {
            this.canCloseWithEscape = canCloseWithEscape;
            return this;
        }

        public Builder submitLabel(Component submitLabel) {
            this.submitLabel = submitLabel;
            return this;
        }

        public Builder cancelLabel(Component cancelLabel) {
            this.cancelLabel = cancelLabel;
            return this;
        }

        public Builder submitTooltip(Component submitTooltip) {
            this.submitTooltip = submitTooltip;
            return this;
        }

        public Builder cancelTooltip(Component cancelTooltip) {
            this.cancelTooltip = cancelTooltip;
            return this;
        }

        public Builder invalidInputMessage(Component invalidInputMessage) {
            this.invalidInputMessage = invalidInputMessage;
            return this;
        }

        public Builder onSubmit(BiConsumer<Player, String> onSubmit) {
            this.onSubmit = Objects.requireNonNull(onSubmit, "onSubmit");
            return this;
        }

        public Builder onCancel(Consumer<Player> onCancel) {
            this.onCancel = Objects.requireNonNull(onCancel, "onCancel");
            return this;
        }

        public TextInputPrompt build() {
            return new TextInputPrompt(this);
        }
    }
}
