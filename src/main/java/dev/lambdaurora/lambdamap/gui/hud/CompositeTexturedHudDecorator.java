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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;

public class CompositeTexturedHudDecorator extends HudDecorator {
	private final int margin;
	private final Identifier textureId;
	private final Identifier bottomId;

	public CompositeTexturedHudDecorator(Identifier id, int margin, Identifier textureId, Identifier bottomId) {
		super(id);
		this.margin = margin;
		this.textureId = textureId;
		this.bottomId = bottomId;
	}

	public CompositeTexturedHudDecorator(Identifier id, int margin, Identifier textureId) {
		this(id, margin, textureId, textureId);
	}

	@Override
	public int getMargin() {
		return this.margin;
	}

	@Override
	public void render(GuiGraphics graphics, VertexConsumerProvider.Immediate immediate, int width, int height) {
		graphics.drawTexture(this.textureId, 0, 0, width, height - this.margin - 1, 0, 0,
				128, 128 - this.margin - 1, 128, 128);

		int bottomHeight = this.client.textRenderer.fontHeight + 2 + this.margin - 1;
		graphics.drawTexture(this.bottomId, 0, height - this.margin - 1, width, bottomHeight,
				0, 128 - bottomHeight, 128, bottomHeight, 128, 128);
	}
}
