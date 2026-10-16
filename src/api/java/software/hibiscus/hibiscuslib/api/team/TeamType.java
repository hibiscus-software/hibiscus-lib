/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

/** What kind of team a {@link Team} is. */
public enum TeamType {
    /**
     * A player's own single-member team. Every player always has one, with the player's UUID as its id, even while
     * they're in a party; it's their team whenever they aren't in one.
     */
    PLAYER,
    /** A team of players that they created and joined together. */
    PARTY,
    /** A team run by the server (admins), not owned by a player: event teams, the server's own shared progress. */
    SERVER;

    /**
     * Whether this is a player's own solo team.
     *
     * @return true for {@link #PLAYER}
     */
    public boolean isPlayer() {
        return this == PLAYER;
    }
}
