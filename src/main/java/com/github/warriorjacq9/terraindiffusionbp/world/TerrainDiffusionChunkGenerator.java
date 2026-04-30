package com.github.warriorjacq9.terraindiffusionbp.world;

import com.github.warriorjacq9.terraindiffusionbp.config.TerrainDiffusionConfig;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.LocalTerrainProvider;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.LocalTerrainProvider.HeightmapData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Optional;

public class TerrainDiffusionChunkGenerator extends ChunkGenerator {

    public static final Codec<TerrainDiffusionChunkGenerator> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source")
                            .forGetter(g -> g.biomeSource),
                    StructuresConfig.CODEC.fieldOf("structures_config")
                            .orElseGet(() ->
                                    new StructuresConfig(
                                            Optional.of(StructuresConfig.DEFAULT_STRONGHOLD),
                                            StructuresConfig.DEFAULT_STRUCTURES
                                    )
                            )
                            .forGetter(g -> g.structuresConfig)
            ).apply(instance, instance.stable(TerrainDiffusionChunkGenerator::new)));

    private final StructuresConfig structuresConfig;

    public TerrainDiffusionChunkGenerator(BiomeSource biomeSource, StructuresConfig structuresConfig) {
        super(biomeSource, biomeSource, structuresConfig, 0L);
        this.structuresConfig = structuresConfig;
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new TerrainDiffusionChunkGenerator(this.biomeSource.withSeed(seed), this.structuresConfig);
    }

    public void buildSurface(ChunkRegion region, Chunk chunk) {
        ChunkRandom random = new ChunkRandom();
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        random.setTerrainSeed(chunkX, chunkZ);

        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = startX + localX;
                int blockZ = startZ + localZ;
                int surfaceHeight = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, localX, localZ) + 1;

                Biome biome = region.getBiome(new BlockPos(blockX, surfaceHeight, blockZ));
                biome.buildSurface(
                        random,
                        chunk,
                        blockX, blockZ,
                        surfaceHeight,
                        0.0,
                        Blocks.STONE.getDefaultState(),
                        Blocks.WATER.getDefaultState(),
                        this.getSeaLevel(),
                        region.getSeed()
                );
            }
        }
    }

    @Override
    public void populateNoise(WorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int chunkX = chunk.getPos().getStartX();
        int chunkZ = chunk.getPos().getStartZ();

        int tileSize  = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int blockX = chunkX + localX;
                int blockZ = chunkZ + localZ;

                int tileX = blockX >> tileShift;
                int tileZ = blockZ >> tileShift;

                int blockStartX = tileX << tileShift;
                int blockStartZ = tileZ << tileShift;
                int blockEndX   = blockStartX + tileSize;
                int blockEndZ   = blockStartZ + tileSize;

                HeightmapData data = LocalTerrainProvider.getInstance()
                        .fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);

                int targetHeight = 64; // fallback sea level
                if (data != null && data.heightmap != null) {
                    int dataLocalX = Math.max(0, Math.min(data.width  - 1, blockX - blockStartX));
                    int dataLocalZ = Math.max(0, Math.min(data.height - 1, blockZ - blockStartZ));
                    targetHeight = HeightConverter.convertToMinecraftHeight(data.heightmap[dataLocalZ][dataLocalX]);
                }

                // Fill from bottom (y=0) up to targetHeight with stone, then water up to sea level
                for (int y = 0; y <= targetHeight; y++) {
                    mutable.set(blockX, y, blockZ);
                    chunk.setBlockState(mutable, Blocks.STONE.getDefaultState(), false);
                }
                for (int y = targetHeight + 1; y <= 62; y++) { // 62 = sea level - 1
                    mutable.set(blockX, y, blockZ);
                    chunk.setBlockState(mutable, Blocks.WATER.getDefaultState(), false);
                }
                // y=0 should always be bedrock
                mutable.set(blockX, 0, blockZ);
                chunk.setBlockState(mutable, Blocks.BEDROCK.getDefaultState(), false);
            }
        }
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap) {
        int tileSize  = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        int tileX = x >> tileShift;
        int tileZ = z >> tileShift;

        int blockStartX = tileX << tileShift;
        int blockStartZ = tileZ << tileShift;
        int blockEndX   = blockStartX + tileSize;
        int blockEndZ   = blockStartZ + tileSize;

        HeightmapData data = LocalTerrainProvider.getInstance()
                .fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);

        if (data != null && data.heightmap != null) {
            int localX = Math.max(0, Math.min(data.width  - 1, x - blockStartX));
            int localZ = Math.max(0, Math.min(data.height - 1, z - blockStartZ));
            return HeightConverter.convertToMinecraftHeight(data.heightmap[localZ][localX]);
        }
        return 64;
    }

    @Override
    public BlockView getColumnSample(int x, int z) {
        BlockState[] states = new BlockState[256];
        int height = getHeight(x, z, Heightmap.Type.OCEAN_FLOOR_WG);
        Arrays.fill(states, 0, Math.min(height + 1, 256), Blocks.STONE.getDefaultState());
        Arrays.fill(states, Math.max(0, height + 1), 62, Blocks.WATER.getDefaultState());
        Arrays.fill(states, Math.max(0, Math.max(height + 1, 62)), 256, Blocks.AIR.getDefaultState());
        states[0] = Blocks.BEDROCK.getDefaultState();
        return new VerticalBlockSample(states);
    }

    @Override
    public int getWorldHeight() {
        return 256;
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }
}