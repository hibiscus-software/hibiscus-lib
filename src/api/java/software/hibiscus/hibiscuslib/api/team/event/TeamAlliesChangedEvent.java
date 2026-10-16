/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Set;
import java.util.UUID;

import software.hibiscus.hibiscuslib.api.team.Team;

/** Teams were added to or removed from a team's allies. Posted after the change. */
public class TeamAlliesChangedEvent extends TeamEvent {
    private final Set<UUID> added;
    private final Set<UUID> removed;

    /**
     * Creates the event.
     *
     * @param team    the team whose allies changed
     * @param added   the ids of teams that became allies
     * @param removed the ids of teams that stopped being allies
     */
    public TeamAlliesChangedEvent(Team team, Set<UUID> added, Set<UUID> removed) {
        super(team);
        this.added = Set.copyOf(added);
        this.removed = Set.copyOf(removed);
    }

    /**
     * Teams that became allies.
     *
     * @return an unmodifiable set of team ids
     */
    public Set<UUID> added() {
        return added;
    }

    /**
     * Teams that stopped being allies.
     *
     * @return an unmodifiable set of team ids
     */
    public Set<UUID> removed() {
        return removed;
    }
}
