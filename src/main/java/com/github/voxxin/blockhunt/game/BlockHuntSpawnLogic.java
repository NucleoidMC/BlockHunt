package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.BlockHunt;
import com.github.voxxin.blockhunt.game.map.BlockHuntMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.Set;

public class BlockHuntSpawnLogic {
    private final GameSpace gameSpace;
    private final BlockHuntMap map;
    private final ServerLevel level;
    private final Set<Relative> flags = ImmutableSet.of();

    public BlockHuntSpawnLogic(GameSpace gameSpace, ServerLevel level, BlockHuntMap map) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.level = level;
    }

    public void resetPlayer(ServerPlayer player, GameType gameMode) {
        player.setGameMode(gameMode);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0f;
    }

    public void spawnPlayer(ServerPlayer player, BlockHuntPlayer participant) {
        var spawns = map.spawns();
        if (spawns == null) {
            BlockHunt.LOGGER.error("Spawns are not defined!");
            return;
        }

        // Determine the default spawn position
        Vec3 spawnPos = (Vec3) spawns.getOrDefault("spawn_everyone", spawns.get("spawn_hider"));
        if (participant == null) {
            player.teleportTo(this.level, spawnPos.x, spawnPos.y, spawnPos.z, flags, 0.0F, 0.0F, true);
            return;
        }

        // Determine team-specific spawn position
        PlayerTeam team = participant.getTeam();
        if (team == null) {
            BlockHunt.LOGGER.error("Cannot spawn player! Team is not defined!");
            return;
        }

        switch (team.getName()) {
            case "seekers" -> spawnPos = (Vec3) spawns.get("spawn_seeker");
            case "hiders" -> spawnPos = (Vec3) spawns.get("spawn_hider");
            default -> {
                BlockHunt.LOGGER.error("Cannot spawn player! Unknown team: " + team.getName());
                return;
            }
        }

        // Find a safe position
        float radius = 4.5f;
        BlockPos safePos = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = Mth.floor(spawnPos.x + Mth.nextFloat(player.getRandom(), -radius, radius));
            int z = Mth.floor(spawnPos.z + Mth.nextFloat(player.getRandom(), -radius, radius));
            BlockPos pos = new BlockPos(x, Mth.floor(spawnPos.y), z);

            if (this.level.getBlockState(pos).isAir() &&
                    this.level.getBlockState(pos.above()).isAir() &&
                    !this.level.getBlockState(pos.below()).isAir()) {
                safePos = pos;
                break;
            }
        }

        if (safePos == null) {
            BlockHunt.LOGGER.error("Failed to find a safe spawn location.");
            return;
        }

        // Adjust position for finer placement
        float newRadius = 0.25f;
        float xPos = safePos.getX() + 0.5f + Mth.nextFloat(player.getRandom(), -newRadius, newRadius);
        float zPos = safePos.getZ() + 0.5f + Mth.nextFloat(player.getRandom(), -newRadius, newRadius);

        // Teleport player
        player.teleportTo(this.level, xPos, safePos.getY(), zPos, flags, 0.0F, 0.0F, true);
    }
}
