package com.github.warriorjacq9.terraindiffusionbp.world;

import com.github.warriorjacq9.terraindiffusionbp.config.TerrainDiffusionConfig;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.LocalTerrainProvider;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.LocalTerrainProvider.HeightmapData;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.dynamic.RegistryLookupCodec;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeSource;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class TerrainDiffusionBiomeSource extends BiomeSource {

    // 1.16.5: Codec takes a Registry<Biome> directly
    public static final Codec<TerrainDiffusionBiomeSource> CODEC =
            RecordCodecBuilder.create((instance) ->
                instance.group(
                        RegistryLookupCodec.of(Registry.BIOME_KEY)
                                .forGetter(s -> s.biomeRegistry)
                ).apply(instance, instance.stable(TerrainDiffusionBiomeSource::new)));

    private final Registry<Biome> biomeRegistry;
    private Map<Short, Biome> biomeIdMap = null;

    public TerrainDiffusionBiomeSource(Registry<Biome> biomeRegistry) {
        this(biomeRegistry, buildBiomeIdMap(biomeRegistry));
    }

    private TerrainDiffusionBiomeSource(Registry<Biome> biomeRegistry, Map<Short, Biome> biomeIdMap) {
        super(new ArrayList<>(biomeIdMap.values()));
        this.biomeRegistry = biomeRegistry;
        this.biomeIdMap = biomeIdMap;
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        return CODEC;
    }

    @Override
    public BiomeSource withSeed(long seed) {
        return new TerrainDiffusionBiomeSource(this.biomeRegistry);
    }

    private static Map<Short, Biome> buildBiomeIdMap(Registry<Biome> biomeRegistry) {
        Map<Short, Biome> map = new HashMap<>();
        map.put((short) 1,  biomeRegistry.getOrThrow(BiomeKeys.PLAINS));
        map.put((short) 3,  biomeRegistry.getOrThrow(BiomeKeys.SNOWY_TUNDRA));  // SNOWY_PLAINS -> SNOWY_TUNDRA in 1.16
        map.put((short) 5,  biomeRegistry.getOrThrow(BiomeKeys.DESERT));
        map.put((short) 6,  biomeRegistry.getOrThrow(BiomeKeys.SWAMP));
        map.put((short) 8,  biomeRegistry.getOrThrow(BiomeKeys.FOREST));
        map.put((short) 15, biomeRegistry.getOrThrow(BiomeKeys.TAIGA));
        map.put((short) 16, biomeRegistry.getOrThrow(BiomeKeys.SNOWY_TAIGA));
        map.put((short) 17, biomeRegistry.getOrThrow(BiomeKeys.SAVANNA));
        map.put((short) 19, biomeRegistry.getOrThrow(BiomeKeys.MOUNTAINS));      // WINDSWEPT_HILLS -> MOUNTAINS
        map.put((short) 23, biomeRegistry.getOrThrow(BiomeKeys.JUNGLE));
        map.put((short) 26, biomeRegistry.getOrThrow(BiomeKeys.BADLANDS));
        // (short) 29 MEADOW        - does not exist in 1.16.5, remap
        map.put((short) 29, biomeRegistry.getOrThrow(BiomeKeys.PLAINS));
        // (short) 31 GROVE         - does not exist in 1.16.5, remap
        map.put((short) 31, biomeRegistry.getOrThrow(BiomeKeys.SNOWY_TAIGA));
        // (short) 32 SNOWY_SLOPES  - does not exist in 1.16.5, remap
        map.put((short) 32, biomeRegistry.getOrThrow(BiomeKeys.SNOWY_TUNDRA));
        // (short) 33 FROZEN_PEAKS  - does not exist in 1.16.5, remap
        map.put((short) 33, biomeRegistry.getOrThrow(BiomeKeys.ICE_SPIKES));
        // (short) 35 STONY_PEAKS   - does not exist in 1.16.5, remap
        map.put((short) 35, biomeRegistry.getOrThrow(BiomeKeys.GRAVELLY_MOUNTAINS));
        map.put((short) 41, biomeRegistry.getOrThrow(BiomeKeys.WARM_OCEAN));
        map.put((short) 44, biomeRegistry.getOrThrow(BiomeKeys.OCEAN));
        map.put((short) 46, biomeRegistry.getOrThrow(BiomeKeys.COLD_OCEAN));
        map.put((short) 48, biomeRegistry.getOrThrow(BiomeKeys.FROZEN_OCEAN));
        map.put((short) 108, biomeRegistry.getOrThrow(BiomeKeys.FOREST));
        map.put((short) 115, biomeRegistry.getOrThrow(BiomeKeys.TAIGA));
        map.put((short) 116, biomeRegistry.getOrThrow(BiomeKeys.SNOWY_TAIGA));
        return map;
    }

    private void requireBiomeIdMap() {
        if (biomeIdMap == null) {
            biomeIdMap = buildBiomeIdMap(biomeRegistry);
        }
    }

    @Override
    public Biome getBiomeForNoiseGen(int x, int y, int z) {
        requireBiomeIdMap();
        Biome defaultBiome = biomeIdMap.get((short) 1);

        // In 1.16.5 these are already biome coords (block >> 2), convert to block coords
        int blockX = x << 2;
        int blockZ = z << 2;

        int tileSize  = TerrainDiffusionConfig.tileSize();
        int tileShift = Integer.numberOfTrailingZeros(tileSize);

        int tileX = blockX >> tileShift;
        int tileZ = blockZ >> tileShift;

        int blockStartX = tileX << tileShift;
        int blockStartZ = tileZ << tileShift;
        int blockEndX   = blockStartX + tileSize;
        int blockEndZ   = blockStartZ + tileSize;

        HeightmapData data = LocalTerrainProvider.getInstance()
                .fetchHeightmap(blockStartZ, blockStartX, blockEndZ, blockEndX);

        if (data != null && data.biomeIds != null) {
            int localX = Math.max(0, Math.min(data.width  - 1, blockX - blockStartX));
            int localZ = Math.max(0, Math.min(data.height - 1, blockZ - blockStartZ));
            Biome entry = biomeIdMap.get(data.biomeIds[localZ][localX]);
            if (entry != null) return entry;
        }

        return defaultBiome;
    }
}