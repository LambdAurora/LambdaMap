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

package me.lambdaurora.lambdamap.map.storage;

import me.lambdaurora.lambdamap.map.MapChunk;
import me.lambdaurora.lambdamap.map.WorldMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

    public static MapRegionFile loadOrCreate(WorldMap worldMap, int x, int z) throws IOException {
        File file = new File(worldMap.getDirectory(), "region_" + x + "_" + z + ".lmr");
        boolean exists = file.exists();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        Header header = new Header(raf, x, z);
        if (!exists) {
            header.writeDefault();
        } else {
            header.read();
        }

        return new MapRegionFile(worldMap, raf, header);
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

    public MapChunk loadChunkOrCreate(int x, int z) {
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
            chunkPos = this.raf.length();
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
        stream.close();
    }

    public void shiftChunksIfNeeded(long pos, long oldSize, long newSize) throws IOException {
        long delta = newSize - oldSize;

        if (delta <= 0 || this.raf.length() == pos + oldSize) {
            return;
        }

        for (int i = 0; i < this.header.getEntriesCount(); i++) {
            long offset = this.header.chunkEntries[i];
            if (pos > offset + HEADER_SIZE) {
                this.header.writeChunkEntry(i, offset + delta);
            }
        }

        long size = this.raf.length() - pos - oldSize;
        LOGGER.info("LENGTH {}, POS {}, OLD SIZE {}, NEW SIZE {}, TO SHIFT {}", this.raf.length(), pos, oldSize, newSize, size);

        this.raf.seek(pos + oldSize);
        // Hopefully the data to shift is small enough.
        // If the data is too big then shift by blocks of 512 or 1024 bytes.
        byte[] dataToShift = new byte[(int) size];
        this.raf.read(dataToShift);
        this.raf.seek(pos + newSize);
        this.raf.write(dataToShift);
    }

    @Override
    public void close() throws IOException {
        this.raf.close();
        this.worldMap.unloadRegion(this);
    }

    static class Header {
        private final RandomAccessFile raf;
        private final long[] chunkEntries = new long[CHUNKS * CHUNKS];
        private final int x;
        private final int z;

        private Header(RandomAccessFile raf, int x, int z) {
            this.raf = raf;
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
            return this.chunkEntries.length;
        }

        public void writeDefault() throws IOException {
            Arrays.fill(this.chunkEntries, INVALID_CHUNK);
            this.write();
        }

        public void write() throws IOException {
            this.raf.seek(0);

            // Header start
            this.raf.write("LambdaMapRegion ".getBytes(StandardCharsets.UTF_8));
            this.raf.writeByte(VERSION);
            this.raf.writeByte(' ');
            this.raf.writeInt(this.x);
            this.raf.writeInt(this.z);
            this.raf.writeByte(' ');

            // Chunk entries
            for (long entry : this.chunkEntries) {
                this.raf.writeLong(entry);
            }
        }

        public void read() throws IOException {
            this.raf.seek(16);

            byte version = this.raf.readByte();
            this.raf.skipBytes(1);
            this.raf.readInt();
            this.raf.readInt();
            this.raf.skipBytes(1);

            for (int i = 0; i < this.chunkEntries.length; i++) {
                long offset = this.raf.readLong();
                this.chunkEntries[i] = offset;
            }
        }

        public void writeChunkEntry(int index, long offset) throws IOException {
            this.chunkEntries[index] = offset;
            this.raf.seek(27 + index * 8L);
            this.raf.writeLong(offset);
        }

        public void writeChunkEntry(int x, int z, long offset) throws IOException {
            this.writeChunkEntry((z & 7) * CHUNKS + (x & 7), offset);
        }

        public long getChunkEntry(int x, int z) {
            return this.chunkEntries[(z & 7) * CHUNKS + (x & 7)];
        }

        public boolean hasChunk(int x, int z) {
            return this.getChunkEntry(x, z) != INVALID_CHUNK;
        }
    }
}
