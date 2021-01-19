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

package me.lambdaurora.lambdamap.map.marker;

import me.lambdaurora.lambdamap.map.WorldMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Manages the markers of a world map.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarkerManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private final List<Marker> markers = new ArrayList<>();
    private final File file;

    public MarkerManager(WorldMap map, File file) {
        this.file = file;
    }

    public void addMarker(Marker marker) {
        this.markers.removeIf(other -> marker.getX() == other.getX() && marker.getY() == other.getY() && marker.getZ() == other.getZ());
        this.markers.add(marker);
    }

    public Stream<Marker> streamMarkersInBox(int startX, int startZ, int width, int height) {
        int endX = startX + width;
        int endZ = startZ + height;
        return this.markers.stream().filter(marker -> marker.getX() >= startX && marker.getZ() >= startZ && marker.getX() < endX && marker.getZ() < endZ);
    }

    public void load() {
        if (this.file.exists()) {
            try {
                this.fromTag(NbtIo.readCompressed(this.file));
            } catch (IOException e) {
                LOGGER.error("Failed to read markers from " + this.file + ".", e);
            }
        }
    }

    public void save() {
        try {
            NbtIo.writeCompressed(this.toTag(), this.file);
        } catch (IOException e) {
            LOGGER.error("Failed to save markers to " + this.file + ".", e);
        }
    }

    public void fromTag(CompoundTag tag) {
        ListTag list = tag.getList("markers", NbtType.COMPOUND);
        list.forEach(child -> this.markers.add(Marker.fromTag((CompoundTag) child)));
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        this.markers.stream().map(Marker::toTag).forEach(list::add);
        tag.put("markers", list);
        return tag;
    }
}
