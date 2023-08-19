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

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.spruceui.Position;
import dev.lambdaurora.spruceui.background.EmptyBackground;
import dev.lambdaurora.spruceui.option.SpruceSeparatorOption;
import dev.lambdaurora.spruceui.screen.SpruceScreen;
import dev.lambdaurora.spruceui.widget.container.SpruceOptionListWidget;
import dev.lambdaurora.spruceui.widget.container.tabbed.SpruceTabbedWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WorldMapScreen extends SpruceScreen {
	private final LambdaMap mod = LambdaMap.get();

	public WorldMapScreen() {
		super(Text.literal("World Map"));
	}

	@Override
	public void removed() {
		super.removed();
	}

	@Override
	protected void init() {
		super.init();

		if (mod.getConfig().isFullscreen()) {
			this.addDrawableChild(new WorldMapWidget(Position.origin(), width, height));
		} else {
			SpruceTabbedWidget tabs = this.addDrawableChild(new SpruceTabbedWidget(Position.origin(), this.width, this.height, Text.literal("LambdaMap")));
			tabs.getList().setBackground(RandomPrideFlagBackground.random());
			tabs.addTabEntry(Text.translatable("lambdamap.tabs.world_map"), Text.translatable("lambdamap.tabs.world_map.description").formatted(Formatting.GRAY),
					(width, height) -> new WorldMapWidget(Position.origin(), width, height));
			if (this.mod.getConfig().isMarkerEditorVisible()) {
				tabs.addTabEntry(Text.translatable("lambdamap.tabs.markers"), Text.translatable("lambdamap.tabs.markers.description").formatted(Formatting.GRAY),
						(width, height) -> new MarkerTabWidget(mod, Position.origin(), width, height));
			}
			tabs.addTabEntry(Text.translatable("lambdamap.tabs.config"), Text.translatable("lambdamap.tabs.config.description").formatted(Formatting.GRAY),
					this::buildConfigTab);
		}
	}

	private SpruceOptionListWidget buildConfigTab(int width, int height) {
		var list = new SpruceOptionListWidget(Position.origin(), width, height);
		list.setBackground(EmptyBackground.EMPTY_BACKGROUND);

		list.addSingleOptionEntry(new SpruceSeparatorOption("lambdamap.config.category.general", true, null));
		list.addSingleOptionEntry(this.mod.getConfig().getRenderBiomeColorsOption());
		list.addSingleOptionEntry(this.mod.getConfig().getShowMarkerEditorOption());
		list.addSingleOptionEntry(this.mod.getConfig().getDoFullscreenOption());
		list.addSingleOptionEntry(new SpruceSeparatorOption("lambdamap.config.category.hud", true, null));
		list.addSingleOptionEntry(this.mod.getConfig().getShowHudOption());
		list.addOptionEntry(this.mod.getConfig().getHudScaleOption(), null);
		list.addOptionEntry(this.mod.getConfig().getNorthLockOption(), this.mod.getConfig().getDirectionIndicatorsOption());
		list.addSingleOptionEntry(this.mod.getConfig().getHudDecoratorOption());
		return list;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
}
