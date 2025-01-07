package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.BlockHunt;
import com.github.voxxin.blockhunt.game.map.BlockHuntMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.scoreboard.AbstractTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.Set;

public class BlockHuntSpawnLogic {
    private final GameSpace gameSpace;
    private final BlockHuntMap map;
    private final ServerWorld world;
    private final Set<PositionFlag> flags = ImmutableSet.of();

    public BlockHuntSpawnLogic(GameSpace gameSpace, ServerWorld world, BlockHuntMap map) {
        this.gameSpace = gameSpace;
        this.map = map;
        this.world = world;
    }

    public void resetPlayer(ServerPlayerEntity player, GameMode gameMode) {
        player.changeGameMode(gameMode);
        player.setVelocity(Vec3d.ZERO);
        player.fallDistance = 0.0f;
    }

    public void spawnPlayer(ServerPlayerEntity player, BlockHuntPlayer participant) {
        var spawns = map.spawns();
        if (spawns == null) {
            BlockHunt.LOGGER.error("Spawns are not defined!");
            return;
        }

        // Determine the default spawn position
        Vec3d spawnPos = (Vec3d) spawns.getOrDefault("spawn_everyone", spawns.get("spawn_hider"));
        if (participant == null) {
            player.teleport(this.world, spawnPos.x, spawnPos.y, spawnPos.z, flags, 0.0F, 0.0F, true);
            return;
        }

        // Determine team-specific spawn position
        Team team = participant.getTeam();
        if (team == null) {
            BlockHunt.LOGGER.error("Cannot spawn player! Team is not defined!");
            return;
        }

        switch (team.getName()) {
            case "seekers" -> spawnPos = (Vec3d) spawns.get("spawn_seeker");
            case "hiders" -> spawnPos = (Vec3d) spawns.get("spawn_hider");
            default -> {
                BlockHunt.LOGGER.error("Cannot spawn player! Unknown team: " + team.getName());
                return;
            }
        }

        // Find a safe position
        float radius = 4.5f;
        BlockPos safePos = null;
        for (int attempt = 0; attempt < 100; attempt++) {
            int x = MathHelper.floor(spawnPos.x + MathHelper.nextFloat(player.getRandom(), -radius, radius));
            int z = MathHelper.floor(spawnPos.z + MathHelper.nextFloat(player.getRandom(), -radius, radius));
            BlockPos pos = new BlockPos(x, MathHelper.floor(spawnPos.y), z);

            if (this.world.getBlockState(pos).isAir() &&
                    this.world.getBlockState(pos.up()).isAir() &&
                    !this.world.getBlockState(pos.down()).isAir()) {
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
        float xPos = safePos.getX() + 0.5f + MathHelper.nextFloat(player.getRandom(), -newRadius, newRadius);
        float zPos = safePos.getZ() + 0.5f + MathHelper.nextFloat(player.getRandom(), -newRadius, newRadius);

        // Teleport player
        player.teleport(this.world, xPos, safePos.getY(), zPos, flags, 0.0F, 0.0F, true);
    }
}
