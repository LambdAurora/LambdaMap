/*
 * Copyright (c) 2021-2022 LambdAurora <email@lambdaurora.dev>
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

import com.mojang.blaze3d.vertex.Tessellator;
import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.widget.SpruceButtonWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class MarkerTypeButton extends SpruceButtonWidget {
	private static final Identifier FOCUSED_TEXTURE = LambdaMap.id("textures/gui/icon_selection.png");
	private final Consumer<MarkerType> changeListener;
	private MarkerType type;

	public MarkerTypeButton(Position position, MarkerType type, Consumer<MarkerType> changeListener) {
		super(position, 20, 20, Text.empty(), btn -> {
			MarkerType next = MarkerType.next(((MarkerTypeButton) btn).type);
			((MarkerTypeButton) btn).type = next;
			changeListener.accept(next);
		});
		this.changeListener = changeListener;
		this.type = type;
	}

	@Override
	protected boolean onMouseClick(double mouseX, double mouseY, int button) {
		if (super.onMouseClick(mouseX, mouseY, button)) {
			return true;
		} else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
			this.playDownSound();
			MarkerType next = MarkerType.previous(this.type);
			this.type = next;
			this.changeListener.accept(next);
			return true;
		}
		return false;
	}

	@Override
	protected boolean onMouseScroll(double mouseX, double mouseY, double amount) {
		this.playDownSound();
		MarkerType next = amount > 0 ? MarkerType.next(this.type) : MarkerType.previous(this.type);
		this.type = next;
		this.changeListener.accept(next);
		return true;
	}

	public MarkerType getMarkerType() {
		return this.type;
	}

	public void setMarkerType(MarkerType type) {
		this.type = type;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.getMatrices().push();
		graphics.getMatrices().translate(this.getX() + 9, this.getY() + 11, 5);
		graphics.getMatrices().scale(2, 2, 1);
		VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBufferBuilder());
		this.type.render(graphics, immediate, 180.f, null, LightmapTextureManager.pack(15, 15));
		immediate.draw();
		graphics.getMatrices().pop();
	}

	@Override
	protected void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		if (this.isFocused()) {
			int width = this.getWidth();
			int height = this.getHeight();
			graphics.drawTexture(FOCUSED_TEXTURE, this.getX(), this.getY(), 0, 0, width, height, width, height);
		}
	}
}
