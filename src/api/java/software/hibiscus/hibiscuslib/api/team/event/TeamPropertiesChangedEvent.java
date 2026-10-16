/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Set;

import software.hibiscus.hibiscuslib.api.team.Team;

/** A team's name, colour, owner, ranks or other settings changed. Posted after the change. */
public class TeamPropertiesChangedEvent extends TeamEvent {
    /** What about a team can change. */
    public enum Property {
        /** {@link Team#displayName()}. */
        DISPLAY_NAME,
        /** {@link Team#shortName()}. */
        SHORT_NAME,
        /** {@link Team#color()}. */
        COLOR,
        /** {@link Team#owner()}. */
        OWNER,
        /** A member's rank. */
        RANKS,
        /** Anything else the teams mod keeps (description, privacy, icon). */
        OTHER
    }

    private final Set<Property> changed;

    /**
     * Creates the event.
     *
     * @param team    the team
     * @param changed what changed
     */
    public TeamPropertiesChangedEvent(Team team, Set<Property> changed) {
        super(team);
        this.changed = Set.copyOf(changed);
    }

    /**
     * What changed.
     *
     * @return an unmodifiable set
     */
    public Set<Property> changed() {
        return changed;
    }
}
