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

package dev.lambdaurora.lambdamap.gui;

import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.map.MapChunk;
import dev.lambdaurora.lambdamap.map.WorldMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the world map renderer.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class WorldMapRenderer {
    /**
     * Stores all the registered world map chunk textures.
     */
    private static final List<ChunkTexture> TEXTURES = new ArrayList<>();

    private int width;
    private int height;

    private WorldMap worldMap;

    private ChunkTextureManager textureManager;

    private int cornerViewX;
    private int cornerViewZ;

    public WorldMapRenderer(LambdaMap mod) {

    }

    public void setWorldMap(WorldMap worldMap) {
        this.worldMap = worldMap;

        this.updateView(worldMap.getViewX(), worldMap.getViewZ());
    }

    public void allocate(int width, int height) {
        this.width = width;
        this.height = height;

        int texturesX = this.width / 128 + 2;
        int texturesZ = this.height / 128 + 2;

        this.textureManager = new ChunkTextureManager(texturesZ, texturesX);
    }

    /**
     * Returns the X coordinate of the north-west corner.
     *
     * @return the corner block X coordinate
     */
    public int cornerX() {
        return this.cornerViewX;
    }

    /**
     * Returns the Z coordinate of the north-west corner.
     *
     * @return the corner block Z coordinate
     */
    public int cornerZ() {
        return this.cornerViewZ;
    }

    public int getCornerMapChunkX() {
        return MapChunk.blockToChunk(this.cornerViewX);
    }

    public int getCornerMapChunkZ() {
        return MapChunk.blockToChunk(this.cornerViewZ);
    }

    /**
     * Returns the width of the area .
     *
     * @return the width
     */
    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public void updateView(int x, int z) {
        this.worldMap.updateViewPos(x, z);

        x -= this.width / 2;
        z -= this.height / 2;

        if (this.cornerViewX != x && this.textureManager != null) {
            int oldChunkX = this.getCornerMapChunkX();
            int newChunkX = MapChunk.blockToChunk(x);

            if (oldChunkX != newChunkX) {
                if (newChunkX < oldChunkX) this.textureManager.shiftLeft();
                else this.textureManager.shiftRight();
            }
        }

        if (this.cornerViewZ != z && this.textureManager != null) {
            int oldChunkZ = this.getCornerMapChunkZ();
            int newChunkZ = MapChunk.blockToChunk(z);

            if (oldChunkZ != newChunkZ) {
                if (newChunkZ < oldChunkZ) this.textureManager.shiftUp();
                else this.textureManager.shiftDown();
            }
        }

        this.cornerViewX = x;
        this.cornerViewZ = z;

        if (this.textureManager != null)
            this.textureManager.updateTextures();
    }

    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, 0xff000000);

        int light = LightmapTextureManager.pack(15, 15);
        this.textureManager.render(matrices, vertexConsumers, light);

        this.worldMap.getMarkerManager().forEachInBox(this.cornerViewX - 5, this.cornerViewZ - 5, this.width + 10, this.height + 10,
                marker -> marker.render(matrices, vertexConsumers, this.cornerViewX, this.cornerViewZ, 1.f, light));

        this.renderPlayerIcon(matrices, vertexConsumers, light);
    }

    private void renderPlayerIcon(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        MinecraftClient client = MinecraftClient.getInstance();

        BlockPos pos = client.player.getBlockPos();

        if (this.cornerViewX > pos.getX() || this.cornerViewZ > pos.getZ() || this.cornerViewX + this.width < pos.getX() || this.cornerViewZ + this.height < pos.getZ())
            return;

        matrices.push();
        matrices.translate(pos.getX() - this.cornerViewX, pos.getZ() - this.cornerViewZ, 1.1f);
        MarkerType.PLAYER.render(matrices, vertexConsumers, client.player.yaw, null, light);
        matrices.pop();
    }

    public static void vertex(VertexConsumer vertices, Matrix4f model, float x, float y,
                              float u, float v, int light) {
        vertices.vertex(model, x, y, 0.f).color(255, 255, 255, 255)
                .texture(u, v).light(light).next();
    }

    /**
     * Represents the chunk texture manager. Manages all the world map textures and re-position them if needed.
     *
     * @version 1.0.0
     * @since 1.0.0
     */
    class ChunkTextureManager {
        private final ChunkTexture[][] textures;

        public ChunkTextureManager(int texturesZ, int texturesX) {
            this.textures = new ChunkTexture[texturesZ][texturesX];

            for (int z = 0; z < this.textures.length; z++) {
                ChunkTexture[] line = this.textures[z];
                for (int x = 0; x < line.length; x++) {
                    int index = z * line.length + x;
                    ChunkTexture texture;
                    if (TEXTURES.size() <= index) {
                        texture = new ChunkTexture();
                    } else {
                        texture = TEXTURES.get(index);
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

        public void updateTextures() {
            int chunkX = WorldMapRenderer.this.getCornerMapChunkX();
            int chunkZ = WorldMapRenderer.this.getCornerMapChunkZ();

            for (int z = 0; z < this.textures.length; z++) {
                ChunkTexture[] line = this.textures[z];
                for (int x = 0; x < line.length; x++) {
                    MapChunk chunk = WorldMapRenderer.this.worldMap.getChunkOrLoad(chunkX + x, chunkZ + z);
                    line[x].update(chunk);
                }
            }
        }

        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
            int originX = -((WorldMapRenderer.this.cornerX() & 127));
            int originZ = -((WorldMapRenderer.this.cornerZ() & 127));
            int offsetZ = -originZ;

            for (int z = 0; z < this.textures.length; z++) {
                ChunkTexture[] line = this.textures[z];

                if (originZ + z * 128 > WorldMapRenderer.this.height)
                    break;

                int offsetX = -originX;
                int height = 128;

                if (originZ + z * 128 + height > WorldMapRenderer.this.height) {
                    height = WorldMapRenderer.this.height - (originZ + z * 128);
                }

                for (int x = 0; x < line.length; x++) {
                    if (originX + x * 128 > WorldMapRenderer.this.width)
                        break;

                    int width = 128;

                    if (originX + x * 128 + width > WorldMapRenderer.this.width) {
                        width = WorldMapRenderer.this.width - (originX + x * 128);
                    }

                    line[x].render(matrices, vertexConsumers, originX + x * 128, originZ + z * 128, offsetX, offsetZ,
                            width, height, light);
                    offsetX = 0;
                }

                offsetZ = 0;
            }
        }
    }

    /**
     * Represents a chunk texture.
     *
     * @version 1.0.0
     * @since 1.0.0
     */
    class ChunkTexture {
        private final NativeImageBackedTexture texture = new NativeImageBackedTexture(128, 128, true);
        private final RenderLayer mapRenderLayer;

        ChunkTexture() {
            Identifier id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("world_map", this.texture);
            this.mapRenderLayer = RenderLayer.getText(id);

            TEXTURES.add(this);
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

            this.texture.upload();
        }

        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int originX, int originY, int offsetX, int offsetY, int width, int height, int light) {
            Matrix4f model = matrices.peek().getModel();
            VertexConsumer vertices = vertexConsumers.getBuffer(this.mapRenderLayer);

            float uOffset = offsetX / 128.f;
            float vOffset = offsetY / 128.f;
            float uWidth = width / 128.f;
            float vHeight = height / 128.f;

            vertex(vertices, model, originX + offsetX, originY + height,
                    uOffset, vHeight, light);
            vertex(vertices, model, originX + width, originY + height,
                    uWidth, vHeight, light);
            vertex(vertices, model, originX + width, originY + offsetY,
                    uWidth, vOffset, light);
            vertex(vertices, model, originX + offsetX, originY + offsetY,
                    uOffset, vOffset, light);
        }
    }
}
