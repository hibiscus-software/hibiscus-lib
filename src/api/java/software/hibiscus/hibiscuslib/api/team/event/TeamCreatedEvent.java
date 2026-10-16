/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Optional;
import java.util.UUID;

import software.hibiscus.hibiscuslib.api.team.Team;

/**
 * A team was created: a party or server team, or a player's solo team the first time the player joins the server.
 * Posted after the team exists, so {@code TeamsApi.get().team(id)} finds it.
 */
public class TeamCreatedEvent extends TeamEvent {
    private final UUID creator;

    /**
     * Creates the event.
     *
     * @param team    the new team
     * @param creator the player who created it, or null (solo teams, server teams made from the console)
     */
    public TeamCreatedEvent(Team team, UUID creator) {
        super(team);
        this.creator = creator;
    }

    /**
     * The player who created the team.
     *
     * @return their UUID, or empty for teams nobody created by hand
     */
    public Optional<UUID> creator() {
        return Optional.ofNullable(creator);
    }
}
