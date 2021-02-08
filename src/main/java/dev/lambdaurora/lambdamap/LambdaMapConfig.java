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

package dev.lambdaurora.lambdamap;

import com.electronwill.nightconfig.core.file.FileConfig;
import me.lambdaurora.spruceui.option.SpruceCheckboxBooleanOption;
import me.lambdaurora.spruceui.option.SpruceOption;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

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

    public static final Path CONFIG_FILE_PATH = Paths.get(LambdaMap.NAMESPACE, "config.toml");

    private static final Logger LOGGER = LogManager.getLogger();

    private final LambdaMap mod;
    private final FileConfig config;

    private final SpruceOption renderBiomeColorsOption;

    private boolean renderBiomeColors;

    public LambdaMapConfig(@NotNull LambdaMap mod) {
        this.mod = mod;
        this.config = FileConfig.builder(CONFIG_FILE_PATH).defaultResource("/lambdamap.toml").autosave().build();

        this.renderBiomeColorsOption = new SpruceCheckboxBooleanOption("lambdamap.config.render_biome_colors",
                this::shouldRenderBiomeColors, value -> {
            this.setRenderBiomeColors(value);
            this.mod.getRenderer().update();
            this.mod.hud.markDirty();
        }, null, true);
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
}
