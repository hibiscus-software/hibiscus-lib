/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.world.entity.player.Player;

/**
 * The server's teams. Get it from {@link TeamsApi#get()}; it's only usable while a server is running (on a client in
 * single-player, from the integrated server's thread).
 */
public interface TeamManager {
    /**
     * The team a player belongs to now: their party if they're in one, else their own solo team. Never empty, so
     * per-team data can always be keyed by {@code teamOf(player).id()}.
     *
     * @param player the player's UUID (they needn't be online)
     * @return the player's current team
     */
    Team teamOf(UUID player);

    /**
     * {@link #teamOf(UUID)} for a player entity.
     *
     * @param player the player
     * @return the player's current team
     */
    default Team teamOf(Player player) {
        return teamOf(player.getUUID());
    }

    /**
     * A team by id.
     *
     * @param teamId the team's id (a player's UUID for their solo team)
     * @return the team, or empty if there's no such team
     */
    Optional<Team> team(UUID teamId);

    /**
     * Every team the server knows: parties, server teams and the solo teams of players who have joined.
     *
     * @return an unmodifiable snapshot
     */
    Collection<? extends Team> allTeams();

    /**
     * A player's own solo team, even while they're in a party (where their {@link #teamOf(UUID)} is the party).
     *
     * @param player the player's UUID
     * @return the player's solo team
     */
    Team playerTeam(UUID player);

    /**
     * Whether two players are on the same team now.
     *
     * @param a one player's UUID
     * @param b the other player's UUID
     * @return true if their current teams are the same
     */
    default boolean sameTeam(UUID a, UUID b) {
        return teamOf(a).id().equals(teamOf(b).id());
    }
}
