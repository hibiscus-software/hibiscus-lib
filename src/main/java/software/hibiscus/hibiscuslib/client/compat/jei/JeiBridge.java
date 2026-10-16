/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.compat.jei;

import org.jspecify.annotations.Nullable;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.world.item.ItemStack;

// The JEI calls behind RecipeViewers. Only loaded once RecipeViewers has checked JEI is installed.
public final class JeiBridge {
    static volatile @Nullable IJeiRuntime runtime;

    private JeiBridge() {}

    // Opens JEI on the recipes that make the stack (OUTPUT) or that use it (INPUT); false if JEI isn't ready yet.
    public static boolean show(ItemStack stack, boolean uses) {
        IJeiRuntime jei = runtime;
        if (jei == null || stack.isEmpty()) {
            return false;
        }
        RecipeIngredientRole role = uses ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        jei.getRecipesGui().show(jei.getJeiHelpers().getFocusFactory().createFocus(role, VanillaTypes.ITEM_STACK, stack));
        return true;
    }
}
