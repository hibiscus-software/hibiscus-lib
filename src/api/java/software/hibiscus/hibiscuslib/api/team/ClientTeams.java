/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * The client's copy of the teams, for screens: the local player's team and any team the server has told it about.
 * Read-only, and only as fresh as the last sync; the server decides what anyone may do.
 *
 * <p>Without a teams mod, {@link #myTeam()} is the local player's solo team. These methods are safe to call from
 * common code (they load no client classes), but only return teams on a client in a world.
 */
public final class ClientTeams {
    /** What a teams mod provides on the client. */
    public interface Provider {
        /**
         * The local player's current team.
         *
         * @return the team, or empty when not in a world
         */
        Optional<Team> myTeam();

        /**
         * A team the client knows about.
         *
         * @param teamId the team's id
         * @return the team, or empty if the client doesn't know it
         */
        Optional<Team> team(UUID teamId);

        /**
         * Every team the client knows about.
         *
         * @return an unmodifiable snapshot
         */
        default Collection<? extends Team> knownTeams() {
            return myTeam().map(List::of).orElse(List.of());
        }
    }

    private static final Provider NONE = new Provider() {
        @Override
        public Optional<Team> myTeam() {
            return Optional.empty();
        }

        @Override
        public Optional<Team> team(UUID teamId) {
            return Optional.empty();
        }
    };

    private static volatile Provider fallback = NONE;
    private static volatile Provider installed;

    private ClientTeams() {}

    private static Provider provider() {
        Provider provider = installed;
        return provider != null ? provider : fallback;
    }

    /**
     * The local player's current team.
     *
     * @return the team, or empty when not in a world (or on a dedicated server)
     */
    public static Optional<Team> myTeam() {
        return provider().myTeam();
    }

    /**
     * A team the client knows about (its own, and whatever the teams mod has synced, such as allies).
     *
     * @param teamId the team's id
     * @return the team, or empty if unknown
     */
    public static Optional<Team> team(UUID teamId) {
        return provider().team(teamId);
    }

    /**
     * Every team the client knows about.
     *
     * @return an unmodifiable snapshot
     */
    public static Collection<? extends Team> knownTeams() {
        return provider().knownTeams();
    }

    /**
     * Makes {@code provider} the client's source of teams, replacing the fallback. Call it once, on the client.
     *
     * @param provider the provider
     * @throws IllegalStateException if another mod already installed one
     */
    public static synchronized void install(Provider provider) {
        Objects.requireNonNull(provider, "provider");
        if (installed != null) {
            throw new IllegalStateException("ClientTeams already has a provider");
        }
        installed = provider;
    }

    /**
     * Sets the client's solo-team fallback. Hibiscus Lib calls this itself; other mods use
     * {@link #install(Provider)}.
     *
     * @param provider the fallback provider
     */
    public static synchronized void setFallback(Provider provider) {
        fallback = Objects.requireNonNull(provider, "provider");
    }
}
