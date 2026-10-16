/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.button;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import software.hibiscus.hibiscuslib.HibiscusLib;

/**
 * The sidebar button registry: the column of icon buttons next to the player and creative inventories that every
 * Hibiscus mod opens its screens from.
 *
 * <p>Register buttons on the client, from your client mod constructor (needed for {@link ButtonSpec#keyMapping()}) or
 * from {@code FMLClientSetupEvent}. Nothing here loads client classes, so calling it from common code is harmless, but
 * the buttons only ever show on a client.
 *
 * <p>Resource packs and modpacks can add buttons too, in {@code assets/<ns>/hibiscus_buttons/<id>.json}; those run a
 * command or an action registered with {@link #registerAction(Identifier, Runnable)}.
 */
public final class HibiscusButtons {
    private static final Map<Identifier, ButtonSpec> CODE_BUTTONS = new LinkedHashMap<>();
    private static final Map<Identifier, Runnable> ACTIONS = new HashMap<>();
    // Replaced whole on each resource reload.
    private static volatile Map<Identifier, ButtonSpec> dataButtons = Map.of();
    // Set once key mappings have been collected; later buttons can't get one.
    private static volatile boolean keyMappingsCollected;

    private HibiscusButtons() {}

    /**
     * Registers a sidebar button.
     *
     * @throws IllegalArgumentException if a button with that id is already registered
     */
    public static synchronized void register(Identifier id, ButtonSpec spec) {
        if (CODE_BUTTONS.containsKey(id)) {
            throw new IllegalArgumentException("Sidebar button " + id + " is already registered");
        }
        if (spec.keyMapping() && keyMappingsCollected) {
            HibiscusLib.LOGGER.warn("Sidebar button {} asked for a key mapping after key mappings were collected; "
                    + "register it during mod construction to get one", id);
        }
        CODE_BUTTONS.put(id, spec);
    }

    /** Registers an action that data-driven buttons can run with {@code "action": "<id>"}. */
    public static synchronized void registerAction(Identifier id, Runnable action) {
        if (ACTIONS.putIfAbsent(id, action) != null) {
            throw new IllegalArgumentException("Sidebar button action " + id + " is already registered");
        }
    }

    public static synchronized Optional<Runnable> action(Identifier id) {
        return Optional.ofNullable(ACTIONS.get(id));
    }

    /** Every button, code-registered and data-driven; a code button wins over a data file with the same id. */
    public static synchronized Map<Identifier, ButtonSpec> all() {
        Map<Identifier, ButtonSpec> all = new LinkedHashMap<>(dataButtons);
        all.putAll(CODE_BUTTONS);
        return all;
    }

    public static @Nullable ButtonSpec get(Identifier id) {
        synchronized (HibiscusButtons.class) {
            ButtonSpec spec = CODE_BUTTONS.get(id);
            return spec != null ? spec : dataButtons.get(id);
        }
    }

    /** The code-registered buttons that asked for a key mapping, and marks key mappings as collected. */
    public static synchronized Map<Identifier, ButtonSpec> collectKeyMappings() {
        keyMappingsCollected = true;
        Map<Identifier, ButtonSpec> withKeys = new LinkedHashMap<>();
        CODE_BUTTONS.forEach((id, spec) -> {
            if (spec.keyMapping()) {
                withKeys.put(id, spec);
            }
        });
        return withKeys;
    }

    // Called by the data-driven button loader after each resource reload.
    public static synchronized void setDataButtons(Map<Identifier, ButtonSpec> buttons) {
        for (Identifier id : buttons.keySet()) {
            if (CODE_BUTTONS.containsKey(id)) {
                HibiscusLib.LOGGER.warn("hibiscus_buttons/{}.json has the same id as a registered button, which wins", id);
            }
        }
        dataButtons = Map.copyOf(buttons);
    }

    /**
     * Puts buttons in the order they're drawn: first the ids in {@code order} (the player's {@code buttons.order}),
     * in that order, then the rest by {@link ButtonSpec#order()} and then id. Ids in {@code hidden} are left out, as
     * are ids in either list that aren't registered. Visibility predicates aren't checked here.
     */
    public static List<Identifier> arrange(Map<Identifier, ButtonSpec> buttons, Collection<String> order, Collection<String> hidden) {
        Set<Identifier> hiddenIds = parseAll(hidden);
        List<Identifier> arranged = new ArrayList<>();
        Set<Identifier> placed = new HashSet<>();
        for (Identifier id : parseAll(order)) {
            if (buttons.containsKey(id) && !hiddenIds.contains(id) && placed.add(id)) {
                arranged.add(id);
            }
        }
        buttons.entrySet().stream()
                .filter(entry -> !placed.contains(entry.getKey()) && !hiddenIds.contains(entry.getKey()))
                .sorted(Comparator.<Map.Entry<Identifier, ButtonSpec>>comparingInt(entry -> entry.getValue().order())
                        .thenComparing(entry -> entry.getKey().toString()))
                .forEach(entry -> arranged.add(entry.getKey()));
        return arranged;
    }

    // Ids from config strings, in order, skipping any that don't parse.
    private static Set<Identifier> parseAll(Collection<String> ids) {
        Set<Identifier> parsed = new java.util.LinkedHashSet<>();
        for (String id : ids) {
            Identifier identifier = Identifier.tryParse(id.trim());
            if (identifier != null) {
                parsed.add(identifier);
            }
        }
        return parsed;
    }
}
