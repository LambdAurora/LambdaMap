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

import dev.lambdaurora.lambdamap.LambdaMap;
import me.lambdaurora.spruceui.Position;
import me.lambdaurora.spruceui.util.ScissorManager;
import me.lambdaurora.spruceui.widget.AbstractSpruceWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public class WorldMapWidget extends AbstractSpruceWidget {
    private final LambdaMap mod = LambdaMap.get();
    private final WorldMapRenderer renderer = this.mod.getRenderer();
    private int intScale = 0;
    private float scale = 1.f;

    public WorldMapWidget(@NotNull Position position, int width, int height) {
        super(position);
        this.width = width;
        this.height = height;

        this.renderer.allocate(this.getWidth(), this.getHeight() - 10);
        this.renderer.setWorldMap(this.mod.getMap());
    }

    private void rescale(int amount) {
        this.intScale = MathHelper.clamp(this.intScale + amount, -4, 3);

        this.applyScale();
    }

    private void applyScale() {
        float oldScale = this.scale;
        if (this.intScale < 0)
            this.scale = -this.intScale;
        else
            this.scale = 1.f;

        if (oldScale != this.scale) {
            float scaleCompensation = 1.f / this.scale;
            this.renderer.allocate((int) (this.getWidth() * scaleCompensation), (int) ((this.getHeight() - 10) * scaleCompensation));
        }

        this.renderer.scale(Math.max(0, this.intScale));
    }

    /* Input */

    @Override
    protected boolean onMouseClick(double mouseX, double mouseY, int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_1;
    }

    @Override
    protected boolean onMouseDrag(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            int viewX = this.mod.getMap().getViewX();
            int viewZ = this.mod.getMap().getViewZ();
            float scaleCompensation = 1.f / this.scale;
            if (this.renderer.scale() != 1) {
                scaleCompensation = this.renderer.scale();
            }
            this.renderer.updateView((int) (viewX - deltaX * scaleCompensation), (int) (viewZ - deltaY * scaleCompensation));
            return true;
        }
        return super.onMouseDrag(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        this.rescale((int) -amount);
        return true;
    }

    /* Rendering */

    @Override
    protected void renderWidget(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        ScissorManager.push(this.getX(), this.getY(), this.getWidth(), this.getHeight() - 10);
        matrices.push();
        matrices.translate(this.getX(), this.getY(), 0);
        matrices.scale(this.scale, this.scale, 1.f);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        this.renderer.render(matrices, immediate);
        immediate.draw();
        matrices.pop();
        ScissorManager.pop();

        int mouseXOffset = mouseX - this.getX();
        int mouseYOffset = mouseY - this.getY();
        float scaleCompensation = 1.f / this.scale;
        if (this.renderer.scale() != 1) {
            scaleCompensation = this.renderer.scale();
        }
        if (mouseXOffset > 0 && mouseXOffset < this.renderer.width() * this.scale && mouseYOffset > 0 && mouseYOffset < this.renderer.height() * this.scale) {
            drawCenteredString(matrices, this.client.textRenderer, String.format("X: %.1f Z: %.1f", this.renderer.cornerX() + mouseXOffset * scaleCompensation, this.renderer.cornerZ() + mouseYOffset * scaleCompensation),
                    this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() - 9, 0xffffffff);
        }

        String scale = "1:" + this.renderer.scale();
        if (this.intScale < 0) {
            scale = -this.intScale + ":1";
        }
        this.client.textRenderer.drawWithShadow(matrices, scale, this.getX(), this.getY() + this.getHeight() - 9, 0xffffffff);
    }
}
