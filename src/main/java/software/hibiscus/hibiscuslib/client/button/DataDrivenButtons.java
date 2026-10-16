/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.button;

import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;

/**
 * Loads sidebar buttons from resource packs: {@code assets/<ns>/hibiscus_buttons/<path>.json} becomes button
 * {@code <ns>:<path>}. A file looks like:
 *
 * <pre>{@code
 * {
 *   "icon": "minecraft:compass",            // an item id, or "sprite": "<ns>:<sprite>" instead
 *   "tooltip": "button.mypack.spawn",       // a lang key
 *   "command": "spawn",                     // a command to run (no leading slash), or "action": "<ns>:<id>"
 *   "order": 500                            // optional, default 1000
 * }
 * }</pre>
 */
public final class DataDrivenButtons extends SimplePreparableReloadListener<Map<Identifier, ButtonSpec>> {
    public static final Identifier ID = HibiscusLib.id("buttons");
    private static final FileToIdConverter FILES = FileToIdConverter.json("hibiscus_buttons");

    @Override
    protected Map<Identifier, ButtonSpec> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Identifier, ButtonSpec> buttons = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Resource> entry : FILES.listMatchingResources(manager).entrySet()) {
            Identifier id = FILES.fileToId(entry.getKey());
            try (Reader reader = entry.getValue().openAsReader()) {
                buttons.put(id, parse(id, JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                HibiscusLib.LOGGER.error("Couldn't load sidebar button {} from {}", id, entry.getKey(), e);
            }
        }
        return buttons;
    }

    @Override
    protected void apply(Map<Identifier, ButtonSpec> buttons, ResourceManager manager, ProfilerFiller profiler) {
        HibiscusButtons.setDataButtons(buttons);
    }

    static ButtonSpec parse(Identifier id, JsonObject json) {
        ButtonSpec.Builder builder = ButtonSpec.builder();
        if (json.has("sprite")) {
            builder.sprite(Identifier.parse(json.get("sprite").getAsString()));
        } else if (json.has("icon")) {
            Identifier itemId = Identifier.parse(json.get("icon").getAsString());
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            builder.item(() -> new ItemStack(item));
        } else {
            throw new IllegalArgumentException("needs an \"icon\" (item id) or a \"sprite\"");
        }
        String tooltip = json.has("tooltip") ? json.get("tooltip").getAsString() : "button." + id.getNamespace() + "." + id.getPath();
        builder.tooltip(Component.translatable(tooltip));
        if (json.has("order")) {
            builder.order(json.get("order").getAsInt());
        }
        if (json.has("command")) {
            String command = stripSlash(json.get("command").getAsString());
            builder.onClick(() -> runCommand(command));
        } else if (json.has("action")) {
            Identifier action = Identifier.parse(json.get("action").getAsString());
            builder.onClick(() -> HibiscusButtons.action(action).ifPresentOrElse(Runnable::run,
                    () -> HibiscusLib.LOGGER.warn("Sidebar button {} runs action {}, which isn't registered", id, action)));
        } else {
            throw new IllegalArgumentException("needs a \"command\" or an \"action\"");
        }
        return builder.build();
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    // Runs the command as the player typed it, so the server checks the player's own permissions.
    private static void runCommand(String command) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.connection.sendCommand(command);
        }
    }
}
