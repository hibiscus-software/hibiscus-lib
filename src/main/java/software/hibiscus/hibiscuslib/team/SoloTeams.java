/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.team;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import software.hibiscus.hibiscuslib.api.team.Team;
import software.hibiscus.hibiscuslib.api.team.TeamManager;
import software.hibiscus.hibiscuslib.api.team.TeamsApi;
import software.hibiscus.hibiscuslib.api.team.event.TeamCreatedEvent;
import software.hibiscus.hibiscuslib.util.Profiles;

// The Teams API when no teams mod is installed: every player is their own team. Remembers each player who joins
// (posting TeamCreatedEvent the first time) so their team keeps a name and its extraData while they're offline.
public final class SoloTeams implements TeamManager {
    public static final SoloTeams INSTANCE = new SoloTeams();

    private SoloTeams() {}

    public static void installFallback() {
        TeamsApi.setFallback(INSTANCE);
        NeoForge.EVENT_BUS.addListener(SoloTeams::onLogin);
    }

    private static MinecraftServer server() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("The Teams API needs a running server");
        }
        return server;
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        SoloTeamStore store = SoloTeamStore.get(player.level().getServer());
        if (store.remember(player.getUUID(), player.nameAndId().name()) && TeamsApi.isFallback()) {
            NeoForge.EVENT_BUS.post(new TeamCreatedEvent(INSTANCE.playerTeam(player.getUUID()), null));
        }
    }

    @Override
    public Team teamOf(UUID player) {
        return playerTeam(player);
    }

    @Override
    public Team playerTeam(UUID player) {
        MinecraftServer server = server();
        SoloTeamStore store = SoloTeamStore.get(server);
        String name = store.entry(player).map(SoloTeamStore.Entry::name)
                .or(() -> Profiles.nameOf(server, player))
                .orElse(player.toString().substring(0, 8));
        return new SoloTeam(player, name, store);
    }

    // A player's team, if the server knows the player (they've joined, are online, or are in the name cache).
    @Override
    public Optional<Team> team(UUID teamId) {
        MinecraftServer server = server();
        if (SoloTeamStore.get(server).entry(teamId).isPresent() || Profiles.nameOf(server, teamId).isPresent()) {
            return Optional.of(playerTeam(teamId));
        }
        return Optional.empty();
    }

    @Override
    public Collection<? extends Team> allTeams() {
        MinecraftServer server = server();
        SoloTeamStore store = SoloTeamStore.get(server);
        List<SoloTeam> teams = new java.util.ArrayList<>();
        store.entries().forEach((id, entry) -> teams.add(new SoloTeam(id, entry.name(), store)));
        return List.copyOf(teams);
    }
}
