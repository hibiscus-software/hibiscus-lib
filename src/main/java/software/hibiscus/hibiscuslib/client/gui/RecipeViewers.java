/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui;

import java.lang.reflect.Method;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.client.compat.jei.JeiBridge;

/**
 * Opens the installed recipe viewer (JEI or EMI) on an item's recipes or uses. Every method quietly does nothing, and
 * returns false, when neither is installed, so callers can offer the click without checking first.
 */
public final class RecipeViewers {
    private static final boolean JEI = ModList.get().isLoaded("jei");
    private static final boolean EMI = ModList.get().isLoaded("emi");
    // EMI has no 26.3 build to compile against, so its API is reached by reflection.
    private static Method emiOf;
    private static Method emiRecipes;
    private static Method emiUses;
    private static boolean emiFailed;

    private RecipeViewers() {}

    public static boolean isAvailable() {
        return JEI || EMI;
    }

    // The recipes that make the stack.
    public static boolean showRecipes(ItemStack stack) {
        return show(stack, false);
    }

    // The recipes that use the stack.
    public static boolean showUses(ItemStack stack) {
        return show(stack, true);
    }

    private static boolean show(ItemStack stack, boolean uses) {
        if (stack.isEmpty()) {
            return false;
        }
        if (JEI && JeiBridge.show(stack, uses)) {
            return true;
        }
        return EMI && showEmi(stack, uses);
    }

    // dev.emi.emi.api.EmiApi.displayRecipes/displayUses(EmiIngredient), with EmiStack.of(ItemStack).
    private static boolean showEmi(ItemStack stack, boolean uses) {
        if (emiFailed) {
            return false;
        }
        try {
            if (emiOf == null) {
                Class<?> api = Class.forName("dev.emi.emi.api.EmiApi");
                Class<?> emiStack = Class.forName("dev.emi.emi.api.stack.EmiStack");
                Class<?> ingredient = Class.forName("dev.emi.emi.api.stack.EmiIngredient");
                emiOf = emiStack.getMethod("of", ItemStack.class);
                emiRecipes = api.getMethod("displayRecipes", ingredient);
                emiUses = api.getMethod("displayUses", ingredient);
            }
            Object ingredient = emiOf.invoke(null, stack);
            (uses ? emiUses : emiRecipes).invoke(null, ingredient);
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            emiFailed = true;
            HibiscusLib.LOGGER.warn("Couldn't open EMI; recipe lookups from Hibiscus screens are off", e);
            return false;
        }
    }
}
