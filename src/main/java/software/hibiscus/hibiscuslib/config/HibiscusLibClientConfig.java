/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.config;

import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.EnumGetMethod;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.ModConfigSpec;
import software.hibiscus.hibiscuslib.button.ButtonLocation;

// Each player's own settings, in hibiscuslib-client.toml: the sidebar button bar and the debug widget gallery.
public final class HibiscusLibClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private HibiscusLibClientConfig() {}

    static {
        BUILDER.comment("The column of Hibiscus buttons next to the inventory. Right-click any button to edit them in game.")
                .push("buttons");
    }

    public static final ModConfigSpec.BooleanValue BUTTONS_ENABLED = BUILDER
            .comment("Show the button bar in the survival and creative inventories.")
            .define("enabled", true);

    public static final ModConfigSpec.EnumValue<ButtonLocation> BUTTONS_LOCATION = BUILDER
            .comment("Where the bar sits: top_left (a column left of the inventory, from its top), top_right (a column",
                    "right of the inventory), bottom_left (left of the inventory, from its bottom up) or left_column",
                    "(along the left edge of the screen).")
            .defineEnum("location", ButtonLocation.TOP_LEFT, EnumGetMethod.NAME_IGNORECASE);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BUTTONS_HIDDEN = BUILDER
            .comment("Button ids not to show, e.g. [\"hibiscusquests:quests\"].")
            .defineListAllowEmpty("hidden", List.of(), () -> "", HibiscusLibClientConfig::isId);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> BUTTONS_ORDER = BUILDER
            .comment("Button ids in the order to show them; buttons not listed follow in their default order.")
            .defineListAllowEmpty("order", List.of(), () -> "", HibiscusLibClientConfig::isId);

    static {
        BUILDER.pop();
        BUILDER.push("debug");
    }

    public static final ModConfigSpec.BooleanValue WIDGET_GALLERY = BUILDER
            .comment("Register the /hibiscuslib widgets client command (always on in a development environment).")
            .define("widgetGallery", false);

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean isId(Object value) {
        return value instanceof String string && Identifier.tryParse(string) != null;
    }

    public static List<String> hidden() {
        return new ArrayList<>(BUTTONS_HIDDEN.get());
    }

    public static List<String> order() {
        return new ArrayList<>(BUTTONS_ORDER.get());
    }

    // Saves the edit screen's changes to hibiscuslib-client.toml.
    public static void saveButtons(List<String> order, List<String> hidden) {
        BUTTONS_ORDER.set(List.copyOf(order));
        BUTTONS_HIDDEN.set(List.copyOf(hidden));
        SPEC.save();
    }
}
