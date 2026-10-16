/*
 * Copyright (C), 2026 Hibiscus Software. Some rights reserved. This work is
 * licensed under the terms of the MIT license which can be found in the
 * root directory of this project.
 */

package software.hibiscus.hibiscuslib.client.gui.widget;

import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import software.hibiscus.hibiscuslib.client.gui.HibiscusTheme;

// Base for Hibiscus widgets: theme and font access, and a tooltip that's asked for each frame the widget is
// hovered (so it can follow state), drawn through the screen's tooltip pass.
public abstract class HibiscusWidget extends AbstractWidget {
    private @Nullable Supplier<List<Component>> tooltipLines;

    protected HibiscusWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    protected static HibiscusTheme theme() {
        return HibiscusTheme.current();
    }

    protected static Font font() {
        return Minecraft.getInstance().font;
    }

    @SuppressWarnings("unchecked")
    public <T extends HibiscusWidget> T tooltip(Component line) {
        this.tooltipLines = () -> List.of(line);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends HibiscusWidget> T tooltipLines(Supplier<List<Component>> lines) {
        this.tooltipLines = lines;
        return (T) this;
    }

    @Override
    protected final void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        extractContents(graphics, mouseX, mouseY, a);
        if (isHovered() && tooltipLines != null) {
            List<Component> lines = tooltipLines.get();
            if (!lines.isEmpty()) {
                graphics.setComponentTooltipForNextFrame(font(), lines, mouseX, mouseY);
            }
        }
    }

    protected abstract void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a);

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
