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

package dev.lambdaurora.lambdamap.map;

import dev.lambdaurora.lambdamap.map.storage.MapRegionFile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.PackedIntegerArray;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

/**
 * Represents a map chunk. A map chunk is 128x128 blocks represented by color IDs and shade value for height differences.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapChunk implements AutoCloseable {
    private static final int SIZE = 16384;

    private final int x;
    private final int z;
    private byte[] colors = new byte[SIZE];
    private final Biome[] biomes = new Biome[SIZE];
    private final BlockState[] blockStates = new BlockState[SIZE];
    private final MapRegionFile regionFile;
    private final Timer saveTimer;
    private boolean locked = false;
    private boolean empty = true;
    private boolean dirty = false;

    public MapChunk(MapRegionFile regionFile, int x, int z) {
        this.x = x;
        this.z = z;
        this.regionFile = regionFile;
        this.saveTimer = new Timer();

        if (this.regionFile != null) {
            this.regionFile.incrementLoadedChunk();
            // Auto-save every 6 minutes
            this.saveTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    MapChunk.this.save();
                }
            }, 360000, 360000);
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

    /**
     * Returns the map chunk as NBT.
     *
     * @return the map chunk as NBT
     */
    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", this.x);
        tag.putInt("z", this.z);
        tag.putByteArray("colors", this.colors);

        this.writeBiomesNbt(tag);
        this.writeBlockPaletteNbt(tag);

        return tag;
    }

    /**
     * Writes the biomes to the NBT.
     *
     * @param tag the parent compound tag
     */
    private void writeBiomesNbt(CompoundTag tag) {
        Registry<Biome> registry = getBiomesRegistry();
        if (registry != null) {
            Map<Biome, IntList> biomes = new Object2ObjectOpenHashMap<>();
            for (int i = 0; i < this.biomes.length; i++) {
                Biome biome = this.biomes[i];
                biomes.computeIfAbsent(biome, o -> new IntArrayList()).add(i);
            }

            ListTag biomeList = new ListTag();
            biomes.forEach((biome, indices) -> {
                if (biome == null)
                    return;
                Identifier id = registry.getId(biome);
                if (indices.size() == SIZE) {
                    if (id != null)
                        tag.putString("biome", id.toString());
                } else if (id != null) {
                    BitSet bitSet = new BitSet(SIZE);

                    for (int i : indices) {
                        bitSet.set(i);
                    }

                    CompoundTag biomeTag = new CompoundTag();
                    biomeTag.putString("biome", id.toString());
                    biomeTag.putByteArray("mask", bitSet.toByteArray());
                    biomeList.add(biomeTag);
                }
            });
            if (!biomeList.isEmpty())
                tag.put("biomes", biomeList);
        }
    }

    /**
     * Writes the block palette as NBT.
     *
     * @param tag the parent compound tag
     */
    private void writeBlockPaletteNbt(CompoundTag tag) {
        List<BlockState> palette = new ArrayList<>();
        for (BlockState state : this.blockStates) {
            if (state != null) {
                if (!palette.contains(state))
                    palette.add(state);
            }
        }

        ListTag paletteTag = new ListTag();
        palette.forEach(state -> paletteTag.add(NbtHelper.fromBlockState(state)));
        tag.put("palette", paletteTag);

        int bits = Math.max(4, MathHelper.log2DeBruijn(paletteTag.size() + 1));
        PackedIntegerArray blockStates = new PackedIntegerArray(bits, SIZE);

        for (int i = 0; i < SIZE; i++) {
            BlockState state = this.blockStates[i];
            if (state == null) blockStates.set(i, 0);
            else blockStates.set(i, palette.indexOf(state) + 1);
        }

        tag.putLongArray("block_states", blockStates.getStorage());
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
        if (this.regionFile != null)
            this.regionFile.unloadChunk(this);
        this.dirty = false;
    }

    @Override
    public void close() {
        this.saveTimer.cancel();
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

    public static MapChunk fromNbt(MapRegionFile regionFile, CompoundTag tag) {
        MapChunk chunk = new MapChunk(regionFile, tag.getInt("x"), tag.getInt("z"));
        byte[] colors = tag.getByteArray("colors");
        if (colors.length == SIZE) {
            chunk.colors = colors;
        }
        chunk.empty = false;

        return readBlockPaletteNbt(readBiomesNbt(chunk, tag), tag);
    }

    private static MapChunk readBiomesNbt(MapChunk chunk, CompoundTag tag) {
        Registry<Biome> registry = getBiomesRegistry();
        if (registry != null) {
            if (tag.contains("biome", NbtType.STRING)) {
                Identifier id = new Identifier(tag.getString("biome"));
                Biome biome = registry.get(id);
                if (biome != null) {
                    for (int i = 0; i < SIZE; i++) chunk.biomes[i] = biome;
                }
            } else if (tag.contains("biomes", NbtType.LIST)) {
                ListTag biomesList = tag.getList("biomes", NbtType.COMPOUND);
                biomesList.stream().map(biomeTag -> (CompoundTag) biomeTag)
                        .forEach(biomeTag -> {
                            Identifier id = new Identifier(biomeTag.getString("biome"));
                            Biome biome = registry.get(id);
                            if (biome == null) return;

                            BitSet bitSet = BitSet.valueOf(biomeTag.getByteArray("mask"));
                            for (int i = 0; i < SIZE; i++) {
                                if (bitSet.get(i))
                                    chunk.biomes[i] = biome;
                            }
                        });
            }
        }
        return chunk;
    }

    private static MapChunk readBlockPaletteNbt(MapChunk chunk, CompoundTag tag) {
        if (tag.contains("palette", NbtType.LIST) && tag.contains("block_states", NbtType.LONG_ARRAY)) {
            ListTag paletteTag = tag.getList("palette", NbtType.COMPOUND);
            Int2ObjectMap<BlockState> palette = new Int2ObjectOpenHashMap<>();
            for (int i = 0; i < paletteTag.size(); i++) {
                palette.put(i + 1, NbtHelper.toBlockState(paletteTag.getCompound(i)));
            }

            int bits = Math.max(4, MathHelper.log2DeBruijn(paletteTag.size() + 1));
            PackedIntegerArray blockStates = new PackedIntegerArray(bits, SIZE, tag.getLongArray("block_states"));

            for (int i = 0; i < SIZE; i++) {
                int id = blockStates.get(i);
                if (id != 0) {
                    chunk.blockStates[i] = palette.get(id);
                }
            }
        }
        return chunk;
    }

    public static @Nullable MapChunk load(MapRegionFile regionFile, int x, int z) {
        if (regionFile != null)
            return regionFile.loadChunk(x, z);
        return null;
    }

    public static @NotNull MapChunk loadOrCreate(MapRegionFile regionFile, int x, int z) {
        if (regionFile == null)
            return new MapChunk(null, x, z);

        return regionFile.loadChunkOrCreate(x, z);
    }

    public static @Nullable Registry<Biome> getBiomesRegistry() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            return client.getNetworkHandler().getRegistryManager().get(Registry.BIOME_KEY);
        }
        return null;
    }

    /**
     * Returns whether the block state can be saved in a map chunk.
     *
     * @param state the block state
     * @return {@code true} if the block state can be saved, else {@code false}
     */
    private static boolean filterBlockState(BlockState state) {
        if (state.getBlock() == Blocks.GRASS_BLOCK
                || state.getBlock() == Blocks.GRASS
                || state.getBlock() == Blocks.TALL_GRASS
                || state.getBlock() == Blocks.VINE)
            return true;
        return state.isIn(BlockTags.LEAVES);
    }
}
