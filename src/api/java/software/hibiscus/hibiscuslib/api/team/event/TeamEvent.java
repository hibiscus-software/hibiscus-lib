/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import net.neoforged.bus.api.Event;
import software.hibiscus.hibiscuslib.api.team.Team;

/**
 * Base for the Teams API's events, posted on {@code NeoForge.EVENT_BUS} on the server thread by the installed teams
 * mod. Hibiscus Lib's solo-team fallback only posts {@link TeamCreatedEvent}, since solo teams never change.
 */
public abstract class TeamEvent extends Event {
    private final Team team;

    /**
     * Creates the event.
     *
     * @param team the team the event is about
     */
    protected TeamEvent(Team team) {
        this.team = team;
    }

    /**
     * The team the event is about.
     *
     * @return the team
     */
    public Team team() {
        return team;
    }
}
