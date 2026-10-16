/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import software.hibiscus.hibiscuslib.gametest.HibiscusLibGameTests;
import software.hibiscus.hibiscuslib.team.SoloTeams;
import software.hibiscus.hibiscuslib.util.PermissionLevels;

// Hibiscus Lib's common entrypoint (both sides). Must match the modId in META-INF/neoforge.mods.toml.
@Mod(HibiscusLib.MODID)
public class HibiscusLib {
    public static final String MODID = "hibiscuslib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HibiscusLib(IEventBus modEventBus, ModContainer modContainer) {
        // The solo-team fallback is the Teams API until Hibiscus Teams (or another teams mod) installs its own.
        SoloTeams.installFallback();
        PermissionLevels.register();
        HibiscusLibGameTests.register(modEventBus);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
