/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.team;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import software.hibiscus.hibiscuslib.api.team.ClientTeams;
import software.hibiscus.hibiscuslib.api.team.Team;
import software.hibiscus.hibiscuslib.team.SoloTeam;

// ClientTeams without a teams mod: the local player's solo team, built from what the client already knows (its own
// UUID and name), so nothing needs syncing. Other players' solo teams aren't known on the client.
public final class ClientSoloTeams implements ClientTeams.Provider {
    public static void installFallback() {
        ClientTeams.setFallback(new ClientSoloTeams());
    }

    @Override
    public Optional<Team> myTeam() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? Optional.empty() : Optional.of(new SoloTeam(player.getUUID(), player.nameAndId().name(), null));
    }

    @Override
    public Optional<Team> team(UUID teamId) {
        return myTeam().filter(team -> team.id().equals(teamId));
    }
}
