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

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.gui.WorldMapRenderer;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.map.MapIcon;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents a marker type.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class MarkerType {
    private static final Object2ObjectMap<String, MarkerType> TYPES = new Object2ObjectOpenHashMap<>();

    public static final MarkerType PLAYER = registerVanilla(MapIcon.Type.PLAYER);
    public static final MarkerType TARGET_POINT = registerVanilla(MapIcon.Type.TARGET_POINT);

    private final String id;
    private final RenderLayer renderLayer;
    private final float uMin;
    private final float vMin;
    private final float uMax;
    private final float vMax;

    MarkerType(String id, RenderLayer renderLayer, float uMin, float vMin, float uMax, float vMax) {
        this.id = id;
        this.renderLayer = renderLayer;
        this.uMin = uMin;
        this.vMin = vMin;
        this.uMax = uMax;
        this.vMax = vMax;

        if (!TYPES.containsKey(this.id))
            TYPES.put(this.id, this);
    }

    public String getId() {
        return this.id;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float rotation, @Nullable Text text, int light) {
        matrices.push();
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotation));
        matrices.scale(4.f, 4.f, 3.f);
        matrices.translate(-0.125, 0.125, 0.0);
        VertexConsumer vertices = vertexConsumers.getBuffer(this.renderLayer);
        Matrix4f model = matrices.peek().getModel();
        WorldMapRenderer.vertex(vertices, model, -1.f, 1.f, this.uMin, this.vMin, light);
        WorldMapRenderer.vertex(vertices, model, 1.f, 1.f, this.uMax, this.vMin, light);
        WorldMapRenderer.vertex(vertices, model, 1.f, -1.f, this.uMax, this.vMax, light);
        WorldMapRenderer.vertex(vertices, model, -1.f, -1.f, this.uMin, this.vMax, light);
        matrices.pop();

        if (text != null) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            float textWidth = textRenderer.getWidth(text);
            float scale = MathHelper.clamp(32.f / textWidth, 0.f, 6.f / 9.f);
            matrices.push();
            matrices.translate(-textWidth * scale / 2.0f, 4.f, 0.02500000037252903D);
            matrices.scale(scale, scale, -1.f);
            matrices.translate(0.f, 0.f, 0.10000000149011612D);

            model = matrices.peek().getModel();
            textRenderer.draw(text, 0.f, 0.f, 0xffffffff, false, model, vertexConsumers, false, 0xaa000000, light);
            matrices.pop();
        }
    }

    @Override
    public String toString() {
        return "MarkerType{" +
                "key='" + this.id + '\'' +
                ", renderLayer=" + this.renderLayer +
                ", uMin=" + this.uMin +
                ", vMin=" + this.vMin +
                ", uMax=" + this.uMax +
                ", vMax=" + this.vMax +
                '}';
    }

    public static @Nullable MarkerType getMarkerType(String key) {
        return TYPES.getOrDefault(key, TARGET_POINT);
    }

    public static MarkerType getVanillaMarkerType(MapIcon.Type type) {
        return TYPES.getOrDefault(type.name().toLowerCase(Locale.ROOT), TARGET_POINT);
    }

    public static MarkerType register(String key, Identifier texture, float u, float v) {
        return register(key, texture, u, v, u + 0.0625f, v + 0.0625f);
    }

    public static MarkerType register(String key, Identifier texture, float uMin, float vMin, float uMax, float vMax) {
        return new MarkerType(key, RenderLayer.getText(texture), uMin, vMin, uMax, vMax);
    }

    private static MarkerType registerVanilla(MapIcon.Type type) {
        byte id = type.getId();
        float uMin = (id % 16) / 16.f;
        float vMin = (id / 16) / 16.f;
        return new MarkerType(type.name().toLowerCase(Locale.ROOT), LambdaMap.MAP_ICONS, uMin, vMin, uMin + 0.0625f, vMin + 0.0625f);
    }

    static {
        for (MapIcon.Type type : MapIcon.Type.values()) {
            if (type != MapIcon.Type.TARGET_POINT)
                registerVanilla(type);
        }
    }
}