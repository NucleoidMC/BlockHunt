package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.game.map.BlockHuntMapConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record BlockHuntConfig(BlockHuntMapConfig mapConfig) {
    public static final MapCodec<BlockHuntConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockHuntMapConfig.CODEC.fieldOf("map").forGetter(BlockHuntConfig::mapConfig)
    ).apply(instance, BlockHuntConfig::new));
}