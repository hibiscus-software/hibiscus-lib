/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.screenshots;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

import org.jspecify.annotations.Nullable;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import software.hibiscus.hibiscuslib.HibiscusLib;
import software.hibiscus.hibiscuslib.button.ButtonBadge;
import software.hibiscus.hibiscuslib.button.ButtonSpec;
import software.hibiscus.hibiscuslib.button.HibiscusButtons;
import software.hibiscus.hibiscuslib.client.button.EditButtonsScreen;
import software.hibiscus.hibiscuslib.client.gui.HibiscusToasts;
import software.hibiscus.hibiscuslib.client.gui.Icon;
import software.hibiscus.hibiscuslib.client.gui.WidgetGalleryScreen;
import software.hibiscus.hibiscuslib.client.gui.popup.ConfirmDialog;
import software.hibiscus.hibiscuslib.client.gui.popup.ContextMenu;

// Development only (src/screenshots, never in the mod jar): ./gradlew runScreenshotClient opens a flat creative world,
// puts the button bar and widget gallery on screen at each GUI scale and window size the README promises, saves a
// screenshot of each to run/screenshotClient/screenshots/, and quits. Lets the layout be checked without clicking
// through it by hand, and makes the README's images.
@EventBusSubscriber(modid = HibiscusLib.MODID, value = Dist.CLIENT)
public final class ScreenshotHarness {
    private static final boolean ENABLED = Boolean.getBoolean("hibiscuslib.screenshots");
    private static final String WORLD = "hibiscus_screenshots";

    private record Step(Runnable action, int waitTicks, @Nullable String shot) {}

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static boolean worldRequested;
    private static boolean planned;
    private static int wait;
    private static int inWorldTicks;

    private ScreenshotHarness() {}

    // Demo buttons: enough to wrap into a second column, with both kinds of badge.
    @SubscribeEvent
    static void setup(FMLClientSetupEvent event) {
        if (!ENABLED) {
            return;
        }
        Object[][] demo = {
                {"quests", Items.WRITABLE_BOOK, ButtonBadge.count(3)},
                {"teams", Items.PLAYER_HEAD, ButtonBadge.dot()},
                {"waypoints", Items.COMPASS, null},
                {"claims", Items.FILLED_MAP, null},
                {"backpack", Items.BUNDLE, ButtonBadge.count(128)},
                {"shop", Items.EMERALD, null},
                {"mail", Items.PAPER, ButtonBadge.count(1)},
                {"guide", Items.KNOWLEDGE_BOOK, null},
                {"homes", Items.CAMPFIRE, null},
                {"settings", Items.COMPARATOR, null},
                {"stats", Items.CLOCK, null},
        };
        for (int i = 0; i < demo.length; i++) {
            Object[] entry = demo[i];
            // Item stacks can't be made during setup (data components aren't bound yet), so the icon makes it on first draw.
            net.minecraft.world.item.Item item = (net.minecraft.world.item.Item) entry[1];
            com.google.common.base.Supplier<ItemStack> stack = com.google.common.base.Suppliers.memoize(() -> new ItemStack(item));
            ButtonBadge badge = (ButtonBadge) entry[2];
            HibiscusButtons.register(HibiscusLib.id("demo/" + entry[0]), ButtonSpec.builder()
                    .item(stack)
                    .tooltip(Component.literal("Demo: " + entry[0]))
                    .badge(() -> badge)
                    .order(i * 10)
                    .onClick(() -> Minecraft.getInstance().gui.setScreen(new WidgetGalleryScreen(Minecraft.getInstance().gui.screen())))
                    .build());
        }
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        if (!ENABLED) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            if (!worldRequested && minecraft.gui.screen() instanceof TitleScreen) {
                worldRequested = true;
                openWorld(minecraft);
            }
            return;
        }
        // Let chunks and JEI's ingredient list load first.
        if (++inWorldTicks < 100) {
            return;
        }
        if (!planned) {
            planned = true;
            // No movement tutorial toast in the pictures.
            minecraft.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
            minecraft.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            // The client half of the Teams API fallback: the local player's own solo team.
            HibiscusLib.LOGGER.info("ClientTeams.myTeam() in world: {}", software.hibiscus.hibiscuslib.api.team.ClientTeams.myTeam());
            plan(minecraft);
        }
        if (wait > 0) {
            wait--;
            return;
        }
        Step step = STEPS.poll();
        if (step == null) {
            HibiscusLib.LOGGER.info("Screenshot harness finished");
            minecraft.stop();
            return;
        }
        if (step.shot() != null) {
            File dir = minecraft.gameDirectory;
            String name = step.shot() + ".png";
            Screenshot.grab(dir, name, minecraft.gameRenderer.mainRenderTarget(), 1, message -> HibiscusLib.LOGGER.info("Screenshot {}: {}", name, message.getString()));
        }
        step.action().run();
        wait = step.waitTicks();
    }

    private static void openWorld(Minecraft minecraft) {
        if (minecraft.getLevelSource().levelExists(WORLD)) {
            minecraft.createWorldOpenFlows().openWorld(WORLD, () -> minecraft.gui.setScreen(new TitleScreen()));
            return;
        }
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE,
                LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(26_3L, false, false),
                WorldPresets::createTestWorldDimensions, new TitleScreen());
    }

    private static void then(Runnable action, int waitTicks) {
        STEPS.add(new Step(action, waitTicks, null));
    }

    // Takes a screenshot of what's on screen now, then runs the next action.
    private static void shot(String name) {
        STEPS.add(new Step(() -> {}, 2, name));
    }

    private static void show(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    private static void plan(Minecraft minecraft) {
        // The smallest GUI space the README promises is 1280x720 at scale 3 (427x240). Scale 4 needs a window at least
        // 960 high; 1440x960 (360x240) fits on a 1080p monitor without maximising, which a 1920x1080 window would (and a
        // maximised window ignores later resizes).
        int[][] sizes = {{1280, 720, 1}, {1280, 720, 2}, {1280, 720, 3}, {1440, 960, 4}};
        for (int[] size : sizes) {
            String tag = size[0] + "x" + size[1] + "_scale" + size[2];
            then(() -> {
                minecraft.getWindow().setWindowed(size[0], size[1]);
                minecraft.options.guiScale().set(size[2]);
                minecraft.resizeGui();
            }, 20);
            then(() -> minecraft.resizeGui(), 5);

            // The survival inventory needs survival mode (in creative, vanilla swaps it for the creative one).
            then(() -> minecraft.player.connection.sendCommand("gamemode survival"), 10);
            then(() -> show(minecraft, new InventoryScreen(minecraft.player)), 10);
            shot("inventory_" + tag);
            then(() -> pressRecipeBook(minecraft), 10);
            shot("inventory_recipe_book_" + tag);
            then(() -> pressRecipeBook(minecraft), 2);
            then(() -> {
                show(minecraft, null);
                minecraft.player.connection.sendCommand("gamemode creative");
            }, 10);
            then(() -> show(minecraft, new CreativeModeInventoryScreen(minecraft.player, minecraft.player.connection.enabledFeatures(), true)), 10);
            shot("creative_" + tag);
            then(() -> show(minecraft, new EditButtonsScreen(new InventoryScreen(minecraft.player))), 10);
            shot("edit_buttons_" + tag);

            for (int tab = 0; tab < 5; tab++) {
                int index = tab;
                then(() -> {
                    WidgetGalleryScreen gallery = new WidgetGalleryScreen(null);
                    show(minecraft, gallery);
                    gallery.showTab(index);
                }, 10);
                shot("gallery_tab" + tab + "_" + tag);
                if (tab == 2) {
                    // The player faces are below the list: scroll down to them.
                    then(() -> minecraft.gui.screen().children().stream()
                            .filter(child -> child instanceof software.hibiscus.hibiscuslib.client.gui.widget.ScrollPanel)
                            .map(child -> (software.hibiscus.hibiscuslib.client.gui.widget.ScrollPanel) child)
                            .forEach(panel -> panel.setScrollAmount(panel.maxScrollAmount())), 5);
                    shot("gallery_tab2_scrolled_" + tag);
                }
            }
            then(() -> {
                if (minecraft.gui.screen() instanceof WidgetGalleryScreen gallery) {
                    gallery.openPopup(new ConfirmDialog(Component.literal("Delete this chapter?"),
                            Component.literal("Every quest in it is removed for everyone. This can't be undone."),
                            Component.literal("Delete"), Component.translatable("gui.cancel"), yes -> {}).dangerous());
                }
            }, 10);
            shot("gallery_dialog_" + tag);
            then(() -> {
                if (minecraft.gui.screen() instanceof WidgetGalleryScreen gallery) {
                    gallery.topPopup().ifPresent(popup -> popup.cancel());
                    gallery.openPopup(new ConfirmDialog(Component.literal("Disband Team Coral?"),
                            Component.literal("Every member goes back to their own team and the team's settings are lost."),
                            Component.literal("Disband"), Component.translatable("gui.cancel"), yes -> {}).dangerous().requireTyping("Coral"));
                }
            }, 10);
            shot("gallery_typed_dialog_" + tag);
            then(() -> {
                if (minecraft.gui.screen() instanceof WidgetGalleryScreen gallery) {
                    gallery.topPopup().ifPresent(popup -> popup.cancel());
                    gallery.openPopup(ContextMenu.builder()
                            .add(Component.literal("Rename"), () -> {})
                            .add(Component.literal("Change colour"), Icon.color(0xFFF2557A), () -> {})
                            .addDisabled(Component.literal("Promote (officers only)"), Icon.NONE)
                            .separator()
                            .addDanger(Component.literal("Kick"), Icon.NONE, () -> {})
                            .build(gallery.width / 2 - 40, gallery.height / 2 - 30));
                    HibiscusToasts.show(Component.literal("Quest complete"), Component.literal("Getting Started: collect your reward."),
                            Icon.item(new ItemStack(Items.WRITABLE_BOOK)));
                }
            }, 30);
            shot("gallery_menu_toast_" + tag);
            then(() -> {
                show(minecraft, null);
                minecraft.gui.toastManager().clear();
            }, 5);
        }
    }

    // Clicks the inventory's recipe book button, as a player would.
    private static void pressRecipeBook(Minecraft minecraft) {
        if (minecraft.gui.screen() instanceof InventoryScreen screen) {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof ImageButton button) {
                    button.onPress(new KeyEvent(InputConstants.KEY_RETURN, 0, 0));
                    return;
                }
            }
        }
    }
}
