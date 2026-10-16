/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Map;
import java.util.UUID;

import software.hibiscus.hibiscuslib.api.team.Team;
import software.hibiscus.hibiscuslib.api.team.TeamRank;

/**
 * A party or server team is about to be deleted (disbanded). Posted before it goes, so its {@link Team#extraData}
 * can still be read, for example to hand progress back to the members' solo teams. Each member then gets a
 * {@link PlayerLeftTeamEvent} and a {@link PlayerJoinedTeamEvent} back to their solo team.
 */
public class TeamDeletedEvent extends TeamEvent {
    private final Map<UUID, TeamRank> formerMembers;

    /**
     * Creates the event.
     *
     * @param team          the team being deleted
     * @param formerMembers its members at the time, with their ranks
     */
    public TeamDeletedEvent(Team team, Map<UUID, TeamRank> formerMembers) {
        super(team);
        this.formerMembers = Map.copyOf(formerMembers);
    }

    /**
     * The team's members when it was deleted.
     *
     * @return an unmodifiable map of member UUID to rank
     */
    public Map<UUID, TeamRank> formerMembers() {
        return formerMembers;
    }
}
