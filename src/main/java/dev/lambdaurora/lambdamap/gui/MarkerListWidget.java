/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.lambdamap.map.marker.Marker;
import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.background.EmptyBackground;
import me.lambdaurora.spruceui.widget.SpruceButtonWidget;
import me.lambdaurora.spruceui.widget.SpruceWidget;
import me.lambdaurora.spruceui.widget.container.SpruceEntryListWidget;
import me.lambdaurora.spruceui.widget.container.SpruceParentWidget;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MarkerListWidget extends SpruceEntryListWidget<MarkerListWidget.MarkerEntry> {
    private final MarkerManager markerManager;

    public MarkerListWidget(@NotNull Position position, int width, int height, MarkerManager markerManager) {
        super(position, width, height, 0, MarkerEntry.class);
        this.markerManager = markerManager;

        this.setBackground(EmptyBackground.EMPTY_BACKGROUND);
        this.setRenderTransition(false);
        this.rebuildList();
    }

    public void rebuildList() {
        this.clearEntries();
        this.markerManager.streamMarkers().forEach(this::addMarker);
    }

    public void addMarker(Marker marker) {
        this.addEntry(new MarkerEntry(this, marker));
    }

    public static class MarkerEntry extends SpruceEntryListWidget.Entry implements SpruceParentWidget<SpruceWidget> {
        private final MarkerListWidget parent;
        private final Marker marker;
        private final List<SpruceWidget> children = new ArrayList<>();
        private @Nullable SpruceWidget focused;

        public MarkerEntry(MarkerListWidget parent, Marker marker) {
            this.parent = parent;
            this.marker = marker;

            this.children.add(new SpruceButtonWidget(Position.of(this, this.getWidth() - 24, 2), 20, 20, new LiteralText("X").formatted(Formatting.RED),
                    btn -> {
                        this.parent.markerManager.removeMarker(this.marker);
                        this.parent.rebuildList();
                    }));
        }

        @Override
        public int getWidth() {
            return this.parent.getWidth() - 6;
        }

        @Override
        public int getHeight() {
            return 24 + 2;
        }

        @Override
        public List<SpruceWidget> children() {
            return this.children;
        }

        @Override
        public @Nullable SpruceWidget getFocused() {
            return this.focused;
        }

        @Override
        public void setFocused(@Nullable SpruceWidget focused) {
            if (this.focused == focused)
                return;
            if (this.focused != null)
                this.focused.setFocused(false);
            this.focused = focused;
            if (this.focused != null)
                this.focused.setFocused(true);
        }

        /* Input */

        @Override
        protected boolean onMouseClick(double mouseX, double mouseY, int button) {
            Iterator<SpruceWidget> it = this.children().iterator();

            SpruceWidget element;
            do {
                if (!it.hasNext()) {
                    return false;
                }

                element = it.next();
            } while (!element.mouseClicked(mouseX, mouseY, button));

            this.setFocused(element);
            if (button == GLFW.GLFW_MOUSE_BUTTON_1)
                this.dragging = true;

            return true;
        }

        @Override
        protected boolean onMouseRelease(double mouseX, double mouseY, int button) {
            this.dragging = false;
            return this.hoveredElement(mouseX, mouseY).filter(element -> element.mouseReleased(mouseX, mouseY, button)).isPresent();
        }

        @Override
        protected boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            return this.getFocused() != null && this.dragging && button == GLFW.GLFW_MOUSE_BUTTON_1
                    && this.getFocused().mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }

        @Override
        protected boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
            return this.focused != null && this.focused.keyPressed(keyCode, scanCode, modifiers);
        }

        /* Rendering */

        @Override
        protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.children.forEach(widget -> widget.render(matrices, mouseX, mouseY, delta));

            int light = LightmapTextureManager.pack(15, 15);

            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
            matrices.push();
            matrices.translate(this.getX() + 12, this.getY() + 13, 5);
            matrices.scale(2, 2, 1);
            this.marker.getType().render(matrices, immediate, this.marker.getRotation(), null, light);
            matrices.pop();

            float textY = this.getY() + this.getHeight() / 2.f - 5;

            Matrix4f model = matrices.peek().getModel();

            if (this.marker.getName() != null)
                this.client.textRenderer.draw(this.marker.getName(), this.getX() + 32, textY, 0xffffffff, true,
                        model, immediate, false, 0, light);

            this.client.textRenderer.draw(String.format("X: %d Z: %d", this.marker.getX(), this.marker.getZ()),
                    this.getX() + this.getWidth() / 2.f, textY, 0xffffffff, true, model, immediate, false, 0, light);

            immediate.draw();
        }

        @Override
        protected void renderBackground(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            fill(matrices, this.getX(), this.getY(), this.getX() + this.parent.getInnerWidth(), this.getY() + this.getHeight() - 2, 0x55555555);
        }
    }
}
