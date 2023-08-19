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

package dev.lambdaurora.lambdamap;

import com.electronwill.nightconfig.core.file.FileConfig;
import dev.lambdaurora.lambdamap.gui.hud.HudDecorator;
import dev.lambdaurora.lambdamap.gui.hud.HudDecorators;
import dev.lambdaurora.spruceui.option.SpruceCheckboxBooleanOption;
import dev.lambdaurora.spruceui.option.SpruceCyclingOption;
import dev.lambdaurora.spruceui.option.SpruceOption;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Represents the mod configuration.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LambdaMapConfig {
	private static final boolean DEFAULT_RENDER_BIOME_COLORS = true;
	private static final boolean DEFAULT_SHOW_HUD = true;
	private static final boolean DEFAULT_SHOW_MARKER_EDITOR = false;
	private static final int DEFAULT_HUD_SCALE = 2;
	private static final boolean DEFAULT_SHOW_DIRECTION_INDICATORS = true;
	private static final boolean DEFAULT_NORTH_LOCK = false;

	public static final Path CONFIG_FILE_PATH = Paths.get(LambdaMap.NAMESPACE, "config.toml");

	private static final Logger LOGGER = LogManager.getLogger();

	private final LambdaMap mod;
	private final FileConfig config;

	private final SpruceOption renderBiomeColorsOption;
	private final SpruceOption showHudOption;
	private final SpruceOption showMarkerEditorOption;
	private final SpruceOption hudScaleOption;
	private final SpruceOption northLockOption;
	private final SpruceOption directionIndicatorsOption;
	private final SpruceOption hudDecoratorOption;

	private boolean renderBiomeColors;
	private boolean showHud;
	private boolean showMarkerEditor;
	private int hudScale;
	private boolean northLock;
	private boolean showDirectionIndicators;
	private HudDecorator hudDecorator;

	public LambdaMapConfig(LambdaMap mod) {
		this.mod = mod;
		this.config = FileConfig.builder(CONFIG_FILE_PATH).defaultResource("/lambdamap.toml").autosave().build();

		this.renderBiomeColorsOption = new SpruceCheckboxBooleanOption("lambdamap.config.render_biome_colors",
				this::shouldRenderBiomeColors, value -> {
			this.setRenderBiomeColors(value);
			this.mod.getRenderer().update(true);
			this.mod.hud.markDirty();
		}, null, true);
		this.showHudOption = new SpruceCheckboxBooleanOption("lambdamap.config.hud.visible",
				this::isHudVisible, this::setHudVisible, null, true);
		this.showMarkerEditorOption = new SpruceCheckboxBooleanOption("lambdamap.config.show_marker_editor",
				this::isMarkerEditorVisible, this::setMarkerEditorVisible, null, true);
		this.hudScaleOption = new SpruceCyclingOption("lambdamap.config.hud.scale",
				amount -> this.setHudScale((this.hudScale + amount) % 4), option -> option.getDisplayText(Text.literal(String.valueOf(this.getHudScale()))),
				Text.translatable("lambdamap.config.hud.scale.tooltip"));
		this.northLockOption = new SpruceCheckboxBooleanOption("lambdamap.config.hud.north_lock",
				this::isNorthLocked, this::setNorthLock,
				Text.translatable("lambdamap.config.hud.north_lock.tooltip"), true);
		this.directionIndicatorsOption = new SpruceCheckboxBooleanOption("lambdamap.config.hud.direction_indicators",
				this::isDirectionIndicatorsVisible, this::setDirectionIndicatorsVisible,
				Text.translatable("lambdamap.config.hud.direction_indicators.tooltip"), true);
		this.hudDecoratorOption = new SpruceCyclingOption("lambdamap.config.hud.decorator",
				amount -> this.setHudDecorator(HudDecorators.pick(this.getHudDecorator(), amount)),
				option -> option.getDisplayText(this.getHudDecorator().getName()),
				Text.translatable("lambdamap.config.hud.decorator.tooltip"));
	}

	/**
	 * Loads the configuration.
	 */
	public void load() {
		if (!Files.exists(CONFIG_FILE_PATH.getParent())) {
			try {
				Files.createDirectory(CONFIG_FILE_PATH.getParent());
			} catch (IOException e) {
				LOGGER.error("Could not create parent directory for configuration.", e);
			}
		}

		this.config.load();

		this.renderBiomeColors = this.config.getOrElse("map.render_biome_colors", DEFAULT_RENDER_BIOME_COLORS);
		this.showHud = this.config.getOrElse("map.hud.visible", DEFAULT_SHOW_HUD);
		this.showMarkerEditor = this.config.getOrElse("map.config.show_marker_editor", DEFAULT_SHOW_MARKER_EDITOR);
		this.hudScale = MathHelper.clamp(this.config.getIntOrElse("map.hud.scale", DEFAULT_HUD_SCALE), 1, 3);
		this.northLock = this.config.getOrElse("map.hud.north_lock", DEFAULT_NORTH_LOCK);
		this.showDirectionIndicators = this.config.getOrElse("map.hud.direction_indicators", DEFAULT_SHOW_DIRECTION_INDICATORS);
		this.hudDecorator = this.config.getOptional("map.hud.decorator")
				.map(o -> {
					if (o instanceof String name) {
						return Identifier.tryParse(name);
					}

					return null;
				}).map(HudDecorators::get)
				.orElse(HudDecorators.MAP);

		LOGGER.info("Configuration loaded.");
	}

	/**
	 * Saves the configuration.
	 */
	public void save() {
		this.config.save();
	}

	/**
	 * Resets the configuration.
	 */
	public void reset() {
		this.setRenderBiomeColors(DEFAULT_RENDER_BIOME_COLORS);
		this.setHudVisible(DEFAULT_SHOW_HUD);
		this.setHudScale(DEFAULT_HUD_SCALE);
		this.setNorthLock(DEFAULT_NORTH_LOCK);
	}

	public boolean shouldRenderBiomeColors() {
		return this.renderBiomeColors;
	}

	public void setRenderBiomeColors(boolean renderBiomeColors) {
		this.renderBiomeColors = renderBiomeColors;
		this.config.set("map.render_biome_colors", renderBiomeColors);
	}

	public SpruceOption getRenderBiomeColorsOption() {
		return this.renderBiomeColorsOption;
	}

	/**
	 * {@return {@code true} if the map HUD is visible, otherwise {@code false}}
	 */
	public boolean isHudVisible() {
		return this.showHud;
	}

	public void setHudVisible(boolean visible) {
		this.showHud = visible;
		this.config.set("map.hud.visible", visible);
	}

	public SpruceOption getShowHudOption() {
		return this.showHudOption;
	}


	public void setMarkerEditorVisible(Boolean visible) {
		this.showMarkerEditor = visible;
		this.config.set("map.config.show_marker_editor", visible);
	}

	public Boolean isMarkerEditorVisible() {
		return this.showMarkerEditor;
	}

	public SpruceOption getShowMarkerEditorOption() {
		return this.showMarkerEditorOption;
	}

	/**
	 * {@return the scale of the map HUD}
	 */
	public int getHudScale() {
		return this.hudScale;
	}

	/**
	 * Sets the scale of the map HUD
	 *
	 * @param scale the scale
	 */
	public void setHudScale(@Range(from = 1, to = 3) int scale) {
		this.hudScale = MathHelper.clamp(scale, 1, 3);
		this.config.set("map.hud.scale", this.hudScale);
	}

	public SpruceOption getHudScaleOption() {
		return this.hudScaleOption;
	}

	/**
	 * {@return {@code true} if the map is locked in place with North being towards up, otherwise {@code false}}
	 */
	public boolean isNorthLocked() {
		return this.northLock;
	}

	public void setNorthLock(boolean northLock) {
		this.northLock = northLock;
		this.config.set("map.hud.north_lock", northLock);
	}

	public SpruceOption getNorthLockOption() {
		return this.northLockOption;
	}

	/**
	 * {@return {@code true} if the direction indicators are shown on the map HUD, otherwise {@code false}}
	 */
	public boolean isDirectionIndicatorsVisible() {
		return this.showDirectionIndicators;
	}

	public void setDirectionIndicatorsVisible(boolean directionIndicatorsVisible) {
		this.showDirectionIndicators = directionIndicatorsVisible;
		this.config.set("map.hud.direction_indicators", directionIndicatorsVisible);
	}

	public SpruceOption getDirectionIndicatorsOption() {
		return this.directionIndicatorsOption;
	}

	/**
	 * {@return the map HUD decorator}
	 */
	public HudDecorator getHudDecorator() {
		return this.hudDecorator;
	}

	/**
	 * Sets the map HUD decorator.
	 *
	 * @param decorator the decorator to use
	 */
	public void setHudDecorator(HudDecorator decorator) {
		this.hudDecorator = decorator;
		this.config.set("map.hud.decorator", decorator.getId().toString());
	}

	public SpruceOption getHudDecoratorOption() {
		return this.hudDecoratorOption;
	}
}
