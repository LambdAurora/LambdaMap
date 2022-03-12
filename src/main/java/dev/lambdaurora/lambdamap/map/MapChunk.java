/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
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

import dev.lambdaurora.lambdamap.map.storage.MapRegionFile;
import dev.lambdaurora.lambdamap.mixin.BlockColorsAccessor;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Represents a map chunk. A map chunk is 128x128 blocks represented by color IDs and shade value for height differences.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapChunk implements AutoCloseable {
	private static final int SIZE = 16384;

	private final WorldMap worldMap;
	private final int x;
	private final int z;
	private byte[] colors = new byte[SIZE];
	private final Biome[] biomes = new Biome[SIZE];
	private final BlockState[] blockStates = new BlockState[SIZE];
	private final MapRegionFile regionFile;
	private final ScheduledFuture<?> saveTask;
	private boolean locked = false;
	private boolean empty = true;
	private boolean dirty = false;

	public MapChunk(WorldMap worldMap, MapRegionFile regionFile, int x, int z) {
		this.worldMap = worldMap;
		this.x = x;
		this.z = z;
		this.regionFile = regionFile;

		if (this.regionFile != null) {
			this.regionFile.incrementLoadedChunk();
			// Auto-save every 6 minutes
			this.saveTask = this.worldMap.service.scheduleAtFixedRate(this::save, 6, 6, TimeUnit.MINUTES);
		} else {
			this.saveTask = null;
		}
	}

	/**
	 * Returns the map chunk X coordinate.
	 *
	 * @return the X coordinate
	 */
	public int getX() {
		return this.x;
	}

	/**
	 * Returns the map chunk Z coordinate.
	 *
	 * @return the Z coordinate
	 */
	public int getZ() {
		return this.z;
	}

	/**
	 * Returns block X coordinate at the start of the map chunk.
	 *
	 * @return the block X coordinate
	 */
	public int getStartX() {
		return this.x << 7;
	}

	/**
	 * Returns block Z coordinate at the start of the map chunk.
	 *
	 * @return the block Z coordinate
	 */
	public int getStartZ() {
		return this.z << 7;
	}

	public int getCenterX() {
		return this.getStartX() + 64;
	}

	public int getCenterZ() {
		return this.getStartZ() + 64;
	}

	public int getStartChunkX() {
		return this.x << 3;
	}

	public int getStartChunkZ() {
		return this.z << 3;
	}

	public boolean isBlockIn(int x, int z) {
		int startX = this.getStartX();
		int startZ = this.getStartZ();
		return startX <= x && startZ <= z && startX + 128 > x && startZ + 128 > z;
	}

	public boolean isCenterInBox(int startBoxX, int startBoxZ, int endBoxX, int endBoxZ) {
		int centerX = this.getCenterX();
		int centerZ = this.getCenterZ();
		return startBoxX <= centerX && centerX < endBoxX && startBoxZ <= centerZ && centerZ < endBoxZ;
	}

	public void lock() {
		this.locked = true;
	}

	public void unlock() {
		this.locked = false;
	}

	public boolean isEmpty() {
		return this.empty;
	}

	public void markDirty() {
		this.dirty = true;
	}

	protected int getIndex(int x, int z) {
		return (x & 127) + (z & 127) * 128;
	}

	/**
	 * Returns the color data at the specified index.
	 *
	 * @param index the index
	 * @return the color data
	 */
	protected byte getColor(int index) {
		return this.colors[index];
	}

	/**
	 * Returns the color data at the specified coordinates.
	 * <p>
	 * Coordinates can be absolute.
	 *
	 * @param x the X coordinate
	 * @param z the Z coordinate
	 * @return the color data
	 */
	public byte getColor(int x, int z) {
		return this.getColor(this.getIndex(x, z));
	}

	public boolean putColor(int x, int z, byte color) {
		if (this.locked)
			return false;
		if (color != 0 && this.empty)
			this.empty = false;
		int index = this.getIndex(x, z);
		if (this.colors[index] != color) {
			this.colors[index] = color;
			this.markDirty();
			return this.dirty;
		}
		return false;
	}

	protected @Nullable Biome getBiome(int index) {
		return this.biomes[index];
	}

	/**
	 * Returns the biome at the specified coordinates.
	 * <p>
	 * Coordinates can be absolute.
	 *
	 * @param x the X coordinate
	 * @param z the Z coordinate
	 * @return the biome if present, else {@code null}
	 */
	public @Nullable Biome getBiome(int x, int z) {
		return this.getBiome(this.getIndex(x, z));
	}

	public boolean putBiome(int x, int z, @Nullable Biome biome) {
		if (this.locked)
			return false;
		int index = this.getIndex(x, z);
		if (this.biomes[index] != biome) {
			this.biomes[index] = biome;
			this.markDirty();
			return this.dirty;
		}
		return false;
	}

	protected @Nullable BlockState getBlockState(int index) {
		return this.blockStates[index];
	}

	/**
	 * Returns the block state at the specified coordinates.
	 * <p>
	 * Coordinates can be absolute.
	 *
	 * @param x the X coordinate
	 * @param z the Z coordinate
	 * @return the block state if present, else {@code null}
	 */
	public @Nullable BlockState getBlockState(int x, int z) {
		return this.getBlockState(this.getIndex(x, z));
	}

	public boolean putBlockState(int x, int z, @Nullable BlockState state) {
		if (this.locked)
			return false;

		if (state != null && !filterBlockState(state)) {
			state = null; // Do not save unnecessary block states.
		}

		int index = this.getIndex(x, z);
		if (this.blockStates[index] != state) {
			this.blockStates[index] = state;
			this.markDirty();
			return this.dirty;
		}
		return false;
	}

	public boolean putPixelAndPreserve(int x, int z, byte color, @Nullable Biome biome, @Nullable BlockState state) {
		this.putColor(x, z, color);
		if (biome != null)
			this.putBiome(x, z, biome);
		if (state != null)
			this.putBlockState(x, z, state);
		return this.dirty;
	}

	/**
	 * Returns the map chunk as NBT.
	 *
	 * @return the map chunk as NBT
	 */
	public NbtCompound toNbt() {
		var nbt = new NbtCompound();
		nbt.putInt("x", this.x);
		nbt.putInt("z", this.z);
		nbt.putByteArray("colors", this.colors);

		this.writeBiomesNbt(nbt);
		this.writeBlockPaletteNbt(nbt);

		return nbt;
	}

	/**
	 * Writes the biomes to the NBT.
	 *
	 * @param nbt the parent compound NBT
	 */
	private void writeBiomesNbt(NbtCompound nbt) {
		var registry = this.worldMap.getBiomeRegistry();
		if (registry != null) {
			var biomes = new Object2ObjectOpenHashMap<Biome, IntList>();
			for (int i = 0; i < this.biomes.length; i++) {
				var biome = this.biomes[i];
				biomes.computeIfAbsent(biome, o -> new IntArrayList()).add(i);
			}

			var biomeList = new NbtList();
			biomes.forEach((biome, indices) -> {
				if (biome == null)
					return;
				var id = registry.getId(biome);
				if (indices.size() == SIZE) {
					if (id != null)
						nbt.putString("biome", id.toString());
				} else if (id != null) {
					var bitSet = new BitSet(SIZE);

					for (int i : indices) {
						bitSet.set(i);
					}

					var biomeNbt = new NbtCompound();
					biomeNbt.putString("biome", id.toString());
					biomeNbt.putByteArray("mask", bitSet.toByteArray());
					biomeList.add(biomeNbt);
				}
			});
			if (!biomeList.isEmpty())
				nbt.put("biomes", biomeList);
		}
	}

	/**
	 * Writes the block palette as NBT.
	 *
	 * @param nbt the parent compound NBT
	 */
	private void writeBlockPaletteNbt(NbtCompound nbt) {
		var palette = new ArrayList<BlockState>();
		for (var state : this.blockStates) {
			if (state != null) {
				if (!palette.contains(state))
					palette.add(state);
			}
		}

		var paletteNbt = new NbtList();
		palette.forEach(state -> paletteNbt.add(NbtHelper.fromBlockState(state)));
		nbt.put("palette", paletteNbt);

		int bits = Math.max(4, MathHelper.log2DeBruijn(paletteNbt.size() + 1));
		var blockStates = new SimpleBitStorage(bits, SIZE);

		for (int i = 0; i < SIZE; i++) {
			var state = this.blockStates[i];
			if (state == null) blockStates.set(i, 0);
			else blockStates.set(i, palette.indexOf(state) + 1);
		}

		nbt.putLongArray("block_states", blockStates.getRaw());
	}

	/**
	 * Saves the chunk if it's dirty.
	 */
	public void save() {
		if (this.empty || !this.dirty || this.regionFile == null)
			return;
		try {
			this.lock();
			this.regionFile.saveChunk(this);
			this.dirty = false;
			this.unlock();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void unload() {
		this.lock();
		if (this.regionFile != null) {
			this.saveTask.cancel(false);
			this.regionFile.unloadChunk(this);
		}
		this.dirty = false;
	}

	@Override
	public void close() {
		this.unload();
	}

	@Override
	public String toString() {
		return "MapChunk{" +
				"x=" + this.x +
				", z=" + this.z +
				", locked=" + this.locked +
				", empty=" + this.empty +
				", dirty=" + this.dirty +
				'}';
	}

	public static int blockToChunk(int coordinate) {
		return coordinate >> 7;
	}

	public static int chunkToRegion(int coordinate) {
		return coordinate >> 3;
	}

	public static MapChunk fromNbt(MapRegionFile regionFile, NbtCompound nbt) {
		var chunk = new MapChunk(regionFile.worldMap(), regionFile, nbt.getInt("x"), nbt.getInt("z"));
		byte[] colors = nbt.getByteArray("colors");
		if (colors.length == SIZE) {
			chunk.colors = colors;
		}
		chunk.empty = false;

		return readBlockPaletteNbt(readBiomesNbt(chunk, nbt), nbt);
	}

	private static MapChunk readBiomesNbt(MapChunk chunk, NbtCompound nbt) {
		var registry = chunk.worldMap.getBiomeRegistry();
		if (registry != null) {
			if (nbt.contains("biome", NbtType.STRING)) {
				var id = new Identifier(nbt.getString("biome"));
				var biome = registry.get(id);
				if (biome != null) {
					for (int i = 0; i < SIZE; i++) chunk.biomes[i] = biome;
				}
			} else if (nbt.contains("biomes", NbtType.LIST)) {
				var biomesList = nbt.getList("biomes", NbtType.COMPOUND);
				biomesList.stream().map(biomeNbt -> (NbtCompound) biomeNbt)
						.forEach(biomeNbt -> {
							var id = new Identifier(biomeNbt.getString("biome"));
							var biome = registry.get(id);
							if (biome == null) return;

							var bitSet = BitSet.valueOf(biomeNbt.getByteArray("mask"));
							for (int i = 0; i < SIZE; i++) {
								if (bitSet.get(i))
									chunk.biomes[i] = biome;
							}
						});
			}
		}
		return chunk;
	}

	private static MapChunk readBlockPaletteNbt(MapChunk chunk, NbtCompound nbt) {
		if (nbt.contains("palette", NbtType.LIST) && nbt.contains("block_states", NbtType.LONG_ARRAY)) {
			var paletteNbt = nbt.getList("palette", NbtType.COMPOUND);
			var palette = new Int2ObjectOpenHashMap<BlockState>();
			for (int i = 0; i < paletteNbt.size(); i++) {
				palette.put(i + 1, NbtHelper.toBlockState(paletteNbt.getCompound(i)));
			}

			int bits = Math.max(4, MathHelper.log2DeBruijn(paletteNbt.size() + 1));
			var blockStates = new SimpleBitStorage(bits, SIZE, nbt.getLongArray("block_states"));

			for (int i = 0; i < SIZE; i++) {
				int id = blockStates.get(i);
				if (id != 0) {
					chunk.blockStates[i] = palette.get(id);
				}
			}
		}
		return chunk;
	}

	public static @Nullable MapChunk load(WorldMap map, int x, int z) {
		var regionFile = map.getOrLoadRegion(x, z);
		if (regionFile != null)
			return regionFile.loadChunk(x, z);
		return null;
	}

	public static MapChunk loadOrCreate(WorldMap map, int x, int z) {
		var regionFile = map.getOrCreateRegion(x, z);
		if (regionFile == null)
			return new MapChunk(map, null, x, z);

		return regionFile.loadChunkOrCreate(x, z);
	}

	/**
	 * Returns whether the block state can be saved in a map chunk.
	 *
	 * @param state the block state
	 * @return {@code true} if the block state can be saved, else {@code false}
	 */
	private static boolean filterBlockState(BlockState state) {
		var client = MinecraftClient.getInstance();

		return ((BlockColorsAccessor) client.getBlockColors()).getProviders().get(Registry.BLOCK.getRawId(state.getBlock())) != null;
	}
}
