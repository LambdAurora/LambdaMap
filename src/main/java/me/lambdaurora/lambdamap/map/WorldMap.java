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

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Represents the world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class WorldMap {
    private final Long2ObjectMap<MapRegion> regions = new Long2ObjectOpenHashMap<>();
    private final File directory;

    public WorldMap(File directory) {
        this.directory = directory;
        if (!this.directory.exists())
            this.directory.mkdirs();
    }

    private static long chunkToRegion(long pos) {
        return ChunkPos.toLong(ChunkPos.getPackedX(pos) >> 3, ChunkPos.getPackedZ(pos) >> 3);
    }

    public MapChunk getChunk(int x, int y) {
        return this.getChunk(ChunkPos.toLong(x, y));
    }

    public MapChunk getChunk(long pos) {
        MapRegion region = this.regions.get(chunkToRegion(pos));
        if (region != null)
            return region.getChunk(pos);
        return null;
    }

    public @Nullable MapChunk getChunkOrLoad(int x, int y) {
        return this.getChunk(ChunkPos.toLong(x, y));
    }

    public @Nullable MapChunk getChunkOrLoad(long pos) {
        long regionPos = chunkToRegion(pos);
        MapRegion region = this.regions.get(regionPos);
        if (region == null) {
            region = this.tryLoad(ChunkPos.getPackedX(regionPos), ChunkPos.getPackedZ(regionPos));
            if (region == null)
                return null;
            this.regions.put(regionPos, region);
        }
        return region.getChunkOrCreate(pos);
    }

    public MapChunk getChunkOrCreate(int x, int z) {
        return this.getChunkOrCreate(ChunkPos.toLong(x, z));
    }

    public MapChunk getChunkOrCreate(long pos) {
        long regionPos = chunkToRegion(pos);
        MapRegion region = this.regions.get(regionPos);
        if (region == null) {
            region = this.tryLoadOrCreate(ChunkPos.getPackedX(regionPos), ChunkPos.getPackedZ(regionPos));
            this.regions.put(regionPos, region);
        }
        return region.getChunkOrCreate(pos);
    }

    public MapRegion getRegion(long pos) {
        return this.regions.get(pos);
    }

    public @Nullable MapRegion tryLoad(int x, int y) {
        return MapRegion.load(this, x, y);
    }

    public @NotNull MapRegion tryLoadOrCreate(int x, int y) {
        return MapRegion.loadOrCreate(this, x, y);
    }

    public File getDirectory() {
        return this.directory;
    }

    public void save() {
        this.regions.forEach((pos, region) -> region.save());
    }
}
