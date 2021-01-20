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

package dev.lambdaurora.lambdamap.map.storage;

import dev.lambdaurora.lambdamap.map.MapChunk;
import dev.lambdaurora.lambdamap.map.WorldMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * Represents a region file.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MapRegionFile implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int VERSION = 0;
    private static final int CHUNKS = 8;
    private static final int HEADER_SIZE = 1024;
    private static final long INVALID_CHUNK = 0xffffffff;

    private final WorldMap worldMap;
    private final RandomAccessFile raf;
    private final Header header;
    private int loadedChunks = 0;

    public MapRegionFile(WorldMap worldMap, RandomAccessFile raf, Header header) {
        this.worldMap = worldMap;
        this.raf = raf;
        this.header = header;
    }

    public int getX() {
        return this.header.getX();
    }

    public int getZ() {
        return this.header.getZ();
    }

    public void incrementLoadedChunk() {
        this.loadedChunks++;
    }

    private static MapRegionFile open(WorldMap worldMap, int x, int z, File file) throws IOException {
        boolean exists = file.exists();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        raf.seek(0);
        Header header = new Header(raf.getChannel(), x, z);
        if (!exists) {
            header.writeDefault();
        } else {
            header.read();
        }

        return new MapRegionFile(worldMap, raf, header);
    }

    /**
     * Loads the region file if found.
     *
     * @param worldMap the world map
     * @param x the region X-coordinate
     * @param z the region Z-coordinate
     * @return the region file if exists, else {@code null}
     * @throws IOException if the file cannot be created or opened or if the header fails to be written/read
     */
    public static @Nullable MapRegionFile load(WorldMap worldMap, int x, int z) throws IOException {
        File file = new File(worldMap.getDirectory(), "region_" + x + "_" + z + ".lmr");
        if (file.exists())
            return open(worldMap, x, z, file);
        return null;
    }

    /**
     * Loads the region file if found, or create a new one.
     *
     * @param worldMap the world map
     * @param x the region X-coordinate
     * @param z the region Z-coordinate
     * @return the region file
     * @throws IOException if the file cannot be created or opened or if the header fails to be written/read
     */
    public static MapRegionFile loadOrCreate(WorldMap worldMap, int x, int z) throws IOException {
        File file = new File(worldMap.getDirectory(), "region_" + x + "_" + z + ".lmr");
        return open(worldMap, x, z, file);
    }

    public synchronized @Nullable MapChunk loadChunk(int x, int z) {
        long chunkPos = this.header.getChunkEntry(x, z);
        if (chunkPos == INVALID_CHUNK) {
            return null;
        }

        try {
            this.raf.seek(HEADER_SIZE + chunkPos);
            int size = this.raf.readInt();
            if (size < 0) {
                LOGGER.error("Chunk ({}, {}) has an invalid size: {}", x, z, size);
                return null;
            }
            byte[] chunkBytes = new byte[size];
            int readBytes = this.raf.read(chunkBytes);
            if (readBytes != size) {
                LOGGER.error("Chunk ({}, {}) is truncated: expected {} but read {}", x, z, size, readBytes);
                return null;
            }

            CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(chunkBytes));
            return MapChunk.fromTag(this, tag);
        } catch (IOException e) {
            LOGGER.error("Failed to load chunk (" + x + ", " + z + ")", e);
        }
        return null;
    }

    public @NotNull MapChunk loadChunkOrCreate(int x, int z) {
        MapChunk chunk = this.loadChunk(x, z);
        if (chunk == null) {
            chunk = new MapChunk(this, x, z);
        }
        return chunk;
    }

    public synchronized void unloadChunk(MapChunk chunk) {
        this.loadedChunks--;
        try {
            this.saveChunk(chunk);
        } catch (IOException e) {
            LOGGER.error("Could not save chunk " + chunk, e);
        }

        if (this.loadedChunks == 0) {
            try {
                this.close();
            } catch (IOException e) {
                LOGGER.error("Failed to close region file (" + this.getX() + ", " + this.getZ() + ")", e);
            }
        }
    }

    public synchronized void saveChunk(MapChunk chunk) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(8096);
        chunk.lock();
        NbtIo.writeCompressed(chunk.toTag(), stream);
        chunk.unlock();

        int size = stream.size();
        long chunkPos = this.header.getChunkEntry(chunk.getX(), chunk.getZ());
        if (chunkPos == INVALID_CHUNK) {
            chunkPos = Math.max(this.raf.length(), HEADER_SIZE);
            this.header.writeChunkEntry(chunk.getX(), chunk.getZ(), chunkPos - HEADER_SIZE);
        } else {
            chunkPos += HEADER_SIZE;
            this.raf.seek(chunkPos);
            this.shiftChunksIfNeeded(
                    chunkPos,
                    this.raf.readInt() + 4, // Read old size of the chunk, including the size int.
                    size + 4);
        }

        this.raf.seek(chunkPos);
        this.raf.writeInt(size);
        this.raf.write(stream.toByteArray());
        this.header.write();
        stream.close();
    }

    public void shiftChunksIfNeeded(long pos, long oldSize, long newSize) throws IOException {
        long delta = newSize - oldSize;

        if (delta <= 0 || this.raf.length() == pos + oldSize) {
            return;
        }

        for (int i = 0; i < this.header.getEntriesCount(); i++) {
            long offset = this.header.getChunkEntry(i);
            if (offset != INVALID_CHUNK && pos < offset + HEADER_SIZE) {
                this.header.writeChunkEntry(i, offset + delta);
            }
        }

        long size = this.raf.length() - pos - oldSize;

        this.raf.seek(pos + oldSize);
        // Hopefully the data to shift is small enough.
        // If the data is too big then shift by blocks of 512 or 1024 bytes.
        byte[] dataToShift = new byte[(int) size];
        int readBytes = this.raf.read(dataToShift);
        this.raf.seek(pos + newSize);
        this.raf.write(dataToShift);
    }

    @Override
    public void close() throws IOException {
        this.header.write();
        this.raf.close();
        this.worldMap.unloadRegion(this);
    }

    /**
     * Represents the header of a region file.
     * <p>
     * The header is a 1024 bytes space containing:
     * <ul>
     *     <li>The UTF-8 string {@code "LambdaMapRegion "}</li>
     *     <li>The version as a 16-bit unsigned integer</li>
     *     <li>An ASCII space {@code ' '}</li>
     *     <li>The X-coordinate as a 32-bit signed integer</li>
     *     <li>The Z-coordinate as a 32-bit signed integer</li>
     *     <li>Starting at the 32th byte, ordered list (size 64) of chunk offset from the Header as 64-bit unsigned integers</li>
     * </ul>
     *
     * @version 1.0.0
     * @since 1.0.0
     */
    static class Header {
        private final FileChannel channel;
        private final ByteBuffer header;
        private final LongBuffer chunkData;
        private final int x;
        private final int z;

        private Header(FileChannel channel, int x, int z) {
            this.channel = channel;
            this.header = ByteBuffer.allocateDirect(HEADER_SIZE);
            this.header.position(32);
            this.chunkData = this.header.asLongBuffer();
            this.chunkData.limit(CHUNKS * CHUNKS);
            this.x = x;
            this.z = z;
        }

        public int getX() {
            return this.x;
        }

        public int getZ() {
            return this.z;
        }

        public long getEntriesCount() {
            return this.chunkData.limit();
        }

        public void writeDefault() throws IOException {
            this.header.position(0);
            for (int i = 0; i < 32; i++) {
                this.header.put((byte) 0);
            }
            this.chunkData.position(0);
            for (int i = 0; i < this.chunkData.limit(); i++) {
                this.chunkData.put(INVALID_CHUNK);
            }
            this.write();
        }

        public void write() throws IOException {
            // Header start
            this.header.position(0);
            this.header.put("LambdaMapRegion ".getBytes(StandardCharsets.UTF_8));
            this.header.putShort((short) VERSION);
            this.header.put((byte) ' ');
            this.header.putInt(this.x);
            this.header.putInt(this.z);

            this.header.position(0);
            this.channel.write(this.header, 0L);
        }

        public void read() throws IOException {
            this.header.position(0);
            this.channel.read(this.header, 0L);

            this.header.position(16);

            short version = this.header.getShort();
            this.header.get();
            this.header.getInt();
            this.header.getInt();
        }

        public void writeChunkEntry(int index, long offset) {
            this.chunkData.put(index, offset);
        }

        public void writeChunkEntry(int x, int z, long offset) {
            this.writeChunkEntry((z & 7) * CHUNKS + (x & 7), offset);
        }

        public long getChunkEntry(int index) {
            return this.chunkData.get(index);
        }

        public long getChunkEntry(int x, int z) {
            return this.getChunkEntry((z & 7) * CHUNKS + (x & 7));
        }

        public boolean hasChunk(int x, int z) {
            return this.getChunkEntry(x, z) != INVALID_CHUNK;
        }
    }
}
