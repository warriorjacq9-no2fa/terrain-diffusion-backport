package com.github.warriorjacq9.terraindiffusionbp.world;

import java.util.List;

import com.mojang.serialization.Codec;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;

public class TerrainDiffusionBiomeSource extends BiomeSource {

    protected TerrainDiffusionBiomeSource(List<Biome> biomes) {
        super(biomes);
        //TODO Auto-generated constructor stub
    }

    @Override
    public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getBiomeForNoiseGen'");
    }

    @Override
    protected Codec<? extends BiomeSource> getCodec() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCodec'");
    }

    @Override
    public BiomeSource withSeed(long arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'withSeed'");
    }
    
}