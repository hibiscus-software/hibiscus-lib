/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.gametest;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import software.hibiscus.hibiscuslib.api.team.Team;
import software.hibiscus.hibiscuslib.api.team.TeamManager;
import software.hibiscus.hibiscuslib.api.team.TeamRank;
import software.hibiscus.hibiscuslib.api.team.TeamType;
import software.hibiscus.hibiscuslib.api.team.TeamsApi;
import software.hibiscus.hibiscuslib.api.team.event.TeamCreatedEvent;
import software.hibiscus.hibiscuslib.team.SoloTeamStore;
import software.hibiscus.hibiscuslib.team.SoloTeams;

// The Teams API with no teams mod installed: Hibiscus Lib's fallback, where every player is their own team. These are
// the guarantees Hibiscus Quests relies on to work without Hibiscus Teams. They test the fallback itself
// (SoloTeams.INSTANCE) rather than TeamsApi.get(), so they also pass in a teams mod's dev runs, where these tests run
// alongside the teams mod's own and TeamsApi.get() is the teams mod.
final class TeamsGameTests {
    private static final Identifier QUESTS = Identifier.fromNamespaceAndPath("hibiscusquests", "progress");
    private static final Identifier OTHER = Identifier.fromNamespaceAndPath("othermod", "data");
    // Teams whose TeamCreatedEvent has been seen, by id.
    private static final Set<UUID> CREATED = ConcurrentHashMap.newKeySet();

    static {
        NeoForge.EVENT_BUS.addListener((TeamCreatedEvent event) -> CREATED.add(event.team().id()));
    }

    private TeamsGameTests() {}

    // A mock player that has logged in (in the player list, so they're online and their join is recorded).
    @SuppressWarnings("removal")
    static ServerPlayer join(GameTestHelper helper) {
        return helper.makeMockServerPlayerInLevel();
    }

    static void leave(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().getServer().getPlayerList().remove(player);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    // TeamsApi.get() is the fallback unless a teams mod has installed itself, in which case it's that mod's.
    static void fallbackInstalled(GameTestHelper helper) {
        if (TeamsApi.isFallback()) {
            check(helper, TeamsApi.get() == SoloTeams.INSTANCE, "TeamsApi.get() should be the solo-team fallback");
            check(helper, TeamsApi.installedBy().isEmpty(), "installedBy should be empty with the fallback");
        } else {
            check(helper, TeamsApi.installedBy().isPresent(), "a replaced fallback should say which mod replaced it");
            check(helper, TeamsApi.get() != SoloTeams.INSTANCE, "TeamsApi.get() should be the installed teams mod");
        }
        helper.succeed();
    }

    // Every Team method answers as a one-player team would.
    static void soloTeamShape(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            UUID id = player.getUUID();
            UUID stranger = UUID.randomUUID();
            TeamManager teams = SoloTeams.INSTANCE;
            Team team = teams.teamOf(id);
            check(helper, team.id().equals(id), "a solo team's id is its player's UUID");
            check(helper, team.type() == TeamType.PLAYER, "type should be PLAYER");
            check(helper, team.owner().equals(java.util.Optional.of(id)), "the player owns their team");
            check(helper, team.members().equals(Map.of(id, TeamRank.OWNER)), "the only member is the player, as OWNER");
            check(helper, team.rankOf(id) == TeamRank.OWNER && team.rankOf(stranger) == TeamRank.NONE, "ranks: OWNER for the player, NONE for others");
            check(helper, team.isMember(id) && !team.isMember(stranger), "isMember");
            check(helper, team.allies().isEmpty() && team.isAlly(id) && !team.isAlly(stranger), "no allies, but allied with itself");
            check(helper, team.shortName().equals(player.nameAndId().name()), "short name is the player's name");
            check(helper, team.equals(teams.playerTeam(id)), "teamOf and playerTeam agree");
            check(helper, teams.teamOf(player).equals(team), "teamOf(Player) agrees");
            check(helper, teams.sameTeam(id, id) && !teams.sameTeam(id, stranger), "sameTeam");
            check(helper, teams.team(id).isPresent(), "team(id) finds a joined player's team");
            check(helper, teams.allTeams().contains(team), "allTeams lists a joined player's team");
            check(helper, team.onlineMembers(helper.getLevel().getServer()).equals(java.util.List.of(player)), "onlineMembers is the online player");
            helper.succeed();
        } finally {
            leave(helper, player);
        }
    }

    static void unknownTeamEmpty(GameTestHelper helper) {
        check(helper, SoloTeams.INSTANCE.team(UUID.randomUUID()).isEmpty(), "team() of a never-seen UUID should be empty");
        helper.succeed();
    }

    // Each mod's extraData is its own, survives fetching the team again (it's stored, not on the object), and
    // markDirty marks the saved data for saving.
    static void extraDataPersists(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            UUID id = player.getUUID();
            Team team = SoloTeams.INSTANCE.teamOf(id);
            team.extraData(QUESTS).putInt("completed", 7);
            team.extraData(OTHER).putString("note", "hello");
            SoloTeamStore store = SoloTeamStore.get(helper.getLevel().getServer());
            store.setDirty(false);
            team.markDirty();
            check(helper, store.isDirty(), "markDirty should mark the saved data dirty");
            Team again = SoloTeams.INSTANCE.teamOf(id);
            CompoundTag quests = again.extraData(QUESTS);
            check(helper, quests.getIntOr("completed", -1) == 7, "extraData should keep its values between lookups");
            check(helper, !quests.contains("note") && again.extraData(OTHER).getStringOr("note", "").equals("hello"),
                    "each mod's extraData is separate");
            helper.succeed();
        } finally {
            leave(helper, player);
        }
    }

    // The first login creates the player's solo team and announces it.
    static void createdEventOnFirstJoin(GameTestHelper helper) {
        ServerPlayer player = join(helper);
        try {
            check(helper, CREATED.contains(player.getUUID()), "TeamCreatedEvent should fire for a player's first login");
            helper.succeed();
        } finally {
            leave(helper, player);
        }
    }
}
