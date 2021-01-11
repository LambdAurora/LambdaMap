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

/**
 * Represents a map chunk. A map chunk is 128x128 blocks represented by color IDs and shade value for height differences.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapChunk {
    private final int x;
    private final int z;
    public byte[] colors = new byte[16384];
    private boolean locked = false;

    public MapChunk(int x, int z) {
        this.x = x;
        this.z = z;
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

    public static MapChunk fromTag(CompoundTag tag) {
        MapChunk chunk = new MapChunk(tag.getInt("x"), tag.getInt("z"));
        byte[] colors = tag.getByteArray("colors");
        if (colors.length == 16384) {
            chunk.colors = colors;
        }
        return chunk;
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
            return true;
        }
        return false;
    }
}
