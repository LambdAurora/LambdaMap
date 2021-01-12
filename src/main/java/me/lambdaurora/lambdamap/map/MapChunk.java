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

import me.lambdaurora.lambdamap.map.storage.MapRegionFile;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Represents a map chunk. A map chunk is 128x128 blocks represented by color IDs and shade value for height differences.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapChunk implements AutoCloseable {
    private final int x;
    private final int z;
    public byte[] colors = new byte[16384];
    private final MapRegionFile regionFile;
    private final Timer saveTimer;
    private boolean locked = false;
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

    public int getStartX() {
        return this.x << 7;
    }

    public int getStartZ() {
        return this.z << 7;
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

    public void lock() {
        this.locked = true;
    }

    public void unlock() {
        this.locked = false;
    }

    public void markDirty() {
        this.dirty = true;
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("x", this.x);
        tag.putInt("z", this.z);
        tag.putByteArray("colors", this.colors);
        return tag;
    }

    public boolean putColor(int x, int z, byte color) {
        if (this.locked)
            return false;
        if (this.colors[x + z * 128] != color) {
            this.colors[x + z * 128] = color;
            this.markDirty();
            return this.dirty;
        }
        return false;
    }

    /**
     * Saves the chunk if it's dirty.
     */
    public void save() {
        if (!this.dirty || this.regionFile == null)
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
                "x=" + x +
                ", z=" + z +
                ", locked=" + locked +
                ", dirty=" + dirty +
                '}';
    }

    public static MapChunk fromTag(MapRegionFile regionFile, CompoundTag tag) {
        MapChunk chunk = new MapChunk(regionFile, tag.getInt("x"), tag.getInt("z"));
        byte[] colors = tag.getByteArray("colors");
        if (colors.length == 16384) {
            chunk.colors = colors;
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
}
