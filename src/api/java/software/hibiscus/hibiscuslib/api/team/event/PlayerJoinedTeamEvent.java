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
 * A player's current team changed: they joined a party (from their solo team or another party), or went back to
 * their solo team after leaving, being kicked or the party being deleted. This is the one event to watch for per-team
 * data: {@link #from()} is the team whose data the player had and {@link #to()} the team they use now, so a mod can
 * keep, merge or reset the data as it chooses.
 *
 * <p>Posted after the change, so {@code TeamsApi.get().teamOf(player)} is already {@link #to()}.
 */
public class PlayerJoinedTeamEvent extends TeamEvent {
    private final UUID playerId;
    private final ServerPlayer player;
    private final Team from;

    /**
     * Creates the event.
     *
     * @param playerId the player
     * @param player   the player entity if they're online, else null
     * @param from     the team they were in
     * @param to       the team they're in now
     */
    public PlayerJoinedTeamEvent(UUID playerId, ServerPlayer player, Team from, Team to) {
        super(to);
        this.playerId = playerId;
        this.player = player;
        this.from = from;
    }

    /**
     * The player who changed team.
     *
     * @return the player's UUID
     */
    public UUID playerId() {
        return playerId;
    }

    /**
     * The player, if online (a kicked player, or a member of a deleted team, may not be).
     *
     * @return the player entity, or empty
     */
    public Optional<ServerPlayer> player() {
        return Optional.ofNullable(player);
    }

    /**
     * The team the player was in: their solo team when they're joining a party.
     *
     * @return the previous team
     */
    public Team from() {
        return from;
    }

    /**
     * The team the player is in now (the same as {@link #team()}).
     *
     * @return the new team
     */
    public Team to() {
        return team();
    }
}
