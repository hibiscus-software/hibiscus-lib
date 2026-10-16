/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.button;

import java.util.LinkedHashMap;
import java.util.Map;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;

// Key mappings for sidebar buttons that asked for one: unbound by default, in the shared "Hibiscus" category
// (key.category.hibiscuslib.hibiscus), and pressing one in game does what clicking the button does.
public final class ButtonKeys {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(HibiscusLib.id("hibiscus"));

    private static final Map<KeyMapping, ButtonSpec> MAPPINGS = new LinkedHashMap<>();

    private ButtonKeys() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ButtonKeys::registerMappings);
        NeoForge.EVENT_BUS.addListener(ButtonKeys::onTick);
    }

    private static void registerMappings(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        HibiscusButtons.collectKeyMappings().forEach((id, spec) -> {
            KeyMapping mapping = new KeyMapping(name(id), InputConstants.Type.KEYBOARD, InputConstants.UNKNOWN.getValue(), CATEGORY);
            event.register(mapping);
            MAPPINGS.put(mapping, spec);
        });
    }

    // The lang key of a button's key mapping: key.<namespace>.<path>, with slashes as dots.
    public static String name(Identifier id) {
        return "key." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        MAPPINGS.forEach((mapping, spec) -> {
            while (mapping.consumeClick()) {
                if (spec.visible().getAsBoolean()) {
                    spec.onClick().run();
                }
            }
        });
    }
}
