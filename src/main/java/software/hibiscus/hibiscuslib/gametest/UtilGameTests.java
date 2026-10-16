/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.gametest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import software.hibiscus.hibiscuslib.util.PermissionLevels;
import software.hibiscus.hibiscuslib.util.Profiles;
import software.hibiscus.hibiscuslib.util.RateLimiter;
import software.hibiscus.hibiscuslib.util.SnbtFiles;

// SnbtFiles, RateLimiter, Profiles and PermissionLevels.
final class UtilGameTests {
    private UtilGameTests() {}

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    // Write, overwrite (keeping a .bak of the first), read back; a broken file fails to read rather than reading empty.
    static void snbtRoundTrip(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        Path dir = server.getServerDirectory().resolve("hibiscuslib-gametest-" + UUID.randomUUID());
        Path file = dir.resolve("sub").resolve("test.snbt");
        try {
            CompoundTag first = new CompoundTag();
            first.putString("name", "first");
            CompoundTag second = new CompoundTag();
            second.putString("name", "second");
            second.putInt("count", 3);
            ListTag list = new ListTag();
            list.add(StringTag.valueOf("a"));
            list.add(StringTag.valueOf("b"));
            second.put("list", list);

            check(helper, SnbtFiles.read(file).isEmpty(), "a missing file reads as empty");
            SnbtFiles.write(file, first);
            SnbtFiles.write(file, second);
            check(helper, SnbtFiles.read(file).orElseThrow().equals(second), "reads back what was written");
            Path backup = file.resolveSibling("test.snbt.bak");
            check(helper, Files.exists(backup) && SnbtFiles.read(backup).orElseThrow().equals(first), ".bak holds the previous version");
            check(helper, !Files.exists(file.resolveSibling("test.snbt.tmp")), "the temp file is moved into place");
            check(helper, Files.readString(file).contains("\n"), "written pretty-printed, over several lines");

            Path broken = dir.resolve("broken.snbt");
            Files.writeString(broken, "{name: \"unterminated", StandardCharsets.UTF_8);
            try {
                SnbtFiles.read(broken);
                helper.fail("reading invalid SNBT should throw");
            } catch (IOException expected) {
                // Expected.
            }
            helper.succeed();
        } catch (IOException e) {
            helper.fail("SNBT file test failed: " + e);
        } finally {
            deleteQuietly(dir);
        }
    }

    private static void deleteQuietly(Path dir) {
        try (var paths = Files.walk(dir)) {
            paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
            // Only test output; nothing to do if it can't be removed.
        }
    }

    static void rateLimiter(GameTestHelper helper) {
        RateLimiter limiter = new RateLimiter(2, 1.0);
        UUID player = UUID.randomUUID();
        long start = 1_000_000_000L;
        check(helper, limiter.tryAcquire(player, start) && limiter.tryAcquire(player, start), "the burst is allowed");
        check(helper, !limiter.tryAcquire(player, start), "past the burst is refused");
        check(helper, limiter.tryAcquire(UUID.randomUUID(), start), "each player has their own bucket");
        check(helper, limiter.tryAcquire(player, start + 1_000_000_000L), "a token comes back after a second");
        check(helper, !limiter.tryAcquire(player, start + 1_000_000_000L), "but only one");
        helper.succeed();
    }

    static void profiles(GameTestHelper helper) {
        ServerPlayer player = TeamsGameTests.join(helper);
        try {
            MinecraftServer server = helper.getLevel().getServer();
            String name = player.nameAndId().name();
            check(helper, Profiles.nameOf(server, player.getUUID()).orElse("").equals(name), "nameOf an online player");
            check(helper, Profiles.idOf(server, name).orElse(null) != null, "idOf an online player's name");
            check(helper, Profiles.nameOf(server, UUID.randomUUID()).isEmpty(), "nameOf an unknown UUID is empty");
            check(helper, Profiles.displayName(server, player.getUUID()).getString().equals(name), "displayName");
            helper.succeed();
        } finally {
            TeamsGameTests.leave(helper, player);
        }
    }

    // A node's default follows its op level: everyone for ALL, ops only for GAMEMASTERS.
    static void permissions(GameTestHelper helper) {
        ServerPlayer player = TeamsGameTests.join(helper);
        try {
            check(helper, PermissionLevels.hasLevel(player, PermissionLevel.ALL), "everyone has level ALL");
            check(helper, !PermissionLevels.hasLevel(player, PermissionLevel.GAMEMASTERS), "a non-op lacks GAMEMASTERS");
            check(helper, PermissionLevels.check(player, HibiscusLibGameTests.TEST_NODE_ALL), "an ALL node is granted to everyone");
            check(helper, !PermissionLevels.check(player, HibiscusLibGameTests.TEST_NODE_OPS), "a GAMEMASTERS node is refused to a non-op");
            helper.succeed();
        } finally {
            TeamsGameTests.leave(helper, player);
        }
    }
}
