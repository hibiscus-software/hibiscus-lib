/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

/**
 * Where a player stands with a team, lowest first: {@code ENEMY < NONE < ALLY < INVITED < MEMBER < OFFICER < OWNER}.
 * Compare with {@link #isAtLeast(TeamRank)}, e.g. {@code rank.isAtLeast(TeamRank.OFFICER)} for "may invite".
 *
 * <p>{@link #MEMBER} and above are members of the team; {@link #INVITED}, {@link #ALLY}, {@link #NONE} and
 * {@link #ENEMY} are not.
 */
public enum TeamRank {
    /** Marked as an enemy of the team. */
    ENEMY,
    /** No relation to the team. */
    NONE,
    /** A member of an allied team, or a player the team trusts as an ally. */
    ALLY,
    /** Invited to join but hasn't yet. */
    INVITED,
    /** A member. */
    MEMBER,
    /** A member who can manage the team (invite, kick members, edit settings). */
    OFFICER,
    /** The team's owner. */
    OWNER;

    /**
     * Whether this rank is the given rank or higher.
     *
     * @param rank the rank to compare with
     * @return true if this rank is {@code rank} or above it
     */
    public boolean isAtLeast(TeamRank rank) {
        return ordinal() >= rank.ordinal();
    }

    /**
     * Whether this rank makes the player a member of the team.
     *
     * @return true for {@link #MEMBER}, {@link #OFFICER} and {@link #OWNER}
     */
    public boolean isMember() {
        return isAtLeast(MEMBER);
    }
}
