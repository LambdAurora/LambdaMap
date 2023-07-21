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

import dev.lambdaurora.lambdamap.LambdaMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;

public final class EmptyHudDecorator extends HudDecorator {
	public static final Identifier ID = LambdaMap.id("none");

	EmptyHudDecorator() {
		super(ID);
	}

	@Override
	public int getMargin() {
		return 0;
	}

	@Override
	public void render(GuiGraphics graphics, VertexConsumerProvider.Immediate immediate, int width, int height) {
	}
}
