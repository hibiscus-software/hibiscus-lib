/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.team;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import software.hibiscus.hibiscuslib.HibiscusLib;

/**
 * What the solo-team fallback saves with the world (data/hibiscuslib/solo_teams.dat): each player who has joined, by
 * UUID, with their last known name and the per-mod {@code extraData} tags of their solo team.
 *
 * <p>A teams mod replacing the fallback can read this with {@link #get(MinecraftServer)} to carry the data into its
 * own player teams, so progress kept before it was installed isn't lost.
 */
public final class SoloTeamStore extends SavedData {
    // One player's solo team.
    public static final class Entry {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(entry -> entry.name),
                Codec.unboundedMap(Identifier.CODEC, CompoundTag.CODEC).optionalFieldOf("data", Map.of()).forGetter(entry -> entry.data)
        ).apply(i, Entry::new));

        private String name;
        private final Map<Identifier, CompoundTag> data;

        Entry(String name, Map<Identifier, CompoundTag> data) {
            this.name = name;
            this.data = new HashMap<>(data);
        }

        public String name() {
            return name;
        }

        // The per-mod tags, live.
        public Map<Identifier, CompoundTag> data() {
            return data;
        }
    }

    private static final Codec<SoloTeamStore> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Entry.CODEC)
            .xmap(SoloTeamStore::new, store -> store.entries);
    public static final SavedDataType<SoloTeamStore> TYPE = new SavedDataType<>(HibiscusLib.id("solo_teams"), SoloTeamStore::new, CODEC);

    private final Map<UUID, Entry> entries;

    private SoloTeamStore() {
        this(Map.of());
    }

    private SoloTeamStore(Map<UUID, Entry> entries) {
        this.entries = new HashMap<>(entries);
    }

    public static SoloTeamStore get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public Optional<Entry> entry(UUID player) {
        return Optional.ofNullable(entries.get(player));
    }

    public Map<UUID, Entry> entries() {
        return Map.copyOf(entries);
    }

    // Records a player who joined (or their new name); returns true the first time the player is seen.
    boolean remember(UUID player, String name) {
        Entry entry = entries.get(player);
        if (entry == null) {
            entries.put(player, new Entry(name, Map.of()));
            setDirty();
            return true;
        }
        if (!entry.name.equals(name)) {
            entry.name = name;
            setDirty();
        }
        return false;
    }

    // A mod's tag for a player's solo team, created (with the entry, if needed) on first use.
    CompoundTag data(UUID player, String fallbackName, Identifier owningMod) {
        Entry entry = entries.computeIfAbsent(player, id -> new Entry(fallbackName, Map.of()));
        return entry.data.computeIfAbsent(owningMod, id -> {
            setDirty();
            return new CompoundTag();
        });
    }
}
