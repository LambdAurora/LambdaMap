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

import dev.lambdaurora.lambdamap.extension.WorldChunkExtension;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements WorldChunkExtension {
	@Unique
	private boolean lambdamap$dirty;
	@Unique
	private boolean lambdamap$biomeDirty = true;

	@Override
	public boolean lambdamap$isDirty() {
		return this.lambdamap$dirty;
	}

	@Override
	public boolean lambdamap$isBiomeDirty() {
		return this.lambdamap$biomeDirty;
	}

	@Override
	public void lambdamap$markDirty() {
		this.lambdamap$dirty = true;
	}

	@Override
	public void lambdamap$markClean() {
		this.lambdamap$dirty = false;
		this.lambdamap$biomeDirty = false;
	}
}
