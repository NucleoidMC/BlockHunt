package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.game.map.BlockHuntMapConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;

public record BlockHuntConfig(BlockHuntMapConfig mapConfig, WaitingLobbyConfig lobbyConfig, int hiderSolidifyTime, int endTime) {
    public static final MapCodec<BlockHuntConfig> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockHuntMapConfig.CODEC.fieldOf("map").forGetter(BlockHuntConfig::mapConfig),
            WaitingLobbyConfig.CODEC.optionalFieldOf("players", new WaitingLobbyConfig((FabricLoader.getInstance().isDevelopmentEnvironment() ? 1 : 2), 32)).forGetter(BlockHuntConfig::lobbyConfig),
            Codec.INT.optionalFieldOf("hider_solidify_time", SharedConstants.TICKS_PER_MINUTE).forGetter(BlockHuntConfig::hiderSolidifyTime),
            Codec.INT.optionalFieldOf("end_time", SharedConstants.TICKS_PER_MINUTE * 20).forGetter(BlockHuntConfig::endTime)
    ).apply(instance, BlockHuntConfig::new));
}