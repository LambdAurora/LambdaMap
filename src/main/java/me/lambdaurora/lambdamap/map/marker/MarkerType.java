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

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.lambdaurora.lambdamap.LambdaMap;
import me.lambdaurora.lambdamap.gui.WorldMapRenderer;
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

    private final String key;
    private final RenderLayer renderLayer;
    private final float u;
    private final float v;

    MarkerType(String key, RenderLayer renderLayer, float u, float v) {
        this.key = key;
        this.renderLayer = renderLayer;
        this.u = u;
        this.v = v;

        if (!TYPES.containsKey(this.key))
            TYPES.put(this.key, this);
    }

    public String getKey() {
        return this.key;
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float rotation, @Nullable Text text, int light) {
        matrices.push();
        matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(rotation));
        matrices.scale(4.f, 4.f, 3.f);
        matrices.translate(-0.125, 0.125, 0.0);
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(this.renderLayer);
        Matrix4f model = matrices.peek().getModel();
        float right = this.u + 0.0625f;
        float bottom = this.v + 0.0625f;
        WorldMapRenderer.vertex(vertexConsumer, model, -1.f, 1.f, this.u, this.v, light);
        WorldMapRenderer.vertex(vertexConsumer, model, 1.f, 1.f, right, this.v, light);
        WorldMapRenderer.vertex(vertexConsumer, model, 1.f, -1.f, right, bottom, light);
        WorldMapRenderer.vertex(vertexConsumer, model, -1.f, -1.f, this.u, bottom, light);
        matrices.pop();

        if (text != null) {
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            float textWidth = textRenderer.getWidth(text);
            float scale = MathHelper.clamp(32.f / textWidth, 0.f, 6.f / 9.f);
            matrices.push();
            matrices.translate(-textWidth * scale / 2.0f, 4.f, 0.02500000037252903D);
            matrices.scale(scale, scale, -1.f);
            matrices.translate(0.f, 0.f, 0.10000000149011612D);
            textRenderer.draw(text, 0.f, 0.f, 0xffffffff, false, matrices.peek().getModel(), vertexConsumers, false, 0xaa000000, light);
            matrices.pop();
        }
    }

    public static @Nullable MarkerType getMarkerType(String key) {
        return TYPES.getOrDefault(key, TARGET_POINT);
    }

    public static MarkerType getVanillaMarkerType(MapIcon.Type type) {
        return TYPES.getOrDefault(type.name().toLowerCase(Locale.ROOT), TARGET_POINT);
    }

    public static MarkerType register(String key, Identifier texture, float u, float v) {
        return new MarkerType(key, RenderLayer.getText(texture), u, v);
    }

    private static MarkerType registerVanilla(MapIcon.Type type) {
        byte id = type.getId();
        return new MarkerType(type.name().toLowerCase(Locale.ROOT), LambdaMap.MAP_ICONS, (id % 16) / 16.f, (id / 16) / 16.f);
    }

    static {
        for (MapIcon.Type type : MapIcon.Type.values()) {
            if (type != MapIcon.Type.TARGET_POINT)
                registerVanilla(type);
        }
    }
}
