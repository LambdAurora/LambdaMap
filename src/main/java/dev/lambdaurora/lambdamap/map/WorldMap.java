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

package dev.lambdaurora.lambdamap.map;

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.gui.WorldMapScreen;
import dev.lambdaurora.lambdamap.map.marker.Marker;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.lambdamap.map.storage.MapRegionFile;
import dev.lambdaurora.lambdamap.mixin.MapColorAccessor;
import dev.lambdaurora.lambdamap.util.ClientWorldWrapper;
import dev.lambdaurora.spruceui.util.ColorUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Represents the world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class WorldMap {
	private static final Logger LOGGER = LogManager.getLogger();

	private static final int VIEW_RANGE = 10000;

	private final Long2ObjectMap<MapRegionFile> regionFiles = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectMap<MapChunk> chunks = new Long2ObjectOpenHashMap<>();
	private final MinecraftClient client = MinecraftClient.getInstance();
	private final File directory;
	private final MarkerManager markerManager;

	final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();

	private final World world;

	private double viewX = 0;
	private double viewZ = 0;
	private double playerViewX = 0;
	private double playerViewZ = 0;

	public WorldMap(World world, File directory) {
		this.directory = directory;
		if (!this.directory.exists())
			this.directory.mkdirs();
		this.markerManager = new MarkerManager(this);
		this.markerManager.load();

		this.world = world;
	}

	public File getDirectory() {
		return this.directory;
	}

	public MarkerManager getMarkerManager() {
		return this.markerManager;
	}

	public World getWorld() {
		return this.world;
	}

	public DynamicRegistryManager getRegistryManager() {
		return this.world.getRegistryManager();
	}

	public Registry<Biome> getBiomeRegistry() {
		return this.getRegistryManager().get(RegistryKeys.BIOME);
	}

	public double getViewX() {
		return this.viewX;
	}

	public double getViewZ() {
		return this.viewZ;
	}

	public boolean updateViewPos(double viewX, double viewZ) {
		boolean changed = viewX != this.viewX || viewZ != this.viewZ;
		this.viewX = viewX;
		this.viewZ = viewZ;
		return changed;
	}

	public boolean updatePlayerViewPos(int viewX, int viewZ, float threshold) {
		if (Math.abs(viewX - this.playerViewX) > threshold || Math.abs(viewZ - this.playerViewZ) > threshold) {
			this.playerViewX = viewX;
			this.playerViewZ = viewZ;
			if (!(client.currentScreen instanceof WorldMapScreen))
				this.updateViewPos(viewX, viewZ);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Returns the ARGB color at the specified coordinates.
	 * <p>
	 * Coordinates are absolute.
	 *
	 * @param x the X coordinate
	 * @param z the Z coordinate
	 * @param mode the chunk getter mode
	 * @return the ARGB color
	 */
	public int getRenderColor(int x, int z, ChunkGetterMode mode) {
		var chunk = mode.getChunk(this, MapChunk.blockToChunk(x), MapChunk.blockToChunk(z));
		if (chunk == null || chunk.isEmpty())
			return 0;
		int index = chunk.getIndex(x, z);
		int color = chunk.getColor(index) & 255;
		if (color / 4 == 0)
			return 0;
		else {
			var mapColor = MapColorAccessor.getColors()[color / 4];
			if (LambdaMap.get().getConfig().shouldRenderBiomeColors()) {
				if (mapColor == MapColor.WATER_BLUE) {
					var biome = chunk.getBiome(index);
					if (biome != null) {
						return this.calculateWaterColor(x, z, biome, color & 3, mode);
					}
				} else {
					var state = chunk.getBlockState(index);
					if (state != null) {
						int argb = 0xff000000 | this.client.getBlockColors().getColor(state, new ClientWorldWrapper(this.client.world, chunk), new BlockPos(x, 64, z), 0);
						return applyShade(ColorUtil.argbMultiply(argb, 0xffb9bcb9), color & 3);
					}
				}
			}
			return applyShade(mapColor.color, color & 3);
		}
	}

	private int calculateWaterColor(int x, int z, Biome sourceBiome, int shade, ChunkGetterMode mode) {
		int biomeBlendRadius = this.client.options.getBiomeBlendRadius().get();
		if (biomeBlendRadius == 0) {
			return applyShade(ColorUtil.argbDarken(sourceBiome.getWaterColor()), shade);
		} else {
			biomeBlendRadius = 2;
			int multiplier = (biomeBlendRadius * 2 + 1) * (biomeBlendRadius * 2 + 1);
			int r = 0;
			int g = 0;
			int b = 0;

			for (int offsetZ = -biomeBlendRadius; offsetZ < biomeBlendRadius; offsetZ++) {
				int resolveZ = z + offsetZ;
				for (int offsetX = -biomeBlendRadius; offsetX < biomeBlendRadius; offsetX++) {
					int resolveX = x + offsetX;
					var chunk = mode.getChunk(this, MapChunk.blockToChunk(resolveX), MapChunk.blockToChunk(resolveZ));
					if (chunk != null) {
						var biome = chunk.getBiome(chunk.getIndex(resolveX, resolveZ));
						if (biome != null) {
							int waterColor = biome.getWaterColor();
							r += (waterColor & 0x00ff0000) >> 16;
							g += (waterColor & 0x0000ff00) >> 8;
							b += waterColor & 0xff;
						}
					} else multiplier--;
				}
			}

			return applyShade(ColorUtil.packARGBColor(r / multiplier & 255, g / multiplier & 255, b / multiplier & 255, 0xff), shade);
		}
	}

	private static int applyShade(int color, int shade) {
		int modifier = 220;
		if (shade == 3) {
			modifier = 135;
		}

		if (shade == 2) {
			modifier = 255;
		}

		if (shade == 0) {
			modifier = 180;
		}

		int j = (color >> 16 & 255) * modifier / 255;
		int k = (color >> 8 & 255) * modifier / 255;
		int l = (color & 255) * modifier / 255;
		return -16777216 | l << 16 | k << 8 | j;
	}

	public @Nullable MapChunk getChunk(int x, int z) {
		return this.getChunk(ChunkPos.toLong(x, z));
	}

	public @Nullable MapChunk getChunk(long pos) {
		return this.chunks.get(pos);
	}

	public @Nullable MapChunk getChunkOrLoad(int x, int z) {
		return this.getChunkOrLoad(ChunkPos.toLong(x, z));
	}

	public @Nullable MapChunk getChunkOrLoad(long pos) {
		var chunk = this.getChunk(pos);
		if (chunk == null) {
			int x = ChunkPos.getPackedX(pos);
			int z = ChunkPos.getPackedZ(pos);
			chunk = MapChunk.load(this, x, z);
			if (chunk != null)
				this.chunks.put(pos, chunk);
		}
		return chunk;
	}

	public MapChunk getChunkOrCreate(int x, int z) {
		long pos = ChunkPos.toLong(x, z);
		var chunk = this.getChunk(pos);
		if (chunk == null) {
			chunk = MapChunk.loadOrCreate(this, x, z);
			this.chunks.put(pos, chunk);
		}
		return chunk;
	}

	public MapChunk getChunkOrCreate(long pos) {
		var chunk = this.getChunk(pos);
		if (chunk == null) {
			int x = ChunkPos.getPackedX(pos);
			int z = ChunkPos.getPackedZ(pos);
			chunk = MapChunk.loadOrCreate(this, x, z);
			this.chunks.put(pos, chunk);
		}
		return chunk;
	}

	public @Nullable MapRegionFile getOrLoadRegion(int x, int z) {
		x = MapChunk.chunkToRegion(x);
		z = MapChunk.chunkToRegion(z);
		long pos = ChunkPos.toLong(x, z);
		var regionFile = this.regionFiles.get(pos);

		if (regionFile == null) {
			try {
				regionFile = MapRegionFile.load(this, x, z);
				if (regionFile != null)
					this.regionFiles.put(pos, regionFile);
			} catch (IOException e) {
				LOGGER.error("Could not load or create region file (" + x + ", " + z + ")", e);
				return null;
			}
		}

		return regionFile;
	}

	public MapRegionFile getOrCreateRegion(int x, int z) {
		x = MapChunk.chunkToRegion(x);
		z = MapChunk.chunkToRegion(z);
		long pos = ChunkPos.toLong(x, z);
		var regionFile = this.regionFiles.get(pos);

		if (regionFile == null) {
			try {
				regionFile = MapRegionFile.loadOrCreate(this, x, z);
				this.regionFiles.put(pos, regionFile);
			} catch (IOException e) {
				LOGGER.error("Could not load or create region file (" + x + ", " + z + ")", e);
				return null;
			}
		}

		return regionFile;
	}

	public void unloadRegion(MapRegionFile regionFile) {
		this.regionFiles.remove(ChunkPos.toLong(regionFile.getX(), regionFile.getZ()));
	}

	public void importMapState(MapState mapState, List<Marker> markers) {
		int scale = 1 << mapState.scale;

		int cornerX = mapState.centerX - 64 * scale;
		int cornerZ = mapState.centerZ - 64 * scale;

		var marker = markers.get(0);
		for (var icon : mapState.getIcons()) {
			if (marker.getType() == MarkerType.getVanillaMarkerType(icon.getType())) {
				int iconX = (int) (icon.getX() / 2.f + 64) * scale;
				int iconZ = (int) (icon.getZ() / 2.f + 64) * scale;

				cornerX = marker.getX() - iconX;
				cornerZ = marker.getZ() - iconZ;
			}
		}

		for (int z = 0; z < 128 * scale; z++) {
			int i = z / scale;
			int chunkZ = MapChunk.blockToChunk(cornerZ + z);

			for (int x = 0; x < 128 * scale; x++) {
				byte color = mapState.colors[x / scale + i * 128];
				if (color / 4 == 0)
					continue;

				int chunkX = MapChunk.blockToChunk(cornerX + x);
				var chunk = this.getChunkOrCreate(chunkX, chunkZ);

				if (chunk.getColor(cornerX + x, cornerZ + z) / 4 == 0) {
					chunk.putColor(cornerX + x, cornerZ + z, color);
				}
			}
		}
	}

	public void tick() {
		var client = MinecraftClient.getInstance();
		int viewDistance = Math.max(2, client.options.getEffectiveViewDistance() - 2);

		int chunkX = ChunkSectionPos.getSectionCoord(this.playerViewX);
		int chunkZ = ChunkSectionPos.getSectionCoord(this.playerViewZ);

		int playerViewStartX = (chunkX - viewDistance) >> 3;
		int playerViewStartZ = (chunkZ - viewDistance) >> 3;
		int playerViewEndX = (chunkX + viewDistance) >> 3;
		int playerViewEndZ = (chunkZ + viewDistance) >> 3;

		int viewStartX = (int) (this.viewX - VIEW_RANGE);
		int viewStartZ = (int) (this.viewZ - VIEW_RANGE);
		int viewEndX = (int) (this.viewX + VIEW_RANGE);
		int viewEndZ = (int) (this.viewZ + VIEW_RANGE);

		boolean hasViewer = this.viewX != this.playerViewX || this.viewZ != this.playerViewZ;
		this.chunks.values().removeIf(chunk -> {
			if (!((chunk.getX() >= playerViewStartX && chunk.getX() <= playerViewEndX && chunk.getZ() >= playerViewStartZ && chunk.getZ() <= playerViewEndZ)
					|| (hasViewer && chunk.isCenterInBox(viewStartX, viewStartZ, viewEndX, viewEndZ)))) {
				chunk.unload();
				return true;
			}
			return false;
		});

		this.getMarkerManager().tick(this.world);
	}

	public void unload() {
		this.service.shutdown();
		this.markerManager.save();
		this.chunks.forEach((pos, chunk) -> chunk.unload());
		this.chunks.clear();
		this.regionFiles.clear();
	}
}
