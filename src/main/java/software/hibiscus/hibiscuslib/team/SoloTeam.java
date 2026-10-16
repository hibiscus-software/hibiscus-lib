/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.team;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import software.hibiscus.hibiscuslib.api.team.Team;
import software.hibiscus.hibiscuslib.api.team.TeamRank;
import software.hibiscus.hibiscuslib.api.team.TeamType;

// A player's own team in the fallback (no teams mod installed): id = the player's UUID, the player its only member
// and owner, no allies. Cheap to make, so the manager makes one per call; two for the same player are equal.
// On the server it reads and writes extraData through the SoloTeamStore; on the client (store null) extraData is an
// empty tag that isn't kept.
public final class SoloTeam implements Team {
    // Team colours, picked from the player's UUID so each player keeps one.
    static final int[] COLORS = {
            0xF2557A, 0xE5484D, 0xF28B30, 0xE8B04B, 0xF2C14E, 0x8BD346, 0x4FC78A, 0x2EC4B6,
            0x3AB7E8, 0x4C7BF2, 0x6C5CE7, 0xA363D9, 0xF07AC0, 0x9C6B4E
    };

    private final UUID player;
    private final String name;
    private final @Nullable SoloTeamStore store;

    public SoloTeam(UUID player, String name, @Nullable SoloTeamStore store) {
        this.player = player;
        this.name = name;
        this.store = store;
    }

    @Override
    public UUID id() {
        return player;
    }

    @Override
    public TeamType type() {
        return TeamType.PLAYER;
    }

    @Override
    public Component displayName() {
        return Component.literal(name).withColor(color());
    }

    @Override
    public String shortName() {
        return name;
    }

    @Override
    public int color() {
        return COLORS[Math.floorMod(player.hashCode(), COLORS.length)];
    }

    @Override
    public Optional<UUID> owner() {
        return Optional.of(player);
    }

    @Override
    public Map<UUID, TeamRank> members() {
        return Map.of(player, TeamRank.OWNER);
    }

    @Override
    public Set<UUID> allies() {
        return Set.of();
    }

    @Override
    public TeamRank rankOf(UUID other) {
        return player.equals(other) ? TeamRank.OWNER : TeamRank.NONE;
    }

    @Override
    public List<ServerPlayer> onlineMembers(MinecraftServer server) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        return online != null ? List.of(online) : List.of();
    }

    @Override
    public CompoundTag extraData(Identifier owningMod) {
        return store != null ? store.data(player, name, owningMod) : new CompoundTag();
    }

    @Override
    public void markDirty() {
        if (store != null) {
            store.setDirty();
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SoloTeam team && team.player.equals(player);
    }

    @Override
    public int hashCode() {
        return player.hashCode();
    }

    @Override
    public String toString() {
        return "SoloTeam[" + name + ", " + player + "]";
    }
}
