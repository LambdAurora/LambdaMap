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

package dev.lambdaurora.lambdamap;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * Represents the block searcher.
 *
 * @author comp500
 */
public class BlockSearcher {
    private final World world;
    public final BlockPos.Mutable pos = new BlockPos.Mutable();
    private final BlockPos.Mutable depthTestPos = new BlockPos.Mutable();
    private BlockState state;
    private int height;
    private int waterDepth;

    public BlockSearcher(World world) {
        this.world = world;
    }

    public BlockState getState() {
        return this.state;
    }

    public int getHeight() {
        return this.height;
    }

    public int getWaterDepth() {
        return this.waterDepth;
    }

    public void searchForBlock(Chunk chunk, Heightmap surfaceHeightmap, int x, int z, int chunkStartX, int chunkStartZ) {
        this.height = surfaceHeightmap.get(x & 15, z & 15);
        this.pos.set(chunkStartX + x, this.height, chunkStartZ + z);
        int minimumY = this.world.getBottomSectionCoord();
        if (this.height <= minimumY + 1) {
            this.state = Blocks.AIR.getDefaultState();
        } else {
            do {
                this.pos.setY(--this.height);
                this.state = chunk.getBlockState(this.pos);
            } while (this.state.getMapColor(this.world, this.pos) == MapColor.CLEAR && this.height > minimumY);
        }
    }

    public void calcWaterDepth(Chunk chunk) {
        int heightTemp = this.height - 1;
        this.waterDepth = 0;
        this.depthTestPos.set(this.pos);

        BlockState depthTestBlock;
        do {
            this.depthTestPos.setY(heightTemp--);
            depthTestBlock = chunk.getBlockState(depthTestPos);
            ++this.waterDepth;
        } while (heightTemp > 0 && !depthTestBlock.getFluidState().isEmpty());

        this.state = this.getFluidStateIfVisible(this.world, this.state, this.depthTestPos);
    }

    public void searchForBlockCeil(Chunk chunk, int x, int z, int chunkStartX, int chunkStartZ) {
        this.height = 85;
        boolean brokeThroughCeil = false;
        this.pos.set(chunkStartX + x, this.height, chunkStartZ + z);
        var firstBlockState = chunk.getBlockState(this.pos);
        this.state = firstBlockState;
        if (this.state.isAir()) {
            brokeThroughCeil = true;
        }
        while ((!brokeThroughCeil || this.state.getMapColor(this.world, this.pos) == MapColor.CLEAR)
                && this.height > this.world.getBottomSectionCoord()) {
            this.pos.setY(--this.height);
            this.state = chunk.getBlockState(this.pos);
            if (this.state.isAir()) {
                brokeThroughCeil = true;
            }
        }
        if (!brokeThroughCeil) {
            this.state = firstBlockState;
            this.height = 85;
            this.pos.setY(this.height);
        }
    }

    private BlockState getFluidStateIfVisible(World world, BlockState state, BlockPos pos) {
        var fluidState = state.getFluidState();
        return !fluidState.isEmpty() && !state.isSideSolidFullSquare(world, pos, Direction.UP) ? fluidState.getBlockState() : state;
    }
}
