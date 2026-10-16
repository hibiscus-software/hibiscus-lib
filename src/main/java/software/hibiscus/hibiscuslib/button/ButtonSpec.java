/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a sidebar button needs: its icon, tooltip, what a click does, when it shows, its badge and where it sorts.
 * Build one with {@link #builder()} and register it with {@link HibiscusButtons#register(Identifier, ButtonSpec)}.
 *
 * <p>The click action runs on the client's main thread, with the inventory screen still open. To change anything on the
 * server it should send a payload; to open a screen it can just call {@code Minecraft.getInstance().setScreen(...)}.
 *
 * @param icon      the 16x16 icon
 * @param tooltip   the tooltip, asked for each frame the button is hovered
 * @param onClick   what a left click (or Enter/Space while focused) does
 * @param visible   whether the button shows at all (e.g. only while a feature is enabled on the server)
 * @param badge     the corner badge, asked for each frame; null or {@link ButtonBadge#NONE} for none
 * @param order     the default sort order, lowest first; the player's {@code buttons.order} list comes before it
 * @param keyMapping whether the button gets an (unbound) key mapping in the Hibiscus category. Only buttons registered
 *                  during mod construction can have one: key mappings are collected before client setup.
 */
public record ButtonSpec(
        ButtonIcon icon,
        Supplier<Component> tooltip,
        Runnable onClick,
        BooleanSupplier visible,
        Supplier<@Nullable ButtonBadge> badge,
        int order,
        boolean keyMapping) {

    public static final int DEFAULT_ORDER = 1000;

    public ButtonSpec {
        Objects.requireNonNull(icon, "icon");
        Objects.requireNonNull(tooltip, "tooltip");
        Objects.requireNonNull(onClick, "onClick");
        Objects.requireNonNull(visible, "visible");
        Objects.requireNonNull(badge, "badge");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private @Nullable ButtonIcon icon;
        private Supplier<Component> tooltip = Component::empty;
        private Runnable onClick = () -> {};
        private BooleanSupplier visible = () -> true;
        private Supplier<@Nullable ButtonBadge> badge = () -> null;
        private int order = DEFAULT_ORDER;
        private boolean keyMapping;

        private Builder() {}

        public Builder icon(ButtonIcon icon) {
            this.icon = icon;
            return this;
        }

        // A GUI sprite id, e.g. hibiscusquests:button/quests (textures/gui/sprites/button/quests.png).
        public Builder sprite(Identifier sprite) {
            return icon(ButtonIcon.sprite(sprite));
        }

        public Builder item(Supplier<ItemStack> stack) {
            return icon(ButtonIcon.item(stack));
        }

        public Builder tooltip(Component tooltip) {
            return tooltip(() -> tooltip);
        }

        public Builder tooltip(Supplier<Component> tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder onClick(Runnable onClick) {
            this.onClick = onClick;
            return this;
        }

        public Builder visibleWhen(BooleanSupplier visible) {
            this.visible = visible;
            return this;
        }

        public Builder badge(Supplier<@Nullable ButtonBadge> badge) {
            this.badge = badge;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        // Gives the button an unbound key mapping, named key.<namespace>.<path> (add that to your lang file).
        public Builder withKeyMapping() {
            this.keyMapping = true;
            return this;
        }

        public ButtonSpec build() {
            if (icon == null) {
                throw new IllegalStateException("A sidebar button needs an icon");
            }
            return new ButtonSpec(icon, tooltip, onClick, visible, badge, order, keyMapping);
        }
    }
}
