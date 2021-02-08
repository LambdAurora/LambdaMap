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

package dev.lambdaurora.lambdamap.map.marker;

import com.electronwill.nightconfig.core.Config;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents a marker.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class Marker {
    private MarkerType type;
    private MarkerSource source;
    private int x;
    private int y;
    private int z;
    private float rotation;
    private @Nullable Text name;

    public Marker(MarkerType type, MarkerSource source, int x, int y, int z, float rotation, @Nullable Text name) {
        this.type = type;
        this.source = source;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.name = name;
    }

    public MarkerType getType() {
        return this.type;
    }

    public void setType(MarkerType type) {
        this.type = type;
    }

    public MarkerSource getSource() {
        return this.source;
    }

    public void setSource(MarkerSource source) {
        this.source = source;
    }

    public int getX() {
        return this.x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return this.y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getZ() {
        return this.z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public BlockPos getPos() {
        return new BlockPos(this.getX(), this.getY(), this.getZ());
    }

    public int getChunkX() {
        return ChunkSectionPos.getSectionCoord(this.getX());
    }

    public int getChunkZ() {
        return ChunkSectionPos.getSectionCoord(this.getZ());
    }

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public @Nullable Text getName() {
        return this.name;
    }

    public void setName(@Nullable Text name) {
        this.name = name;
    }

    /**
     * Returns whether this marker is in the specified box.
     *
     * @param minX the minimum X-coordinate of the box
     * @param minZ the minimum Z-coordinate of the box
     * @param maxX the maximum X-coordinate of the box
     * @param maxZ the maximum Z-coordinate of the box
     * @return {@code true} if the marker is in the box, else {@code false}
     */
    public boolean isIn(int minX, int minZ, int maxX, int maxZ) {
        return this.getX() >= minX && this.getZ() >= minZ && this.getX() < maxX && this.getZ() < maxZ;
    }

    public boolean isAt(int x, int y, int z) {
        return this.getX() == x && this.getY() == y && this.getZ() == z;
    }

    public boolean isAt(Marker marker) {
        return this.isAt(marker.getX(), marker.getY(), marker.getZ());
    }

    public void merge(Marker marker) {
        this.setType(marker.getType());
        this.setSource(marker.getSource());
        this.setX(marker.getX());
        this.setY(marker.getY());
        this.setZ(marker.getZ());
        this.setRotation(marker.getRotation());
        if (marker.getName() != null)
            this.setName(marker.getName());
    }

    @Environment(EnvType.CLIENT)
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int startX, int startZ, float scale, int light) {
        matrices.push();
        int x = this.getX() - startX;
        int z = this.getZ() - startZ;

        matrices.translate(x / scale, z / scale, 1.f);
        this.getType().render(matrices, vertexConsumers, this.getRotation(), this.getName(), light);
        matrices.pop();
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", this.type.getId());
        tag.putString("source", this.source.getId());
        tag.putInt("x", this.x);
        tag.putInt("y", this.y);
        tag.putInt("z", this.z);
        tag.putFloat("rotation", this.rotation);
        if (this.name != null) {
            tag.putString("name", Text.Serializer.toJson(this.name));
        }
        return tag;
    }

    public Config writeTo(Config config) {
        config.set("type", this.type.getId());
        config.set("source", this.source.getId());
        config.set("x", this.x);
        config.set("y", this.y);
        config.set("z", this.z);
        config.set("rotation", this.rotation);
        if (this.name != null) {
            config.set("name", Text.Serializer.toJson(this.name));
        }
        return config;
    }

    @Override
    public String toString() {
        return "Marker{" +
                "type=" + this.getType() +
                ", source=" + this.getSource() +
                ", x=" + this.getX() +
                ", y=" + this.getY() +
                ", z=" + this.getZ() +
                ", rotation=" + this.getRotation() +
                ", name=" + this.getName() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marker marker = (Marker) o;
        return this.x == marker.x && this.y == marker.y && this.z == marker.z && this.rotation == marker.rotation
                && Objects.equals(this.type, marker.type) && Objects.equals(this.name, marker.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, x, y, z, rotation, name);
    }

    public static @Nullable Marker fromBanner(BlockView world, BlockPos pos) {
        MapBannerMarker bannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
        if (bannerMarker == null)
            return null;

        return new Marker(MarkerType.getVanillaMarkerType(bannerMarker.getIconType()), MarkerSource.BANNER,
                pos.getX(), pos.getY(), pos.getZ(), 180.f, bannerMarker.getName());
    }

    public static Marker fromNbt(CompoundTag tag) {
        MarkerType type = MarkerType.getMarkerType(tag.getString("type"));
        Text name = null;
        if (tag.contains("name", NbtType.STRING))
            name = Text.Serializer.fromJson(tag.getString("name"));
        return new Marker(type, MarkerSource.fromId(tag.getString("source")),
                tag.getInt("x"), tag.getInt("y"), tag.getInt("z"), tag.getFloat("rotation"), name);
    }

    public static Marker fromConfig(Config config) {
        MarkerType type = MarkerType.getMarkerType(config.getOrElse("type", MarkerType.TARGET_POINT.getId()));
        MarkerSource source = MarkerSource.fromId(config.getOrElse("source", MarkerSource.USER.getId()));
        Text name = null;
        if (config.contains("name"))
            name = Text.Serializer.fromJson(config.getOrElse("name", "{}"));
        return new Marker(type, source,
                config.getIntOrElse("x", 0),
                config.getIntOrElse("y", 0),
                config.getIntOrElse("z", 0),
                config.getOrElse("rotation", 0.0).floatValue(),
                name);
    }
}
