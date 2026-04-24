package org.yusaki.lib.dialog;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class NoticePrompt {
    private final Component title;
    private final List<Component> body;
    private final Component actionLabel;
    private final Component actionTooltip;
    private final boolean canCloseWithEscape;
    private final Consumer<Player> onAcknowledge;

    private NoticePrompt(Builder builder) {
        this.title = Objects.requireNonNull(builder.title, "title");
        this.body = List.copyOf(builder.body);
        this.actionLabel = Objects.requireNonNull(builder.actionLabel, "actionLabel");
        this.actionTooltip = builder.actionTooltip;
        this.canCloseWithEscape = builder.canCloseWithEscape;
        this.onAcknowledge = builder.onAcknowledge;
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

    public Component actionLabel() {
        return actionLabel;
    }

    public Component actionTooltip() {
        return actionTooltip;
    }

    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    public Consumer<Player> onAcknowledge() {
        return onAcknowledge;
    }

    public static final class Builder {
        private final Component title;
        private final List<Component> body = new ArrayList<>();
        private Component actionLabel = Component.text("OK");
        private Component actionTooltip;
        private boolean canCloseWithEscape = true;
        private Consumer<Player> onAcknowledge = player -> { };

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

        public Builder actionLabel(Component actionLabel) {
            this.actionLabel = actionLabel;
            return this;
        }

        public Builder actionTooltip(Component actionTooltip) {
            this.actionTooltip = actionTooltip;
            return this;
        }

        public Builder canCloseWithEscape(boolean canCloseWithEscape) {
            this.canCloseWithEscape = canCloseWithEscape;
            return this;
        }

        public Builder onAcknowledge(Consumer<Player> onAcknowledge) {
            this.onAcknowledge = Objects.requireNonNull(onAcknowledge, "onAcknowledge");
            return this;
        }

        public NoticePrompt build() {
            return new NoticePrompt(this);
        }
    }
}
