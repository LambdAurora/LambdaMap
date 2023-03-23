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

package dev.lambdaurora.lambdamap.mixin;

import dev.lambdaurora.lambdamap.LambdaMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
	@Final
	@Shadow
	private MinecraftClient client;

	@Shadow
	private ClientWorld world;

	@Inject(method = "onChunkData", at = @At("RETURN"))
	private void onChunkData(ChunkDataS2CPacket packet, CallbackInfo ci) {
		LambdaMap.get().onChunkUpdate(packet.chunkX(), packet.chunkZ());
	}

	@Inject(method = "onChunkDeltaUpdate", at = @At("RETURN"))
	private void onChunkDeltaUpdate(ChunkDeltaUpdateS2CPacket packet, CallbackInfo ci) {
		var accessor = (ChunkDeltaUpdateS2CPacketAccessor) packet;
		LambdaMap.get().onChunkUpdate(accessor.getSectionPos().getX(), accessor.getSectionPos().getZ());
	}

	@Inject(method = "onBlockUpdate", at = @At("RETURN"))
	private void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
		LambdaMap.get().onBlockUpdate(packet.getPos().getX(), packet.getPos().getZ());
	}

	@Inject(method = "onGameJoin", at = @At("TAIL"))
	private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
		LambdaMap.get().unloadMap();

		if (world != null) {
			LambdaMap.get().loadMap(client, world);
		}
	}
}
