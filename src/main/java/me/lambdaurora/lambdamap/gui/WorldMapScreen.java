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
import me.lambdaurora.spruceui.background.EmptyBackground;
import me.lambdaurora.spruceui.background.SimpleColorBackground;
import me.lambdaurora.spruceui.screen.SpruceScreen;
import me.lambdaurora.spruceui.widget.container.SpruceContainerWidget;
import me.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;
import me.lambdaurora.spruceui.widget.container.tabbed.SpruceTabbedWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

public class WorldMapScreen extends SpruceScreen {
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
        super.init();

        SpruceTabbedWidget tabs = this.addChild(new SpruceTabbedWidget(Position.origin(), this.width, this.height, new LiteralText("LambdaMap")));
        tabs.getList().setBackground(new SimpleColorBackground(-1072689136));
        tabs.addTabEntry(new LiteralText("World Map"), new LiteralText("explore the world!").formatted(Formatting.GRAY),
                (width, height) -> new WorldMapWidget(Position.origin(), width, height));
        tabs.addTabEntry(new LiteralText("Markers"), new LiteralText("mark places in your world!").formatted(Formatting.GRAY),
                (width, height) -> new SpruceContainerWidget(Position.origin(), width, height));
        tabs.addTabEntry(new LiteralText("Config"), new LiteralText("mod configuration").formatted(Formatting.GRAY), this::buildConfigTab);
    }

    private SpruceOptionListWidget buildConfigTab(int width, int height) {
        SpruceOptionListWidget list = new SpruceOptionListWidget(Position.origin(), width, height);
        list.setBackground(EmptyBackground.EMPTY_BACKGROUND);
        return list;
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
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
