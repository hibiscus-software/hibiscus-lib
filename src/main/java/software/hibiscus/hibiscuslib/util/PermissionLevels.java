/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

/**
 * Permission checks for Hibiscus mods: vanilla op levels, and NeoForge permission nodes whose default is an op level,
 * so a permissions mod (LuckPerms and the like) can grant or deny them while servers without one get sensible
 * defaults.
 *
 * <p>Declare nodes in a static field, e.g. {@code static final PermissionNode<Boolean> EDIT =
 * PermissionLevels.node("hibiscusquests", "edit", PermissionLevel.GAMEMASTERS);}, and make sure the class is loaded
 * during mod construction (reference it from your constructor): nodes are registered with NeoForge when the server
 * starts.
 */
public final class PermissionLevels {
    private static final List<PermissionNode<?>> NODES = new ArrayList<>();

    private PermissionLevels() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PermissionLevels::gatherNodes);
    }

    private static void gatherNodes(PermissionGatherEvent.Nodes event) {
        synchronized (NODES) {
            event.addNodes(NODES);
        }
    }

    /**
     * Declares a yes/no permission node, {@code <modId>.<name>}, granted by default to players with at least
     * {@code defaultLevel} ({@link PermissionLevel#ALL} for everyone).
     */
    public static PermissionNode<Boolean> node(String modId, String name, PermissionLevel defaultLevel) {
        PermissionNode<Boolean> node = new PermissionNode<>(modId, name, PermissionTypes.BOOLEAN,
                (player, playerId, context) -> player != null ? hasLevel(player, defaultLevel) : offlineHasLevel(playerId, defaultLevel));
        synchronized (NODES) {
            NODES.add(node);
        }
        return node;
    }

    // Whether an online player has a node (through the permissions mod, or the node's default).
    public static boolean check(ServerPlayer player, PermissionNode<Boolean> node) {
        return PermissionAPI.getPermission(player, node);
    }

    // Whether an offline player has a node; permissions mods may answer differently for offline players.
    public static boolean checkOffline(UUID player, PermissionNode<Boolean> node) {
        return PermissionAPI.getOfflinePermission(player, node);
    }

    // Whether an online player has at least a vanilla op level.
    public static boolean hasLevel(ServerPlayer player, PermissionLevel level) {
        return level == PermissionLevel.ALL || player.permissions().hasPermission(new Permission.HasCommandLevel(level));
    }

    private static boolean offlineHasLevel(UUID playerId, PermissionLevel level) {
        if (level == PermissionLevel.ALL) {
            return true;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return false;
        }
        return Profiles.nameOf(server, playerId).map(name -> {
            LevelBasedPermissionSet permissions = server.getProfilePermissions(new NameAndId(playerId, name));
            return permissions.level().isEqualOrHigherThan(level);
        }).orElse(false);
    }

    // For Brigadier .requires(...): players need the node; the console and command blocks need the node's level.
    public static Predicate<CommandSourceStack> requires(PermissionNode<Boolean> node, PermissionLevel consoleLevel) {
        return source -> {
            ServerPlayer player = source.getPlayer();
            if (player != null) {
                return check(player, node);
            }
            return source.permissions().hasPermission(new Permission.HasCommandLevel(consoleLevel));
        };
    }
}
