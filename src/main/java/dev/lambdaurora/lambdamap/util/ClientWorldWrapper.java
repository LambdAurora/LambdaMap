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

package dev.lambdaurora.lambdamap.util;

import dev.lambdaurora.lambdamap.map.MapChunk;
import me.lambdaurora.spruceui.util.ColorUtil;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a client world wrapper as a {@link BlockRenderView} with a {@link MapChunk} associated to provide biome coloring.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class ClientWorldWrapper implements BlockRenderView {
    private final ClientWorld world;
    private final MapChunk chunk;

    public ClientWorldWrapper(ClientWorld world, MapChunk chunk) {
        this.world = world;
        this.chunk = chunk;
    }

    @Override
    public float getBrightness(Direction direction, boolean shaded) {
        return this.world.getBrightness(direction, shaded);
    }

    @Override
    public LightingProvider getLightingProvider() {
        return this.world.getLightingProvider();
    }

    @Override
    public int getColor(BlockPos pos, ColorResolver colorResolver) {
        if (this.chunk == null || this.chunk.isEmpty())
            return 0;
        Biome biome = this.chunk.getBiome(pos.getX(), pos.getZ());
        if (biome == null) {
            int color = this.chunk.getColor(pos.getX(), pos.getZ()) & 255;
            return MapColor.COLORS[color / 4].color; // Give up
        }
        int color = colorResolver.getColor(biome, pos.getX(), pos.getZ());

        BlockState state = chunk.getBlockState(pos.getX(), pos.getZ());
        if (state != null && (state.getBlock() == Blocks.GRASS || state.getBlock() == Blocks.TALL_GRASS
                || state.getBlock() == Blocks.VINE)) {
            return ColorUtil.argbMultiply(color, .8f, 0xff);
        }

        return color;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.world.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.world.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.world.getFluidState(pos);
    }

    @Override
    public int getBottomSectionLimit() {
        return this.world.getBottomSectionLimit();
    }

    @Override
    public int getSectionCount() {
        return this.world.getSectionCount();
    }
}
