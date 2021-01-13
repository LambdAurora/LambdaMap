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

package me.lambdaurora.lambdamap.gui;

import me.lambdaurora.lambdamap.map.MapChunk;
import me.lambdaurora.lambdamap.map.WorldMap;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class WorldMapRenderer {
    private static final List<ChunkTexture> TEXTURES = new ArrayList<>();

    private int width;
    private int height;

    private WorldMap worldMap;

    private ChunkTextureManager textureManager;

    public void allocate(int width, int height) {
        this.width = width;
        this.height = height;

        int texturesZ = this.width / 128 + 1;
        int texturesX = this.height / 128 + 1;

        this.textureManager = new ChunkTextureManager(texturesZ, texturesX);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xff000000);
    }

    class ChunkTextureManager {
        private final ChunkTexture[][] textures;

        public ChunkTextureManager(int texturesZ, int texturesX) {
            this.textures = new ChunkTexture[texturesZ][texturesX];

            for (int z = 0; z < this.textures.length; z++) {
                ChunkTexture[] line = this.textures[z];
                for (int x = 0; x < line.length; x++) {
                    int index = z * texturesZ + texturesX;
                    ChunkTexture texture = TEXTURES.get(index);
                    if (texture == null) {
                        texture = new ChunkTexture();
                        TEXTURES.add(texture);
                    }
                    line[x] = texture;
                }
            }
        }

        public void shiftUp() {
            ChunkTexture[] firstLine = this.textures[0];

            System.arraycopy(this.textures, 1, this.textures, 0, this.textures.length - 1);

            this.textures[this.textures.length - 1] = firstLine;
        }

        public void shiftDown() {
            ChunkTexture[] lastLine = this.textures[this.textures.length - 1];

            System.arraycopy(this.textures, 0, this.textures, 1, this.textures.length - 1);

            this.textures[0] = lastLine;
        }

        public void shiftLeft() {
            for (ChunkTexture[] line : this.textures) {
                ChunkTexture first = line[0];
                System.arraycopy(line, 1, line, 0, line.length - 1);
                line[line.length - 1] = first;
            }
        }

        public void shiftRight() {
            for (ChunkTexture[] line : this.textures) {
                ChunkTexture last = line[line.length - 1];
                System.arraycopy(line, 0, line, 1, line.length - 1);
                line[0] = last;
            }
        }
    }

    class ChunkTexture {
        private final NativeImageBackedTexture texture = new NativeImageBackedTexture(128, 128, true);
        private final RenderLayer mapRenderLayer;

        ChunkTexture() {
            Identifier id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("world_map", this.texture);
            this.mapRenderLayer = RenderLayer.getText(id);
        }

        public void update(MapChunk chunk) {
            for (int z = 0; z < 128; z++) {
                for (int x = 0; x < 128; x++) {
                    if (chunk == null) {
                        texture.getImage().setPixelColor(x, z, 0x00000000);
                    } else {
                        int color = chunk.getColor(x, z) & 255;
                        if (color / 4 == 0) {
                            this.texture.getImage().setPixelColor(x, z, 0);
                        } else {
                            this.texture.getImage().setPixelColor(x, z, MapColor.COLORS[color / 4].getRenderColor(color & 3));
                        }
                    }
                }
            }
        }

        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int x, int y, int width, int height) {
            matrices.push();
            VertexConsumer consumer = vertexConsumers.getBuffer(this.mapRenderLayer);
            Matrix4f model = matrices.peek().getModel();
            matrices.pop();
        }

        private void vertex(VertexConsumer vertexConsumer, Matrix4f model, float x, float y,
                            float u, float v, int light) {
            vertexConsumer.vertex(model, x, y, 0.f).color(255, 255, 255, 255)
                    .texture(u, v).light(light).next();
        }
    }
}
