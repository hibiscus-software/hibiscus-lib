/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team;

import java.util.Objects;
import java.util.Optional;

/**
 * The entry point to the Teams API: {@code TeamsApi.get().teamOf(player)}.
 *
 * <p>With Hibiscus Teams (or another teams mod) installed, {@link #get()} returns its teams. Without one, it returns
 * Hibiscus Lib's fallback, where every player is their own single-member team whose id is the player's UUID, so code
 * written against this API works the same either way.
 *
 * <p>A teams mod installs itself with {@link #install(TeamManager, String)} from its mod constructor, and its client
 * side with {@link ClientTeams#install(ClientTeams.Provider)}.
 */
public final class TeamsApi {
    private static volatile TeamManager fallback;
    private static volatile TeamManager installed;
    private static volatile String installedBy;

    private TeamsApi() {}

    /**
     * The server's team manager: the installed teams mod's, or the solo-team fallback.
     *
     * @return the team manager
     * @throws IllegalStateException if Hibiscus Lib hasn't loaded (only possible from another mod's static
     *                               initialiser, before mod construction)
     */
    public static TeamManager get() {
        TeamManager manager = installed;
        if (manager == null) {
            manager = fallback;
        }
        if (manager == null) {
            throw new IllegalStateException("Hibiscus Lib hasn't set up the Teams API yet");
        }
        return manager;
    }

    /**
     * Whether teams come from Hibiscus Lib's solo-team fallback (no teams mod installed), where each player is their
     * own team.
     *
     * @return true if no teams mod has installed itself
     */
    public static boolean isFallback() {
        return installed == null;
    }

    /**
     * The mod id of the teams mod that installed itself, if any.
     *
     * @return the mod id, or empty when the fallback is in use
     */
    public static Optional<String> installedBy() {
        return Optional.ofNullable(installedBy);
    }

    /**
     * Makes {@code manager} the team manager, replacing the fallback. Call it once, from your mod constructor.
     *
     * @param manager the team manager
     * @param modId   your mod id, for messages
     * @throws IllegalStateException if another mod already installed a team manager
     */
    public static synchronized void install(TeamManager manager, String modId) {
        Objects.requireNonNull(manager, "manager");
        if (installed != null) {
            throw new IllegalStateException("The Teams API is already provided by " + installedBy + "; " + modId + " can't replace it");
        }
        installed = manager;
        installedBy = modId;
    }

    /**
     * Sets the solo-team fallback. Hibiscus Lib calls this itself; other mods use
     * {@link #install(TeamManager, String)}.
     *
     * @param manager the fallback team manager
     */
    public static synchronized void setFallback(TeamManager manager) {
        fallback = Objects.requireNonNull(manager, "manager");
    }
}
