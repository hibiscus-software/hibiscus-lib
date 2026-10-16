/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.network;

import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import software.hibiscus.hibiscuslib.api.team.Team;

/**
 * A small layer over NeoForge's payload registration for each Hibiscus mod's channel, plus send helpers.
 *
 * <pre>{@code
 * HibiscusNetwork.channel(modEventBus, "1", channel -> {
 *     channel.toServer(InvitePayload.TYPE, InvitePayload.STREAM_CODEC, InvitePayload::handle);
 *     channel.toClient(TeamSyncPayload.TYPE, TeamSyncPayload.STREAM_CODEC, TeamSyncPayload::handle);
 * });
 * }</pre>
 *
 * <p>Handlers run on the main thread. A {@code toServer} handler must check everything itself (the sender's
 * permission and rank, a {@link software.hibiscus.hibiscuslib.util.RateLimiter}), since a client can send anything.
 * Client handlers for {@code toClient} payloads must not load client classes from the payload class itself on a
 * dedicated server; keep client code in a separate class the handler calls through. Clients send with
 * {@link software.hibiscus.hibiscuslib.client.HibiscusClientNetwork#sendToServer}.
 */
public final class HibiscusNetwork {
    private HibiscusNetwork() {}

    // A mod's channel: payloads registered under one network version (clients and servers must match).
    public static final class Channel {
        private final PayloadRegistrar registrar;

        private Channel(PayloadRegistrar registrar) {
            this.registrar = registrar;
        }

        public <T extends CustomPacketPayload> Channel toServer(CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
            registrar.playToServer(type, codec, handler);
            return this;
        }

        public <T extends CustomPacketPayload> Channel toClient(CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
            registrar.playToClient(type, codec, handler);
            return this;
        }

        public <T extends CustomPacketPayload> Channel bidirectional(CustomPacketPayload.Type<T> type,
                StreamCodec<? super RegistryFriendlyByteBuf, T> codec, IPayloadHandler<T> handler) {
            registrar.playBidirectional(type, codec, handler);
            return this;
        }

        // The underlying registrar, for anything this doesn't cover (configuration-phase payloads, optional ones).
        public PayloadRegistrar registrar() {
            return registrar;
        }
    }

    /** Registers a mod's payloads when NeoForge asks for them. {@code version} must change when payloads do. */
    public static void channel(IEventBus modEventBus, String version, Consumer<Channel> payloads) {
        modEventBus.addListener((RegisterPayloadHandlersEvent event) -> payloads.accept(new Channel(event.registrar(version))));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    // To the team's online members (just the player, for a solo team).
    public static void sendToTeam(MinecraftServer server, Team team, CustomPacketPayload payload) {
        for (ServerPlayer member : team.onlineMembers(server)) {
            PacketDistributor.sendToPlayer(member, payload);
        }
    }

    public static void sendToAll(CustomPacketPayload payload) {
        PacketDistributor.sendToAllPlayers(payload);
    }
}
