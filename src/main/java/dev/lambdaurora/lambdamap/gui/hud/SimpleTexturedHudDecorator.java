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

package dev.lambdaurora.lambdamap.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class SimpleTexturedHudDecorator extends HudDecorator {
	private final int margin;
	private final Identifier texture;
	private final int bottomHeight;
	private final int coordinatesOffset;

	public SimpleTexturedHudDecorator(Identifier id, int margin, Identifier texture, int bottomHeight, int coordinatesOffset) {
		super(id);
		this.margin = margin;
		this.texture = texture;
		this.bottomHeight = bottomHeight;
		this.coordinatesOffset = coordinatesOffset;
	}

	@Override
	public int getMargin() {
		return this.margin;
	}

	@Override
	public int getCoordinatesOffset() {
		return this.coordinatesOffset;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, int width, int height) {
		RenderSystem.setShaderTexture(0, this.texture);
		DrawableHelper.drawTexture(matrices, 0, 0, width, height + this.bottomHeight, 0, 0,
				width, height + this.bottomHeight, width, height + this.bottomHeight);
	}
}
