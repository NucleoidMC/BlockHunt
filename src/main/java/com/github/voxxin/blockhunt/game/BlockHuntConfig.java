package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.game.map.BlockHuntMapConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;

public record BlockHuntConfig(BlockHuntMapConfig mapConfig, int hiderSolidifyTime, int endTime) {
    public static final MapCodec<BlockHuntConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockHuntMapConfig.CODEC.fieldOf("map").forGetter(BlockHuntConfig::mapConfig),
            Codec.INT.optionalFieldOf("hider_solidify_time", SharedConstants.TICKS_PER_MINUTE).forGetter(BlockHuntConfig::hiderSolidifyTime),
            Codec.INT.optionalFieldOf("end_time", SharedConstants.TICKS_PER_MINUTE * 20).forGetter(BlockHuntConfig::endTime)
    ).apply(instance, BlockHuntConfig::new));
}