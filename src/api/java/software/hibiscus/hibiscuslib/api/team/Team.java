/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * A team: a player's own solo team, a party of players, or a server team. Get one from {@link TeamsApi#get()} on the
 * server or {@link ClientTeams} on the client.
 *
 * <p>Every method works for solo teams (the only kind there is when Hibiscus Teams isn't installed): a solo team's
 * id is its player's UUID, its only member is that player as {@link TeamRank#OWNER}, and it has no allies.
 *
 * <p>Teams on the server are live: a team you hold reflects later changes. Don't keep one across ticks expecting a
 * player's membership to stay put; ask {@link TeamManager#teamOf(UUID)} again.
 */
public interface Team {
    /**
     * The team's id: stable for the team's lifetime, and the player's UUID for a solo team.
     *
     * @return the id
     */
    UUID id();

    /**
     * What kind of team this is.
     *
     * @return the type
     */
    TeamType type();

    /**
     * The team's name for display, already styled as the team wants it shown.
     *
     * @return the display name
     */
    Component displayName();

    /**
     * A short plain name (no spaces or formatting), for commands, file names and tight spaces. A solo team's is its
     * player's name.
     *
     * @return the short name
     */
    String shortName();

    /**
     * The team's colour, {@code 0xRRGGBB}.
     *
     * @return the colour
     */
    int color();

    /**
     * The player who owns the team; empty for server teams.
     *
     * @return the owner's UUID, if any
     */
    Optional<UUID> owner();

    /**
     * The team's members and their ranks ({@link TeamRank#MEMBER} or above), in no particular order. Invited
     * players, allies and enemies aren't members and aren't listed.
     *
     * @return an unmodifiable map of member UUID to rank
     */
    Map<UUID, TeamRank> members();

    /**
     * The ids of the teams allied with this one.
     *
     * @return an unmodifiable set of team ids
     */
    Set<UUID> allies();

    /**
     * Where a player stands with this team: their member rank if they're a member, else {@link TeamRank#INVITED},
     * {@link TeamRank#ALLY} (a member of an allied team), {@link TeamRank#ENEMY} or {@link TeamRank#NONE}.
     *
     * @param player the player's UUID
     * @return the player's rank
     */
    TeamRank rankOf(UUID player);

    /**
     * Whether a player is a member of this team.
     *
     * @param player the player's UUID
     * @return true if {@code rankOf(player)} is {@link TeamRank#MEMBER} or above
     */
    default boolean isMember(UUID player) {
        return rankOf(player).isMember();
    }

    /**
     * Whether another team is allied with this one. A team counts as allied with itself.
     *
     * @param teamId the other team's id
     * @return true if allied
     */
    default boolean isAlly(UUID teamId) {
        return id().equals(teamId) || allies().contains(teamId);
    }

    /**
     * The team's members who are online, for syncing data to them. Empty on the client.
     *
     * @param server the running server
     * @return the online members
     */
    List<ServerPlayer> onlineMembers(MinecraftServer server);

    /**
     * Storage that another mod keeps with this team, saved with it: for example Hibiscus Quests' progress. Each mod
     * gets its own tag, under its own id; change the tag in place, then call {@link #markDirty()} so it's saved.
     *
     * <p>Server-side only: on the client, and for a team that doesn't exist any more, it's an empty tag that isn't
     * kept. Nothing here is synced to clients; send what they need yourself.
     *
     * @param owningMod an id in your mod's namespace, e.g. {@code hibiscusquests:progress}
     * @return the live tag (created empty the first time)
     */
    CompoundTag extraData(Identifier owningMod);

    /** Marks the team as changed, so it (and its {@link #extraData(Identifier)}) is saved with the world. */
    void markDirty();
}
