/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.TagParser;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Reads and writes human-editable SNBT files (quest books, team settings) under {@code config/}.
 *
 * <p>Writes are safe against crashes and half-written files: the new text goes to {@code <name>.tmp}, the current file
 * is copied to {@code <name>.bak}, and the temp file is then moved into place (atomically where the file system
 * allows). If a file won't parse, {@link #read(Path)} throws rather than returning an empty tag, so a typo in a
 * hand-edited file can't wipe it on the next save.
 */
public final class SnbtFiles {
    private SnbtFiles() {}

    // A path under the game's config directory, e.g. configFile("hibiscusquests", "chapters", "basics.snbt").
    public static Path configFile(String first, String... more) {
        return FMLPaths.CONFIGDIR.get().resolve(Path.of(first, more));
    }

    /**
     * Reads a file.
     *
     * @return the tag, or empty if the file doesn't exist
     * @throws IOException if it can't be read or isn't valid SNBT (the message says where)
     */
    public static Optional<CompoundTag> read(Path file) throws IOException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        try {
            return Optional.of(TagParser.parseCompoundFully(text));
        } catch (CommandSyntaxException e) {
            throw new IOException("Invalid SNBT in " + file + ": " + e.getMessage(), e);
        }
    }

    // Formats a tag as indented SNBT, one entry per line.
    public static String toText(CompoundTag tag) {
        return new SnbtPrinterTagVisitor().visit(tag);
    }

    /** Writes a file: temp file, back up the current one to .bak, then move into place. Creates parent directories. */
    public static void write(Path file, CompoundTag tag) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(temp, toText(tag) + "\n", StandardCharsets.UTF_8);
        if (Files.exists(file)) {
            Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
