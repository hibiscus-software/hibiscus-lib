/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.util;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

/**
 * Player name and UUID lookups on the server, for offline players too: online players first, then the server's name
 * cache (usercache.json, every player who has joined recently). Nothing here asks Mojang's servers, so it's safe on
 * the server thread.
 */
public final class Profiles {
    private Profiles() {}

    // A player's name, if the server knows it.
    public static Optional<String> nameOf(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return Optional.of(online.nameAndId().name());
        }
        return server.services().nameToIdCache().get(player).map(NameAndId::name);
    }

    // A player's UUID by name (any case), if the server knows it.
    public static Optional<UUID> idOf(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return Optional.of(online.getUUID());
        }
        return server.services().nameToIdCache().get(name).map(NameAndId::id);
    }

    // idOf, or on an offline-mode server the UUID the player would get (so a name can be used before they've joined).
    public static Optional<UUID> idOrOffline(MinecraftServer server, String name) {
        Optional<UUID> known = idOf(server, name);
        if (known.isPresent() || server.usesAuthentication()) {
            return known;
        }
        return Optional.of(UUIDUtil.createOfflinePlayerUUID(name));
    }

    // A name to show: the player's name, else the start of their UUID.
    public static Component displayName(MinecraftServer server, UUID player) {
        return Component.literal(nameOf(server, player).orElseGet(() -> player.toString().substring(0, 8)));
    }
}
