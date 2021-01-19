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

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapBannerMarker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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
    private int x;
    private int y;
    private int z;
    private float rotation;
    private @Nullable Text name;

    public Marker(MarkerType type, int x, int y, int z, float rotation, @Nullable Text name) {
        this.type = type;
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

    public float getRotation() {
        return this.rotation;
    }

    public void setRotation(byte rotation) {
        this.rotation = rotation;
    }

    public @Nullable Text getName() {
        return this.name;
    }

    public void setName(@Nullable Text name) {
        this.name = name;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int startX, int startZ, float scaleCompensation, int light) {
        matrices.push();
        int x = this.getX() - startX;
        int z = this.getZ() - startZ;

        matrices.translate(x * scaleCompensation, z * scaleCompensation, 1.f);
        this.getType().render(matrices, vertexConsumers, this.getRotation(), this.getName(), light);
        matrices.pop();
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", this.type.getKey());
        tag.putInt("x", this.x);
        tag.putInt("y", this.y);
        tag.putInt("z", this.z);
        tag.putFloat("rotation", this.rotation);
        if (this.name != null) {
            tag.putString("name", Text.Serializer.toJson(this.name));
        }
        return tag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marker marker = (Marker) o;
        return x == marker.x && y == marker.y && z == marker.z && rotation == marker.rotation && Objects.equals(type, marker.type) && Objects.equals(name, marker.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, x, y, z, rotation, name);
    }

    public static @Nullable Marker fromBanner(BlockView world, BlockPos pos) {
        MapBannerMarker bannerMarker = MapBannerMarker.fromWorldBlock(world, pos);
        if (bannerMarker == null)
            return null;

        return new Marker(MarkerType.getVanillaMarkerType(bannerMarker.getIconType()), pos.getX(), pos.getY(), pos.getZ(), 180.f, bannerMarker.getName());
    }

    public static Marker fromTag(CompoundTag tag) {
        MarkerType type = MarkerType.getMarkerType(tag.getString("type"));
        Text name = null;
        if (tag.contains("name", NbtType.STRING))
            name = Text.Serializer.fromJson(tag.getString("name"));
        return new Marker(type, tag.getInt("x"), tag.getInt("y"), tag.getInt("z"), tag.getFloat("rotation"), name);
    }
}
