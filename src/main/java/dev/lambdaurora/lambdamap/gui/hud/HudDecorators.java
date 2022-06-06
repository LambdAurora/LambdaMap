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
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class HudDecorators {
	private static final List<HudDecorator> DECORATORS = new ArrayList<>();

	public static final HudDecorator NONE = register(new EmptyHudDecorator());
	public static final HudDecorator MAP = register(new TexturedHudDecorator(LambdaMap.id("map"), 6,
			new Identifier("textures/map/map_background_checkerboard.png"), new Identifier("textures/map/map_background.png")));

	public static <T extends HudDecorator> T register(T decorator) {
		DECORATORS.add(decorator);
		return decorator;
	}

	public static HudDecorator get(Identifier id) {
		for (var decorator : DECORATORS) {
			if (decorator.getId().equals(id))
				return decorator;
		}

		return NONE;
	}

	public static HudDecorator pick(HudDecorator current, int amount) {
		if (current == null) return DECORATORS.get(0);

		int index = DECORATORS.indexOf(current);
		int newIndex = (index + amount) % DECORATORS.size();

		return DECORATORS.get(newIndex);
	}

	private HudDecorators() {
		throw new IllegalStateException("HudDecorators only contain static definitions.");
	}
}
