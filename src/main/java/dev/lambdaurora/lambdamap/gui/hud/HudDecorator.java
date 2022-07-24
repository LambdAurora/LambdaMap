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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Represents a map HUD decorator.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public abstract class HudDecorator {
	protected final MinecraftClient client = MinecraftClient.getInstance();
	private final Identifier id;

	protected HudDecorator(Identifier id) {
		this.id = id;
	}

	/**
	 * {@return the identifier of this map HUD decorator}
	 */
	public Identifier getId() {
		return this.id;
	}

	public String getTranslationKey() {
		var prefix = "lambdamap.hud.decorator.";
		if (!id.getNamespace().equals(LambdaMap.NAMESPACE))
			prefix += id.getNamespace() + '.';
		return prefix + id.getPath().replace('/', '.');
	}

	public Text getName() {
		return Text.translatable(this.getTranslationKey());
	}

	public abstract int getMargin();

	public int getCoordinatesOffset() {
		return 0;
	}

	public abstract void render(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, int width, int height);
}
