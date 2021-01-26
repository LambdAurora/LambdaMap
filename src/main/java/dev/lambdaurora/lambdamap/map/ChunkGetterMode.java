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

import org.jetbrains.annotations.Nullable;

/**
 * Represents a chunk getter mode.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public enum ChunkGetterMode {
    /**
     * Gets the chunk from memory.
     */
    GET(WorldMap::getChunk),
    /**
     * Gets the chunk from memory, or if absent loads the chunk from disk.
     */
    LOAD(WorldMap::getChunkOrLoad),
    /**
     * Gets or loads the chunk, if absent creates a new empty chunk.
     */
    CREATE(WorldMap::getChunkOrCreate);

    private final ChunkFactory factory;

    ChunkGetterMode(ChunkFactory factory) {
        this.factory = factory;
    }

    public @Nullable MapChunk getChunk(WorldMap map, int x, int z) {
        return this.factory.getChunk(map, x, z);
    }

    @FunctionalInterface
    public interface ChunkFactory {
        @Nullable MapChunk getChunk(WorldMap map, int x, int z);
    }
}
