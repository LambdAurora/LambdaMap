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

import dev.lambdaurora.lambdamap.extension.WorldChunkExtension;
import dev.lambdaurora.lambdamap.gui.MapHud;
import dev.lambdaurora.lambdamap.gui.WorldMapRenderer;
import dev.lambdaurora.lambdamap.gui.WorldMapScreen;
import dev.lambdaurora.lambdamap.map.WorldMap;
import dev.lambdaurora.lambdamap.mixin.BiomeAccessAccessor;
import dev.lambdaurora.lambdamap.mixin.PersistentStateManagerAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import org.lwjgl.glfw.GLFW;

import java.io.File;

/**
 * Represents the LambdaMap mod.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class LambdaMap implements ClientModInitializer {
    public static final String NAMESPACE = "lambdamap";
    public static final Identifier MAP_ICONS_TEXTURE = new Identifier("textures/map/map_icons.png");
    public static final RenderLayer MAP_ICONS = RenderLayer.getText(MAP_ICONS_TEXTURE);
    private static LambdaMap INSTANCE;
    private final KeyBinding hudKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("lambdamap.keybind.hud", GLFW.GLFW_KEY_O, "key.categories.misc"));
    private final KeyBinding mapKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("lambdamap.keybind.map", GLFW.GLFW_KEY_B, "key.categories.misc"));
    private final LambdaMapConfig config = new LambdaMapConfig(this);
    private final WorldMapRenderer renderer = new WorldMapRenderer(this);
    private WorldMap map = null;
    public MapHud hud = null;

    private int updatedChunks = 0;

    @Override
    public void onInitializeClient() {
        INSTANCE = this;

        this.config.load();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            this.hud = new MapHud(this.config, client);
        });

        HudRenderCallback.EVENT.register((matrices, delta) -> {
            this.hud.render(matrices, LightmapTextureManager.pack(15, 15), delta);
        });

        ClientTickEvents.START_WORLD_TICK.register(world -> {
            var client = MinecraftClient.getInstance();
            if (this.map.updatePlayerViewPos(client.player.getBlockX(), client.player.getBlockZ(), this.hud.getMovementThreshold())) {
                this.hud.markDirty();
            }
            this.map.tick();
            this.updateChunks(world, client.player);

            if (this.hudKeybind.wasPressed()) {
                this.hud.setVisible(!this.hud.isVisible());
            }

            if (this.mapKeybind.wasPressed()) {
                client.setScreen(new WorldMapScreen());
            }

            this.hud.updateTexture(this.getMap());
        });
    }

    /**
     * Returns the configuration of the mod.
     *
     * @return the configuration
     */
    public LambdaMapConfig getConfig() {
        return this.config;
    }

    public WorldMap getMap() {
        return this.map;
    }

    public WorldMapRenderer getRenderer() {
        return this.renderer;
    }

    public void loadMap(MinecraftClient client, ClientWorld world) {
        File directory;
        if (client.getServer() != null) {
            directory = getWorldMapDirectorySP(client, world.getRegistryKey());
        } else {
            var hashedSeed = ((BiomeAccessAccessor) world.getBiomeAccess()).getSeed();
            directory = getWorldMapDirectoryMP(client, world.getRegistryKey(), hashedSeed);
        }
        this.map = new WorldMap(world, directory);
        this.renderer.setWorldMap(this.map);
    }

    public void unloadMap() {
        if (this.map != null) {
            this.map.unload();
            this.map = null;
        }
    }

    public void onBlockUpdate(int x, int z) {
        int chunkX = ChunkSectionPos.getSectionCoord(x);
        int chunkZ = ChunkSectionPos.getSectionCoord(z);
        int localX = ChunkSectionPos.getLocalCoord(x);
        int localZ = ChunkSectionPos.getLocalCoord(z);

        if (localX >= 2 && localX < 14 && localZ >= 2 && localZ < 14) {
            var chunk = this.map.getWorld().getChunk(chunkX, chunkZ);
            if (chunk != null) {
                ((WorldChunkExtension) chunk).lambdamap$markDirty();
            }
        } else this.onChunkUpdate(chunkX, chunkZ);
    }

    public void onChunkUpdate(int chunkX, int chunkZ) {
        for (int x = chunkX - 1; x < chunkX + 2; ++x) {
            for (int z = chunkZ - 1; z < chunkZ + 2; ++z) {
                var chunk = this.map.getWorld().getChunk(x, z);
                if (chunk != null) {
                    ((WorldChunkExtension) chunk).lambdamap$markDirty();
                }
            }
        }
    }

    public void updateChunks(World world, PlayerEntity entity) {
        var pos = entity.getChunkPos();
        var client = MinecraftClient.getInstance();
        int viewDistance = Math.max(2, client.options.viewDistance - 2);
        this.updatedChunks = 0;
        for (int x = pos.x - viewDistance; x <= pos.x + viewDistance; x++) {
            for (int z = pos.z - viewDistance; z <= pos.z + viewDistance; z++) {
                this.updateChunk(world, x, z);
            }
        }
    }

    public void updateChunk(World world, int chunkX, int chunkZ) {
        int chunkStartX = ChunkSectionPos.getBlockCoord(chunkX);
        int chunkStartZ = ChunkSectionPos.getBlockCoord(chunkZ);
        int mapChunkStartX = chunkStartX & 127;
        int mapChunkStartZ = chunkStartZ & 127;

        // Big thanks to comp500 for this piece of code
        // https://github.com/comp500/tinymap/blob/master/src/main/java/link/infra/tinymap/TileGenerator.java#L103
        var searcher = new BlockSearcher(world);
        boolean hasCeiling = world.getDimension().hasCeiling();

        var chunkBefore = (WorldChunk) world.getChunk(chunkX, chunkZ - 1, ChunkStatus.SURFACE, false);
        var chunkPosBefore = new ChunkPos(chunkX, chunkZ - 1);
        Heightmap chunkBeforeHeightmap;
        if (chunkBefore != null) chunkBeforeHeightmap = chunkBefore.getHeightmap(Heightmap.Type.WORLD_SURFACE);
        else return;

        int[] lastHeights = new int[16];

        var chunk = world.getChunk(chunkX, chunkZ, ChunkStatus.SURFACE, false);
        if (chunk == null)
            return;

        if (chunk instanceof WorldChunkExtension extendedChunk) {
            if (!extendedChunk.lambdamap$isDirty())
                return;
        }

        this.updatedChunks++;

        var chunkHeightmap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);

        var mapChunk = this.map.getChunkOrCreate(chunkX >> 3, chunkZ >> 3);

        for (int xOffset = 0; xOffset < 16; xOffset++) {
            if (!chunkBefore.isEmpty()) {
                // Get first line, to calculate proper shade
                if (hasCeiling) {
                    searcher.searchForBlockCeil(chunkBefore, xOffset, 15, chunkPosBefore.getStartX(), chunkPosBefore.getStartZ());
                } else {
                    searcher.searchForBlock(chunkBefore, chunkBeforeHeightmap, xOffset, 15, chunkPosBefore.getStartX(), chunkPosBefore.getStartZ());
                }
                lastHeights[xOffset] = searcher.getHeight();
            }

            for (int zOffset = 0; zOffset < 16; zOffset++) {
                if (hasCeiling) {
                    searcher.searchForBlockCeil(chunk, xOffset, zOffset, chunkStartX, chunkStartZ);
                } else {
                    searcher.searchForBlock(chunk, chunkHeightmap, xOffset, zOffset, chunkStartX, chunkStartZ);
                }

                if (searcher.getHeight() > 0 && !searcher.getState().getFluidState().isEmpty()) {
                    searcher.calcWaterDepth(chunk);
                }

                var mapColor = searcher.getState().getMapColor(world, searcher.pos);
                var biome = world.getBiome(searcher.pos);
                int shade;

                if (mapColor == MapColor.WATER_BLUE) {
                    double shadeTest = (double) searcher.getWaterDepth() * 0.1D + (double) (xOffset + zOffset & 1) * 0.2D;
                    shade = 1;
                    if (shadeTest < 0.5D) {
                        shade = 2;
                    }

                    if (shadeTest > 0.9D) {
                        shade = 0;
                    }
                } else {
                    double shadeTest = (searcher.getHeight() - lastHeights[xOffset]) * 4.0D / 5.0D + ((double) (xOffset + zOffset & 1) - 0.5D) * 0.4D;
                    shade = 1;
                    if (shadeTest > 0.6D) {
                        shade = 2;
                    }
                    if (shadeTest < -0.6D) {
                        shade = 0;
                    }
                }

                lastHeights[xOffset] = searcher.getHeight();
                int x = mapChunkStartX + xOffset;
                int z = mapChunkStartZ + zOffset;
                if (mapChunk.putPixelAndPreserve(x, z, (byte) (mapColor.id * 4 + shade), biome, searcher.getState())) {
                    this.hud.markDirty();
                }
            }
        }

        if (chunk instanceof WorldChunkExtension extendedChunk) {
            extendedChunk.lambdamap$markClean();
        }
    }

    public static File getWorldMapDirectorySP(MinecraftClient client, RegistryKey<World> worldKey) {
        var world = client.getServer().getWorld(worldKey);
        if (world == null) {
            world = client.getServer().getOverworld();
        }
        var worldDirectory = ((PersistentStateManagerAccessor) world.getPersistentStateManager()).getDirectory().getParentFile();
        var mapDirectory = new File(worldDirectory, NAMESPACE);
        mapDirectory.mkdirs();
        return mapDirectory;
    }

    public static File getWorldMapDirectoryMP(MinecraftClient client, RegistryKey<World> worldKey, long hashedSeed) {
        var serverInfo = client.getCurrentServerEntry();
        var gameDir = FabricLoader.getInstance().getGameDir().toFile();
        var lambdaMapDir = new File(gameDir, NAMESPACE);
        var serverDir = new File(lambdaMapDir, (serverInfo.name + "_" + serverInfo.address).replaceAll("[^A-Za-z0-9_.]", "_"));
        var seedDir = new File(serverDir, String.valueOf(hashedSeed));
        var worldDir = new File(seedDir, worldKey.getValue().getNamespace() + "/" + worldKey.getValue().getPath());
        if (!worldDir.exists())
            worldDir.mkdirs();
        return worldDir;
    }

    public static Identifier id(String path) {
        return new Identifier(NAMESPACE, path);
    }

    public static LambdaMap get() {
        return INSTANCE;
    }
}
