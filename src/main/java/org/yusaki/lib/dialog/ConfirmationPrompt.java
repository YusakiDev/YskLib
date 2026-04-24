package org.yusaki.lib.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class ConfirmationPrompt {
    private final Component title;
    private final List<Component> body;
    private final Component confirmLabel;
    private final Component cancelLabel;
    private final Component confirmTooltip;
    private final Component cancelTooltip;
    private final boolean canCloseWithEscape;
    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;

    private ConfirmationPrompt(Builder builder) {
        this.title = Objects.requireNonNull(builder.title, "title");
        this.body = List.copyOf(builder.body);
        this.confirmLabel = Objects.requireNonNull(builder.confirmLabel, "confirmLabel");
        this.cancelLabel = Objects.requireNonNull(builder.cancelLabel, "cancelLabel");
        this.confirmTooltip = builder.confirmTooltip;
        this.cancelTooltip = builder.cancelTooltip;
        this.canCloseWithEscape = builder.canCloseWithEscape;
        this.onConfirm = builder.onConfirm;
        this.onCancel = builder.onCancel;
    }

    public static Builder builder(Component title) {
        return new Builder(title);
    }

    public Component title() {
        return title;
    }

    public List<Component> body() {
        return body;
    }

    public Component confirmLabel() {
        return confirmLabel;
    }

    public Component cancelLabel() {
        return cancelLabel;
    }

    public Component confirmTooltip() {
        return confirmTooltip;
    }

    public Component cancelTooltip() {
        return cancelTooltip;
    }

    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    public Consumer<Player> onConfirm() {
        return onConfirm;
    }

    public Consumer<Player> onCancel() {
        return onCancel;
    }

    public static final class Builder {
        private final Component title;
        private final List<Component> body = new ArrayList<>();
        private Component confirmLabel = Component.text("Confirm");
        private Component cancelLabel = Component.text("Cancel");
        private Component confirmTooltip;
        private Component cancelTooltip;
        private boolean canCloseWithEscape = true;
        private Consumer<Player> onConfirm = player -> { };
        private Consumer<Player> onCancel = player -> { };

        private Builder(Component title) {
            this.title = title;
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

        public Builder confirmLabel(Component confirmLabel) {
            this.confirmLabel = confirmLabel;
            return this;
        }

        public Builder cancelLabel(Component cancelLabel) {
            this.cancelLabel = cancelLabel;
            return this;
        }

        public Builder confirmTooltip(Component confirmTooltip) {
            this.confirmTooltip = confirmTooltip;
            return this;
        }

        public Builder cancelTooltip(Component cancelTooltip) {
            this.cancelTooltip = cancelTooltip;
            return this;
        }

        public Builder canCloseWithEscape(boolean canCloseWithEscape) {
            this.canCloseWithEscape = canCloseWithEscape;
            return this;
        }

        public Builder onConfirm(Consumer<Player> onConfirm) {
            this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
            return this;
        }

        public Builder onCancel(Consumer<Player> onCancel) {
            this.onCancel = Objects.requireNonNull(onCancel, "onCancel");
            return this;
        }

        public ConfirmationPrompt build() {
            return new ConfirmationPrompt(this);
        }
    }
}
