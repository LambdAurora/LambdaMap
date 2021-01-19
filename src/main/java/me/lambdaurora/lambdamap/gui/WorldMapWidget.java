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

package me.lambdaurora.lambdamap.gui;

import me.lambdaurora.lambdamap.LambdaMap;
import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.util.ScissorManager;
import me.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class WorldMapWidget extends AbstractSpruceWidget {
    private final LambdaMap mod = LambdaMap.get();
    private final WorldMapRenderer renderer = this.mod.getRenderer();

    public WorldMapWidget(@NotNull Position position, int width, int height) {
        super(position);
        this.width = width;
        this.height = height;

        this.renderer.allocate(this.getWidth(), this.getHeight() - 10);
        this.renderer.setWorldMap(this.mod.getMap());
    }

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_1;
    }

    @Override
    protected boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            int viewX = this.mod.getMap().getViewX();
            int viewZ = this.mod.getMap().getViewZ();
            this.renderer.updateView((int) (viewX - deltaX), (int) (viewZ - deltaY));
            return true;
        }
        return super.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        ScissorManager.push(this.getX(), this.getY(), this.getWidth(), this.getHeight() - 10);
        matrices.push();
        matrices.translate(this.getX(), this.getY(), 0);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        this.renderer.render(matrices, immediate);
        immediate.draw();
        matrices.pop();
        ScissorManager.pop();

        int mouseXOffset = mouseX - 25;
        int mouseYOffset = mouseY - 25;
        if (mouseXOffset > 0 && mouseXOffset < this.renderer.width() && mouseYOffset > 0 && mouseYOffset < this.renderer.height()) {
            drawCenteredString(matrices, this.client.textRenderer, String.format("X: %d Z: %d", this.renderer.cornerX() + mouseXOffset, this.renderer.cornerZ() + mouseYOffset),
                    this.getX() + this.getWidth() / 2, this.getHeight() - 9, 0xffffffff);
        }
    }
}
