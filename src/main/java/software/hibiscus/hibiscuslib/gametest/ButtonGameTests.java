/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;

// The sidebar button registry's ordering and hidden list. The registry is common code, so it's tested on the server;
// the widgets themselves are client-only and checked in game.
final class ButtonGameTests {
    private ButtonGameTests() {}

    private static ButtonSpec spec(int order) {
        return ButtonSpec.builder().item(() -> ItemStack.EMPTY).tooltip(Component.literal("test")).order(order).build();
    }

    private static Identifier id(String path) {
        return HibiscusLibGameTests.id("test/" + path);
    }

    private static Map<Identifier, ButtonSpec> sample() {
        Map<Identifier, ButtonSpec> buttons = new LinkedHashMap<>();
        buttons.put(id("c"), spec(30));
        buttons.put(id("a"), spec(10));
        buttons.put(id("b"), spec(20));
        buttons.put(id("d"), spec(10));
        return buttons;
    }

    private static void expect(GameTestHelper helper, List<Identifier> expected, List<Identifier> actual) {
        if (!expected.equals(actual)) {
            helper.fail("expected " + expected + ", got " + actual);
        }
    }

    // No config: by each spec's order, ties by id.
    static void defaultOrder(GameTestHelper helper) {
        expect(helper, List.of(id("a"), id("d"), id("b"), id("c")), HibiscusButtons.arrange(sample(), List.of(), List.of()));
        helper.succeed();
    }

    // buttons.order comes first, in its order, then the rest by default order.
    static void configOrderFirst(GameTestHelper helper) {
        List<String> order = List.of(id("c").toString(), id("b").toString());
        expect(helper, List.of(id("c"), id("b"), id("a"), id("d")), HibiscusButtons.arrange(sample(), order, List.of()));
        helper.succeed();
    }

    // buttons.hidden wins over buttons.order.
    static void hiddenLeftOut(GameTestHelper helper) {
        List<String> order = List.of(id("c").toString(), id("b").toString());
        List<String> hidden = List.of(id("c").toString(), id("a").toString());
        expect(helper, List.of(id("b"), id("d")), HibiscusButtons.arrange(sample(), order, hidden));
        helper.succeed();
    }

    // Unknown, malformed and repeated ids in the config are skipped rather than breaking the bar.
    static void badIdsIgnored(GameTestHelper helper) {
        List<String> order = List.of("not an id!", id("missing").toString(), id("d").toString(), id("d").toString());
        List<String> hidden = List.of("::", id("also_missing").toString());
        expect(helper, List.of(id("d"), id("a"), id("b"), id("c")), HibiscusButtons.arrange(sample(), order, hidden));
        helper.succeed();
    }

    static void duplicateRejected(GameTestHelper helper) {
        Identifier id = id("duplicate");
        if (HibiscusButtons.get(id) == null) {
            HibiscusButtons.register(id, spec(1));
        }
        try {
            HibiscusButtons.register(id, spec(2));
            helper.fail("registering " + id + " twice should throw");
        } catch (IllegalArgumentException expected) {
            helper.succeed();
        }
    }

    // A data-driven button with a registered button's id doesn't replace it.
    static void codeWinsOverData(GameTestHelper helper) {
        Identifier id = id("shared");
        ButtonSpec code = spec(5);
        if (HibiscusButtons.get(id) == null) {
            HibiscusButtons.register(id, code);
        }
        ButtonSpec registered = HibiscusButtons.get(id);
        Map<Identifier, ButtonSpec> data = Map.of(id, spec(99), id("data_only"), spec(7));
        HibiscusButtons.setDataButtons(data);
        try {
            Map<Identifier, ButtonSpec> all = HibiscusButtons.all();
            if (all.get(id) != registered) {
                helper.fail("the data file replaced the registered button");
            } else if (!all.containsKey(id("data_only"))) {
                helper.fail("the data-only button is missing");
            } else {
                helper.succeed();
            }
        } finally {
            HibiscusButtons.setDataButtons(Map.of());
        }
    }
}
