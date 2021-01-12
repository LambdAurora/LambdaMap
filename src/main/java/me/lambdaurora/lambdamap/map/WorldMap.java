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
import me.lambdaurora.lambdamap.map.storage.MapRegionFile;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Represents the world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class WorldMap {
    private static final Logger LOGGER = LogManager.getLogger();

    private final Long2ObjectMap<MapRegionFile> regionFiles = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<MapChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final File directory;

    public WorldMap(File directory) {
        this.directory = directory;
        if (!this.directory.exists())
            this.directory.mkdirs();
    }

    public MapChunk getChunk(int x, int y) {
        return this.getChunk(ChunkPos.toLong(x, y));
    }

    public MapChunk getChunk(long pos) {
        return this.chunks.get(pos);
    }

    public @Nullable MapChunk getChunkOrLoad(int x, int y) {
        return this.getChunk(ChunkPos.toLong(x, y));
    }

    public @Nullable MapChunk getChunkOrLoad(long pos) {
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            chunk = MapChunk.load(getOrLoadRegion(x, z), x, z);
            if (chunk != null)
                this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public MapChunk getChunkOrCreate(int x, int z) {
        long pos = ChunkPos.toLong(x, z);
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            chunk = MapChunk.loadOrCreate(getOrLoadRegion(x, z), x, z);
            this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public MapChunk getChunkOrCreate(long pos) {
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            chunk = MapChunk.loadOrCreate(getOrLoadRegion(x, z), x, z);
            this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public MapRegionFile getOrLoadRegion(int x, int z) {
        x >>= 3;
        z >>= 3;
        MapRegionFile regionFile = this.regionFiles.get(ChunkPos.toLong(x, z));

        if (regionFile == null) {
            try {
                regionFile = MapRegionFile.loadOrCreate(this, x, z);
            } catch (IOException e) {
                LOGGER.error("Could not load or create region file (" + x + ", " + z + ")", e);
                return null;
            }
        }

        return regionFile;
    }

    public void unloadRegion(MapRegionFile regionFile) {
        this.regionFiles.remove(ChunkPos.toLong(regionFile.getX(),regionFile.getZ()));
    }

    public File getDirectory() {
        return this.directory;
    }

    public void save() {
        this.chunks.forEach((pos, chunk) -> chunk.save());

        this.regionFiles.forEach((pos, region) -> {
            try {
                region.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
