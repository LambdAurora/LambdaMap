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

import dev.lambdaurora.lambdamap.gui.WorldMapScreen;
import dev.lambdaurora.lambdamap.map.marker.MarkerManager;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.lambdamap.map.storage.MapRegionFile;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import dev.lambdaurora.lambdamap.map.marker.Marker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.map.MapIcon;
import net.minecraft.item.map.MapState;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Represents the world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class WorldMap {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PLAYER_RANGE = 128 + 32;
    private static final int VIEW_RANGE = 12800;

    private final Long2ObjectMap<MapRegionFile> regionFiles = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectMap<MapChunk> chunks = new Long2ObjectOpenHashMap<>();
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final File directory;
    private final MarkerManager markerManager;

    private int viewX = 0;
    private int viewZ = 0;
    private int playerViewX = 0;
    private int playerViewZ = 0;

    public WorldMap(File directory) {
        this.directory = directory;
        if (!this.directory.exists())
            this.directory.mkdirs();
        this.markerManager = new MarkerManager(this);
        this.markerManager.load();
    }

    public File getDirectory() {
        return this.directory;
    }

    public MarkerManager getMarkerManager() {
        return this.markerManager;
    }

    public int getViewX() {
        return this.viewX;
    }

    public int getViewZ() {
        return this.viewZ;
    }

    public boolean updateViewPos(int viewX, int viewZ) {
        boolean changed = viewX != this.viewX || viewZ != this.viewZ;
        this.viewX = viewX;
        this.viewZ = viewZ;
        return changed;
    }

    public boolean updatePlayerViewPos(int viewX, int viewZ) {
        boolean changed = viewX != this.playerViewX || viewZ != this.playerViewZ;
        this.playerViewX = viewX;
        this.playerViewZ = viewZ;
        if (!(client.currentScreen instanceof WorldMapScreen))
            this.updateViewPos(viewX, viewZ);
        return changed;
    }

    public MapChunk getChunk(int x, int y) {
        return this.getChunk(ChunkPos.toLong(x, y));
    }

    public MapChunk getChunk(long pos) {
        return this.chunks.get(pos);
    }

    public @Nullable MapChunk getChunkOrLoad(int x, int y) {
        return this.getChunkOrLoad(ChunkPos.toLong(x, y));
    }

    public @Nullable MapChunk getChunkOrLoad(long pos) {
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            chunk = MapChunk.load(this.getOrLoadRegion(x, z), x, z);
            if (chunk != null)
                this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public MapChunk getChunkOrCreate(int x, int z) {
        long pos = ChunkPos.toLong(x, z);
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            chunk = MapChunk.loadOrCreate(this.getOrCreateRegion(x, z), x, z);
            this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public MapChunk getChunkOrCreate(long pos) {
        MapChunk chunk = this.getChunk(pos);
        if (chunk == null) {
            int x = ChunkPos.getPackedX(pos);
            int z = ChunkPos.getPackedZ(pos);
            chunk = MapChunk.loadOrCreate(this.getOrCreateRegion(x, z), x, z);
            this.chunks.put(pos, chunk);
        }
        return chunk;
    }

    public @Nullable MapRegionFile getOrLoadRegion(int x, int z) {
        x >>= 3;
        z >>= 3;
        long pos = ChunkPos.toLong(x, z);
        MapRegionFile regionFile = this.regionFiles.get(pos);

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
        x >>= 3;
        z >>= 3;
        long pos = ChunkPos.toLong(x, z);
        MapRegionFile regionFile = this.regionFiles.get(pos);

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

        int cornerX = mapState.xCenter - 64 * scale;
        int cornerZ = mapState.zCenter - 64 * scale;

        Marker marker = markers.get(0);
        for (MapIcon icon : mapState.method_32373()) {
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
                MapChunk chunk = this.getChunkOrCreate(chunkX, chunkZ);

                if (chunk.getColor(cornerX + x, cornerZ + z) / 4 == 0) {
                    chunk.putColor(cornerX + x, cornerZ + z, color);
                }
            }
        }
    }

    public void tick(@Nullable ClientWorld world) {
        int playerViewStartX = this.playerViewX - PLAYER_RANGE;
        int playerViewStartZ = this.playerViewZ - PLAYER_RANGE;
        int playerViewEndX = this.playerViewX + PLAYER_RANGE;
        int playerViewEndZ = this.playerViewZ + PLAYER_RANGE;

        int viewStartX = this.viewX - VIEW_RANGE;
        int viewStartZ = this.viewZ - VIEW_RANGE;
        int viewEndX = this.viewX + VIEW_RANGE;
        int viewEndZ = this.viewZ + VIEW_RANGE;

        boolean hasViewer = this.viewX != this.playerViewX || this.viewZ != this.playerViewZ;
        this.chunks.long2ObjectEntrySet().removeIf(entry -> {
            if (!(entry.getValue().isCenterInBox(playerViewStartX, playerViewStartZ, playerViewEndX, playerViewEndZ)
                    || (hasViewer && entry.getValue().isCenterInBox(viewStartX, viewStartZ, viewEndX, viewEndZ)))) {
                entry.getValue().unload();
                return true;
            }
            return false;
        });

        if (world != null) {
            this.getMarkerManager().tick(world);
        }
    }

    public void unload() {
        this.markerManager.save();
        this.chunks.forEach((pos, chunk) -> chunk.unload());
        this.chunks.clear();
        this.regionFiles.clear();
    }
}
