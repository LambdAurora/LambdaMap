/*
 * Copyright (c) 2021 LambdAurora <aurora42lambda@gmail.com>
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
import dev.lambdaurora.lambdamap.LambdaMapConfig;
import dev.lambdaurora.lambdamap.map.ChunkGetterMode;
import dev.lambdaurora.lambdamap.map.WorldMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.spruceui.util.ScissorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3f;

public class MapHud implements AutoCloseable {
    private final LambdaMapConfig config;
    private final MinecraftClient client;
    private final NativeImageBackedTexture texture = new NativeImageBackedTexture(128 + 64, 128 + 64, true);
    private final RenderLayer mapRenderLayer;
    private boolean dirty = true;

    public MapHud(LambdaMapConfig config, MinecraftClient client) {
        this.config = config;
        this.client = client;
        var id = LambdaMap.id("hud");
        client.getTextureManager().registerTexture(id, this.texture);
        this.mapRenderLayer = RenderLayer.getText(id);
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isVisible() {
        return this.config.isHudVisible();
    }

    public void setVisible(boolean visible) {
        this.config.setHudVisible(visible);
    }

    public void updateTexture(WorldMap map) {
        if (!this.isVisible() || this.client.currentScreen != null && this.client.currentScreen.isPauseScreen())
            return;
        if (!this.dirty) return;
        else this.dirty = false;

        int width = this.texture.getImage().getWidth();
        int height = this.texture.getImage().getHeight();
        var corner = this.client.player.getBlockPos().add(-(width / 2), 0, -(height / 2));
        for (int z = 0; z < width; ++z) {
            int absoluteZ = corner.getZ() + z;
            for (int x = 0; x < height; ++x) {
                int absoluteX = corner.getX() + x;
                this.texture.getImage().setPixelColor(x, z, map.getRenderColor(absoluteX, absoluteZ, ChunkGetterMode.LOAD));
            }
        }

        this.texture.upload();
    }

    public void render(MatrixStack matrices, int light, float delta) {
        if (!this.isVisible() || this.client.currentScreen != null && this.client.currentScreen.isPauseScreen())
            return;

        float scaleFactor = (float) this.client.getWindow().getScaleFactor();
        float newScaleFactor = scaleFactor;
        float scaleCompensation = 1.f;
        if (!(scaleFactor <= 2)) {
            newScaleFactor = (float) (this.client.getWindow().getScaleFactor() - 1);
            scaleCompensation = newScaleFactor / scaleFactor;
        }
        {
            int i = (int) ((double) this.client.getWindow().getFramebufferWidth() / newScaleFactor);
            int scaledWidth = (double) this.client.getWindow().getFramebufferWidth() / newScaleFactor > (double) i ? i + 1 : i;
            ScissorManager.push(scaledWidth - 128, 0, 128, 128, newScaleFactor);
        }
        int width = (int) (this.client.getWindow().getFramebufferWidth() / scaleFactor);
        matrices.push();
        matrices.translate(width - 128 * scaleCompensation, 0, -20);
        matrices.scale(scaleCompensation, scaleCompensation, 1);

        var immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBuffer());

        int textureWidth = this.texture.getImage().getWidth();
        int textureHeight = this.texture.getImage().getHeight();

        matrices.push();

        float uStart = 0.f;
        float uEnd = 1.f;
        float vStart = 0.f;
        float vEnd = 1.f;
        if (!this.config.isNorthLocked()) {
            matrices.translate(64, 64, 0);
            matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-this.client.player.getYaw(delta) + 180));
            matrices.translate(-64 - 32, -64 - 32, 0);
        } else {
            textureWidth = 128;
            textureHeight = 128;
        }
        var model = matrices.peek().getModel();
        var vertices = immediate.getBuffer(this.mapRenderLayer);
        WorldMapRenderer.vertex(vertices, model, 0.f, textureHeight, uStart, vEnd, light);
        WorldMapRenderer.vertex(vertices, model, textureWidth, textureHeight, uEnd, vEnd, light);
        WorldMapRenderer.vertex(vertices, model, textureWidth, 0.f, uEnd, vStart, light);
        WorldMapRenderer.vertex(vertices, model, 0.f, 0.f, uStart, vStart, light);

        {
            double cornerX = this.client.player.getX() - (textureWidth / 2.f);
            double cornerZ = this.client.player.getZ() - (textureWidth / 2.f);
            LambdaMap.get().getMap().getMarkerManager().forEachInBox((int) cornerX, (int) cornerZ, textureWidth, textureHeight, marker -> {
                matrices.push();

                float x = (float) (marker.getX() - cornerX);
                float z = (float) (marker.getZ() - cornerZ);

                matrices.translate(x, z, 1.f);
                if (!this.config.isNorthLocked())
                    matrices.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(this.client.player.getYaw(delta) - 180));
                marker.getType().render(matrices, immediate, marker.getRotation(), marker.getName(), light);
                matrices.pop();
            });
        }
        matrices.pop();

        this.renderPlayerIcon(matrices, immediate, light, delta);
        immediate.draw();
        ScissorManager.pop();

        if (!this.client.options.debugEnabled) {
            var pos = this.client.player.getBlockPos();
            var str = String.format("X: %d Y: %d Z: %d", pos.getX(), pos.getY(), pos.getZ());
            int strWidth = this.client.textRenderer.getWidth(str);
            this.client.textRenderer.draw(str, 64 - strWidth / 2.f, 130, 0xffffffff, true, matrices.peek().getModel(), immediate,
                    false, 0, light);
            immediate.draw();
        } else {
            matrices.translate(0.f, 0.f, 1.2f);
            DrawableHelper.fill(matrices, 0, 0, 128, 128, 0x88000000);
        }
        matrices.pop();
        ScissorManager.popScaleFactor();
    }

    private void renderPlayerIcon(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float delta) {
        matrices.push();
        matrices.translate(64.f, 64.f, 1.1f);
        MarkerType.PLAYER.render(matrices, vertexConsumers, this.config.isNorthLocked() ? this.client.player.getYaw(delta) : 180, null, light);
        matrices.pop();
    }

    public void close() {
        this.texture.close();
    }
}
