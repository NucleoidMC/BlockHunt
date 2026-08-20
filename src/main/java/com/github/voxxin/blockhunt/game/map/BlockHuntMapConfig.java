package com.github.voxxin.blockhunt.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;


public record BlockHuntMapConfig(Identifier id, float spawnRadius) {
    public static final MapCodec<BlockHuntMapConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(BlockHuntMapConfig::id),
            Codec.FLOAT.optionalFieldOf("spawn_radius", 4.5f).forGetter(BlockHuntMapConfig::spawnRadius)
    ).apply(instance, BlockHuntMapConfig::new));
}
