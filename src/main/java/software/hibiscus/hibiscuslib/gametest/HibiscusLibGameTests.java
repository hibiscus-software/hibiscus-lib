/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.gametest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.util.PermissionLevels;

// Registers every Hibiscus Lib gametest. All run in one batch on an empty structure; ./gradlew runGameTestServer runs
// them and exits, failing the build if any required test fails.
public final class HibiscusLibGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, HibiscusLib.MODID);

    // Permission nodes for UtilGameTests, declared like a mod would (registered when the server starts).
    static final PermissionNode<Boolean> TEST_NODE_ALL = PermissionLevels.node(HibiscusLib.MODID, "gametest_all", PermissionLevel.ALL);
    static final PermissionNode<Boolean> TEST_NODE_OPS = PermissionLevels.node(HibiscusLib.MODID, "gametest_ops", PermissionLevel.GAMEMASTERS);

    // Every test, by name.
    private static final Map<String, Consumer<GameTestHelper>> TESTS = new LinkedHashMap<>();

    static {
        TESTS.put("buttons_default_order", ButtonGameTests::defaultOrder);
        TESTS.put("buttons_config_order_first", ButtonGameTests::configOrderFirst);
        TESTS.put("buttons_hidden_left_out", ButtonGameTests::hiddenLeftOut);
        TESTS.put("buttons_bad_ids_ignored", ButtonGameTests::badIdsIgnored);
        TESTS.put("buttons_duplicate_rejected", ButtonGameTests::duplicateRejected);
        TESTS.put("buttons_code_wins_over_data", ButtonGameTests::codeWinsOverData);
        TESTS.put("buttons_layout_wraps", ButtonGameTests::layoutWraps);
        TESTS.put("buttons_layout_narrow_grows_taller", ButtonGameTests::layoutNarrowGrowsTaller);
        TESTS.put("buttons_layout_drops_overflow", ButtonGameTests::layoutDropsOverflow);
        TESTS.put("buttons_layout_bottom_left", ButtonGameTests::layoutBottomLeft);
        TESTS.put("teams_fallback_installed", TeamsGameTests::fallbackInstalled);
        TESTS.put("teams_solo_team_shape", TeamsGameTests::soloTeamShape);
        TESTS.put("teams_unknown_team_empty", TeamsGameTests::unknownTeamEmpty);
        TESTS.put("teams_extra_data_persists", TeamsGameTests::extraDataPersists);
        TESTS.put("teams_created_event_on_first_join", TeamsGameTests::createdEventOnFirstJoin);
        TESTS.put("util_snbt_round_trip", UtilGameTests::snbtRoundTrip);
        TESTS.put("util_rate_limiter", UtilGameTests::rateLimiter);
        TESTS.put("util_profiles", UtilGameTests::profiles);
        TESTS.put("util_permissions", UtilGameTests::permissions);
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
    }

    private HibiscusLibGameTests() {}

    public static void register(IEventBus modEventBus) {
        FUNCTIONS.register(modEventBus);
        modEventBus.addListener(HibiscusLibGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment = event.registerEnvironment(id("default"));
        TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(environment, Level.OVERWORLD, Identifier.withDefaultNamespace("empty"),
                100, 0, true, Rotation.NONE, false, 1, 1, false, 8);
        TESTS.keySet().forEach(name -> event.registerTest(id(name), new FunctionGameTestInstance(function(name), data)));
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(HibiscusLib.MODID, path);
    }

    private static ResourceKey<Consumer<GameTestHelper>> function(String path) {
        return ResourceKey.create(Registries.TEST_FUNCTION, id(path));
    }
}
