/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;
import software.hibiscus.hibiscuslib.api.team.Team;

/**
 * A player stopped being a member of a party or server team, {@link #team()}. Posted after the player has left; a
 * {@link PlayerJoinedTeamEvent} to their new team (their solo team, or the team they switched to) follows.
 */
public class PlayerLeftTeamEvent extends TeamEvent {
    /** Why the player left. */
    public enum Reason {
        /** They left on their own. */
        LEFT,
        /** An owner or officer removed them. */
        KICKED,
        /** The team was deleted. */
        DELETED,
        /** They left to join another team. */
        SWITCHED
    }

    private final UUID playerId;
    private final ServerPlayer player;
    private final Reason reason;

    /**
     * Creates the event.
     *
     * @param team     the team the player left
     * @param playerId the player
     * @param player   the player entity if they're online, else null
     * @param reason   why they left
     */
    public PlayerLeftTeamEvent(Team team, UUID playerId, ServerPlayer player, Reason reason) {
        super(team);
        this.playerId = playerId;
        this.player = player;
        this.reason = reason;
    }

    /**
     * The player who left.
     *
     * @return the player's UUID
     */
    public UUID playerId() {
        return playerId;
    }

    /**
     * The player, if online.
     *
     * @return the player entity, or empty
     */
    public Optional<ServerPlayer> player() {
        return Optional.ofNullable(player);
    }

    /**
     * Why the player left.
     *
     * @return the reason
     */
    public Reason reason() {
        return reason;
    }
}
