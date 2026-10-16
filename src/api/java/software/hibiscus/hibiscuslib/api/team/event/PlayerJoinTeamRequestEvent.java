/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.api.team.event;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.ICancellableEvent;
import software.hibiscus.hibiscuslib.api.team.Team;

/**
 * A player is about to join a team (accepting an invite, joining an open team, or being added by an admin). Cancel it,
 * ideally with {@link #deny(Component)} and a reason the teams mod shows the player, to stop the join: for example a
 * quest mod refusing while a team quest is in progress. Posted before anything changes.
 */
public class PlayerJoinTeamRequestEvent extends TeamEvent implements ICancellableEvent {
    private final UUID playerId;
    private final ServerPlayer player;
    private final Team from;
    private Component denyReason;

    /**
     * Creates the event.
     *
     * @param team     the team the player wants to join
     * @param playerId the player
     * @param player   the player entity if they're online, else null
     * @param from     the team they're in now
     */
    public PlayerJoinTeamRequestEvent(Team team, UUID playerId, ServerPlayer player, Team from) {
        super(team);
        this.playerId = playerId;
        this.player = player;
        this.from = from;
    }

    /**
     * The player who wants to join.
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
     * The team the player would leave.
     *
     * @return their current team
     */
    public Team from() {
        return from;
    }

    /**
     * Stops the join and says why.
     *
     * @param reason shown to the player
     */
    public void deny(Component reason) {
        this.denyReason = reason;
        setCanceled(true);
    }

    /**
     * Why the join was stopped, if a reason was given.
     *
     * @return the reason, or empty
     */
    public Optional<Component> denyReason() {
        return Optional.ofNullable(denyReason);
    }
}
