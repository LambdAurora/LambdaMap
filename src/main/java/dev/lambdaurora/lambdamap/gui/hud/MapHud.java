/*
 * Copyright (c) 2021-2022 LambdAurora <email@lambdaurora.dev>
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

package dev.lambdaurora.lambdamap.gui.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tessellator;
import dev.lambdaurora.lambdamap.LambdaMap;
import dev.lambdaurora.lambdamap.LambdaMapConfig;
import dev.lambdaurora.lambdamap.gui.WorldMapRenderer;
import dev.lambdaurora.lambdamap.map.ChunkGetterMode;
import dev.lambdaurora.lambdamap.map.WorldMap;
import dev.lambdaurora.lambdamap.map.marker.MarkerType;
import dev.lambdaurora.spruceui.util.ColorUtil;
import dev.lambdaurora.spruceui.util.ScissorManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.Axis;
import net.minecraft.util.math.MathHelper;

public class MapHud implements AutoCloseable {
	private static final Text NORTH = Text.translatable("lambdamap.compass.short.north");
	private static final Text EAST = Text.translatable("lambdamap.compass.short.east");
	private static final Text SOUTH = Text.translatable("lambdamap.compass.short.south");
	private static final Text WEST = Text.translatable("lambdamap.compass.short.west");

	private final LambdaMapConfig config;
	private final MinecraftClient client;
	private final NativeImageBackedTexture texture = new NativeImageBackedTexture(128 + 64, 128 + 64, true);
	private final RenderLayer mapRenderLayer;
	private boolean dirty = true;
	private int renderPosX;
	private int renderPosZ;

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

	private static final int TEXTURE_SIZE = 128 + 64;
	private static final int HUD_SIZE = 128;
	private static final float THRESHOLD_NORTH_LOCKED = ((TEXTURE_SIZE - HUD_SIZE) / 2f) - 1f;
	private static final float THRESHOLD_ROTATED = ((TEXTURE_SIZE / 2f) - MathHelper.sqrt((HUD_SIZE * HUD_SIZE) / 2f)) - 1f;

	/**
	 * Returns the threshold of distance between rendered and current player position after which to update the map.
	 * Greater if the map is north-locked, as there is a larger margin around the map.
	 */
	public float getMovementThreshold() {
		return this.config.isNorthLocked() ? THRESHOLD_NORTH_LOCKED : THRESHOLD_ROTATED;
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
		this.renderPosX = this.client.player.getBlockPos().getX();
		this.renderPosZ = this.client.player.getBlockPos().getZ();
	}

	public void render(MatrixStack matrices, int light, float delta) {
		if (!this.isVisible() || this.client.currentScreen != null && this.client.currentScreen.isPauseScreen())
			return;

		float scaleFactor = (float) this.client.getWindow().getScaleFactor();
		float newScaleFactor = this.config.getHudScale();
		float scaleCompensation = newScaleFactor / scaleFactor;

		int width = (int) (this.client.getWindow().getFramebufferWidth() / scaleFactor);
		matrices.push();
		matrices.translate(width - HUD_SIZE * scaleCompensation, 0, -20);
		matrices.scale(scaleCompensation, scaleCompensation, 1);

		var immediate = VertexConsumerProvider.immediate(Tessellator.getInstance().getBufferBuilder());

		HudDecorator decorator = this.config.getHudDecorator();
		int margin = decorator.getMargin();

		matrices.translate(-margin * 2, 0, -1);

		matrices.push();
		if (!this.client.options.debugEnabled) {
			RenderSystem.setShaderColor(1.f, 1.f, 1.f, 1.f);
		} else {
			RenderSystem.setShaderColor(0.25f, 0.25f, 0.25f, 0.5f);
		}
		decorator.render(matrices, immediate, HUD_SIZE + margin * 2, HUD_SIZE + margin * 2);
		matrices.pop();

		matrices.translate(margin, margin, 1);

		{
			int i = (int) ((double) this.client.getWindow().getFramebufferWidth() / newScaleFactor);
			int scaledWidth = (double) this.client.getWindow().getFramebufferWidth() / newScaleFactor > (double) i ? i + 1 : i;
			ScissorManager.push(scaledWidth - HUD_SIZE - margin, margin, HUD_SIZE, HUD_SIZE, newScaleFactor);
		}

		int textureWidth = this.texture.getImage().getWidth();
		int textureHeight = this.texture.getImage().getHeight();

		matrices.push();

		float uStart = 0.f;
		float uEnd = 1.f;
		float vStart = 0.f;
		float vEnd = 1.f;
		if (!this.config.isNorthLocked()) {
			matrices.translate(64, 64, 0);
			matrices.multiply(Axis.Z_POSITIVE.rotationDegrees(-this.client.player.getYaw(delta) + 180));
			matrices.translate(-64, -64, 0);
		}
		// Translate so map is centred
		matrices.translate(-32, -32, 0);

		// Translate by offset from position that map was last rendered from
		var lerped = this.client.player.getLerpedPos(delta);
		float offsetX = (float) (renderPosX - lerped.getX());
		float offsetZ = (float) (renderPosZ - lerped.getZ());
		matrices.translate(offsetX, offsetZ, 0);

		var model = matrices.peek().getModel();
		var vertices = immediate.getBuffer(this.mapRenderLayer);
		WorldMapRenderer.vertex(vertices, model, 0.f, textureHeight, uStart, vEnd, light);
		WorldMapRenderer.vertex(vertices, model, textureWidth, textureHeight, uEnd, vEnd, light);
		WorldMapRenderer.vertex(vertices, model, textureWidth, 0.f, uEnd, vStart, light);
		WorldMapRenderer.vertex(vertices, model, 0.f, 0.f, uStart, vStart, light);

		{
			int cornerX = renderPosX - (textureWidth / 2);
			int cornerZ = renderPosZ - (textureHeight / 2);
			LambdaMap.get().getMap().getMarkerManager().forEachInBox(cornerX, cornerZ, textureWidth, textureHeight, marker -> {
				matrices.push();

				float x = (float) (marker.getX() - cornerX);
				float z = (float) (marker.getZ() - cornerZ);

				matrices.translate(x, z, 1.f);
				if (!this.config.isNorthLocked())
					matrices.multiply(Axis.Z_POSITIVE.rotationDegrees(this.client.player.getYaw(delta) - 180));
				marker.getType().render(matrices, immediate, marker.getRotation(), marker.getName(), light);
				matrices.pop();
			});
		}
		matrices.pop();

		this.renderPlayerIcon(matrices, immediate, light, delta);
		immediate.draw();

		ScissorManager.pop();

		if (this.config.isDirectionIndicatorsVisible()) {
			if (this.config.isNorthLocked()) {
				this.renderStaticCompassIndicators(matrices, immediate, light);
			} else {
				this.renderDynamicCompassIndicators(matrices, immediate, light, delta);
			}
			immediate.draw();
		}

		if (!this.client.options.debugEnabled) {
			var pos = this.client.player.getBlockPos();
			var str = String.format("X: %d Y: %d Z: %d", pos.getX(), pos.getY(), pos.getZ());
			int strWidth = this.client.textRenderer.getWidth(str);
			this.client.textRenderer.draw(str, 64 - strWidth / 2.f, 130 + decorator.getCoordinatesOffset(), ColorUtil.WHITE, true,
					matrices.peek().getModel(), immediate, false, 0, light);
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

	private void renderStaticCompassIndicators(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
		int fontHeight = this.client.textRenderer.fontHeight;
		this.client.textRenderer.draw(NORTH, 64 - this.client.textRenderer.getWidth(NORTH) / 2.f, 0, 0xffff0000, true,
				matrices.peek().getModel(), vertexConsumers, false, 0, light);
		this.client.textRenderer.draw(SOUTH, 64 - this.client.textRenderer.getWidth(SOUTH) / 2.f, HUD_SIZE - fontHeight, ColorUtil.WHITE, true,
				matrices.peek().getModel(), vertexConsumers, false, 0, light);
		this.client.textRenderer.draw(EAST, HUD_SIZE - this.client.textRenderer.getWidth(EAST), 64 - fontHeight / 2.f, ColorUtil.WHITE, true,
				matrices.peek().getModel(), vertexConsumers, false, 0, light);
		this.client.textRenderer.draw(WEST, 0, 64 - fontHeight / 2.f, ColorUtil.WHITE, true,
				matrices.peek().getModel(), vertexConsumers, false, 0, light);
	}

	private void renderDynamicCompassIndicators(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float delta) {
		float yaw = this.client.player.getYaw(delta);

		this.renderCompassIndicator(matrices, vertexConsumers, WEST, light, yaw);
		this.renderCompassIndicator(matrices, vertexConsumers, NORTH, light, yaw - 90);
		this.renderCompassIndicator(matrices, vertexConsumers, EAST, light, yaw - 180);
		this.renderCompassIndicator(matrices, vertexConsumers, SOUTH, light, yaw + 90);
	}

	private void renderCompassIndicator(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Text text, int light, float yaw) {
		float x = (64 + 32) * MathHelper.cos(-yaw / 360 * MathHelper.TAU) + 64;
		float y = (64 + 32) * MathHelper.sin(-yaw / 360 * MathHelper.TAU) + 64;

		x = MathHelper.clamp(x, 0, HUD_SIZE - this.client.textRenderer.getWidth(text));
		y = MathHelper.clamp(y, 0, HUD_SIZE - this.client.textRenderer.fontHeight);

		this.client.textRenderer.draw(text, x, y, text == NORTH ? 0xffff0000 : ColorUtil.WHITE, true,
				matrices.peek().getModel(), vertexConsumers, false, 0, light);
	}

	@Override
	public void close() {
		this.texture.close();
	}
}
