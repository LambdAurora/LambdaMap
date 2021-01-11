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

package me.lambdaurora.lambdamap.map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a map region. A map region contains 8x8 map chunks and is loaded/saved as a compressed NBT file.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapRegion implements AutoCloseable {
    private static final int CHUNKS = 8;
    private final MapChunk[][] chunks = new MapChunk[CHUNKS][CHUNKS];
    private final int x;
    private final int z;
    private final File file;
    private final Timer saveTimer;

    public MapRegion(WorldMap worldMap, int x, int z) {
        this.x = x;
        this.z = z;
        this.file = new File(worldMap.getDirectory(), "region_" + this.x + "_" + this.z + ".nbt");
        this.saveTimer = new Timer();
        // Auto-save every 6 minutes
        this.saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                MapRegion.this.save();
            }
        }, 360000, 360000);
    }

    public boolean isValidCoordinate(long pos) {
        int x = ChunkPos.getPackedX(pos);
        int z = ChunkPos.getPackedZ(pos);
        return x >> 3 == this.x && z >> 3 == this.z;
    }

    /**
     * Returns whether this region is empty or not.
     *
     * @return {@code true} if this region is empty, else {@code false}
     */
    public boolean isEmpty() {
        for (int x = 0; x < CHUNKS; x++) {
            for (int z = 0; z < CHUNKS; z++) {
                if (this.chunks[x][z] != null)
                    return false;
            }
        }
        return true;
    }

    public @Nullable MapChunk getChunk(long pos) {
        return this.getChunk(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
    }

    /**
     * Returns the map chunk at the specified coordinates.
     *
     * @param x the map chunk X coordinate
     * @param z the map chunk Z coordinate
     * @return the map chunk
     */
    public @Nullable MapChunk getChunk(int x, int z) {
        return this.chunks[x & 7][z & 7];
    }

    public MapChunk getChunkOrCreate(long pos) {
        return this.getChunkOrCreate(ChunkPos.getPackedX(pos), ChunkPos.getPackedZ(pos));
    }

    /**
     * Returns the map chunk at the specified coordinates. If the map chunk did not exist prior to this then a map chunk is created.
     *
     * @param x the map chunk X coordinate
     * @param z the map chunk Z coordinate
     * @return the map chunk
     */
    public @NotNull MapChunk getChunkOrCreate(int x, int z) {
        MapChunk chunk = this.getChunk(x, z);
        if (chunk == null) {
            chunk = new MapChunk(x, z);
            this.chunks[x & 7][z & 7] = chunk;
        }
        return chunk;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (int x = 0; x < CHUNKS; x++) {
            for (int z = 0; z < CHUNKS; z++) {
                MapChunk chunk = this.chunks[x][z];
                if (chunk != null) {
                    chunk.lock();
                    list.add(chunk.toTag());
                    chunk.unlock();
                }
            }
        }
        tag.put("chunks", list);
        return tag;
    }

    /**
     * Saves the region if it's not empty.
     */
    public void save() {
        if (this.isEmpty())
            return;
        try {
            NbtIo.writeCompressed(this.toTag(), this.file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static MapRegion fromTag(WorldMap worldMap, int x, int z, CompoundTag tag) {
        MapRegion region = new MapRegion(worldMap, x, z);
        tag.getList("chunks", 10).stream()
                .map(compound -> (CompoundTag) compound)
                .map(MapChunk::fromTag)
                .forEach(chunk -> {
                    region.chunks[chunk.getX() & 7][chunk.getZ() & 7] = chunk;
                });
        return region;
    }

    public static @Nullable MapRegion load(WorldMap worldMap, int x, int z) {
        File file = new File(worldMap.getDirectory(), "region_" + x + "_" + z + ".nbt");
        if (file.exists()) {
            try {
                return fromTag(worldMap, x, z, NbtIo.readCompressed(file));
            } catch (IOException e) {
            }
        }
        return null;
    }

    public static @NotNull MapRegion loadOrCreate(WorldMap worldMap, int x, int z) {
        MapRegion region = load(worldMap, x, z);
        return region == null ? new MapRegion(worldMap, x, z) : region;
    }

    @Override
    public void close() {
        this.saveTimer.cancel();
        this.save();
    }
}
