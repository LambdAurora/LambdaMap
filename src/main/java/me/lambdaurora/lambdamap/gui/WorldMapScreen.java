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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import org.lwjgl.glfw.GLFW;

public class WorldMapScreen extends Screen {
    private final LambdaMap mod = LambdaMap.get();
    private final WorldMapRenderer renderer = this.mod.getRenderer();

    public WorldMapScreen() {
        super(new LiteralText("World Map"));
    }

    @Override
    public void removed() {
        super.removed();
    }

    @Override
    protected void init() {
        this.renderer.allocate(this.width - 50, this.height - 50);
        this.renderer.setWorldMap(mod.getMap());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int viewX = this.mod.getMap().getViewX();
        int viewZ = this.mod.getMap().getViewZ();
        int multiplier = Screen.hasShiftDown() ? 10 : 1;
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT:
                this.renderer.updateView(viewX - multiplier, viewZ);
                return true;
            case GLFW.GLFW_KEY_RIGHT:
                this.renderer.updateView(viewX + multiplier, viewZ);
                return true;
            case GLFW.GLFW_KEY_UP:
                this.renderer.updateView(viewX, viewZ - multiplier);
                return true;
            case GLFW.GLFW_KEY_DOWN:
                this.renderer.updateView(viewX, viewZ + multiplier);
                return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_1) {
            int viewX = this.mod.getMap().getViewX();
            int viewZ = this.mod.getMap().getViewZ();
            this.renderer.updateView((int) (viewX - deltaX), (int) (viewZ - deltaY));
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);
        this.renderBackgroundTexture(0);
        matrices.push();
        matrices.translate(25, 25, 0);
        VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());
        this.renderer.render(matrices, immediate);
        immediate.draw();
        matrices.pop();

        int mouseXOffset = mouseX - 25;
        int mouseYOffset = mouseY - 25;
        if (mouseXOffset > 0 && mouseXOffset < this.renderer.width() && mouseYOffset > 0 && mouseYOffset < this.renderer.height()) {
            drawCenteredString(matrices, this.client.textRenderer, String.format("X: %d Z: %d", this.renderer.cornerX() + mouseXOffset, this.renderer.cornerZ() + mouseYOffset),
                    this.width / 2, this.height - 15, 0xffffffff);
        }
    }
}
