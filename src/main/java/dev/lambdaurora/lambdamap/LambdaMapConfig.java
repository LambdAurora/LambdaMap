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
import dev.lambdaurora.spruceui.option.SpruceCheckboxBooleanOption;
import dev.lambdaurora.spruceui.option.SpruceOption;
import net.minecraft.text.TranslatableText;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final boolean DEFAULT_NORTH_LOCK = false;

	public static final Path CONFIG_FILE_PATH = Paths.get(LambdaMap.NAMESPACE, "config.toml");

	private static final Logger LOGGER = LogManager.getLogger();

	private final LambdaMap mod;
	private final FileConfig config;

	private final SpruceOption renderBiomeColorsOption;
	private final SpruceOption showHudOption;
	private final SpruceOption northLockOption;

	private boolean renderBiomeColors;
	private boolean showHud;
	private boolean northLock;

	public LambdaMapConfig(LambdaMap mod) {
		this.mod = mod;
		this.config = FileConfig.builder(CONFIG_FILE_PATH).defaultResource("/lambdamap.toml").autosave().build();

		this.renderBiomeColorsOption = new SpruceCheckboxBooleanOption("lambdamap.config.render_biome_colors",
				this::shouldRenderBiomeColors, value -> {
			this.setRenderBiomeColors(value);
			this.mod.getRenderer().update();
			this.mod.hud.markDirty();
		}, null, true);
		this.showHudOption = new SpruceCheckboxBooleanOption("lambdamap.config.hud.visible",
				this::isHudVisible, this::setHudVisible, null, true);
		this.northLockOption = new SpruceCheckboxBooleanOption("lambdamap.config.hud.north_lock",
				this::isNorthLocked, this::setNorthLock,
				new TranslatableText("lambdamap.config.hud.north_lock.tooltip"), true);
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
		this.northLock = this.config.getOrElse("map.hud.north_lock", DEFAULT_NORTH_LOCK);

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
}
