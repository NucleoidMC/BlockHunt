package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.BlockHunt;
import com.github.voxxin.blockhunt.game.map.BlockHuntMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameType;
import xyz.nucleoid.plasmid.api.game.*;
import xyz.nucleoid.plasmid.api.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.api.game.common.config.WaitingLobbyConfig;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BlockHuntWaiting {
    private final GameSpace gameSpace;
    private static BlockHuntMap thisMap;
    private final BlockHuntConfig config;
    private final BlockHuntSpawnLogic spawnLogic;
    private final ServerLevel level;
    private static final List<Block> deniedBlockInteractions = new ArrayList<>();

    private static boolean warnedForSpawns = false;

    private BlockHuntWaiting(GameSpace gameSpace, ServerLevel level, BlockHuntMap map, BlockHuntConfig config) {
        this.gameSpace = gameSpace;
        thisMap = map;
        this.config = config;
        this.level = level;
        this.spawnLogic = new BlockHuntSpawnLogic(gameSpace, level, map);
    }

    public static GameOpenProcedure open(GameOpenContext<BlockHuntConfig> context) {
        WaitingLobbyConfig config = new WaitingLobbyConfig(2, 32);
        BlockHuntMap map;

        try {
            map = BlockHuntMap.from(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        thisMap = map;

        return context.openWithLevel(map.levelConfig(), (game, level) -> {
            BlockHuntWaiting waiting = new BlockHuntWaiting(game.getGameSpace(), level, map, context.config());

            GameWaitingLobby.addTo(game, config);

            deniedBlockInteractions.clear();

            map.noInteractList().forEach((blockPos) -> {
                deniedBlockInteractions.add(level.getBlockState((BlockPos) blockPos).getBlock());
            });

            level.getServer().getCustomBossEvents().getEvents().forEach((bossBar) -> {
                level.getServer().getCustomBossEvents().remove(bossBar);
            });

            // Game Rules
            game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
            game.setRule(GameRuleType.PICKUP_ITEMS, EventResult.DENY);
            game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
            game.setRule(GameRuleType.BREAK_BLOCKS, EventResult.DENY);
            game.setRule(GameRuleType.FIRE_TICK, EventResult.DENY);
            game.setRule(GameRuleType.FLUID_FLOW, EventResult.DENY);
            game.setRule(GameRuleType.HUNGER, EventResult.DENY);
            game.setRule(GameRuleType.MODIFY_ARMOR, EventResult.DENY);
            game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);

            game.listen(GameActivityEvents.REQUEST_START, waiting::requestStart);
            game.listen(GamePlayerEvents.ADD, waiting::addPlayer);
            game.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            game.listen(GamePlayerEvents.ACCEPT, (offer) -> offer.teleport(level, map.getSpawnPos()));
            game.listen(PlayerDeathEvent.EVENT, waiting::onPlayerDeath);

            game.listen(BlockUseEvent.EVENT, waiting::allowInteraction);
        });
    }

    private InteractionResult allowInteraction(ServerPlayer serverPlayerEntity, InteractionHand hand, BlockHitResult blockHitResult) {
        if (deniedBlockInteractions.isEmpty()) return InteractionResult.SUCCESS;
        if (level == null) return InteractionResult.SUCCESS;

        for (Block block : deniedBlockInteractions) {
            if (block == level.getBlockState(blockHitResult.getBlockPos()).getBlock()) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.SUCCESS;
    }

    private GameResult requestStart() {
        BlockHuntActive.open(this.gameSpace, this.level, thisMap, this.config);
        return GameResult.ok();
    }

    private void addPlayer(ServerPlayer player) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        player.setHealth(20.0f);
        this.spawnPlayer(player);
        return EventResult.DENY;
    }

    private void spawnPlayer(ServerPlayer player) {
        if (thisMap.spawns().entrySet().stream().noneMatch((entry) -> entry.getKey().equals("spawn_seeker")) || thisMap.spawns().entrySet().stream().noneMatch((entry) -> entry.getKey().equals("spawn_hider"))) {
            boolean noSeeker = thisMap.spawns().entrySet().stream().noneMatch((entry) -> entry.getKey().equals("spawn_seeker"));
            boolean noHider = thisMap.spawns().entrySet().stream().noneMatch((entry) -> entry.getKey().equals("spawn_hider"));
            if (noHider && noSeeker)
                BlockHunt.LOGGER.fatal("No hider or seeker spawn points were found for this map.");
            else if (noHider) BlockHunt.LOGGER.fatal("No hider spawn point was found for this map.");
            else if (noSeeker) BlockHunt.LOGGER.fatal("No seeker spawn point was found for this map.");

            this.gameSpace.close(GameCloseReason.ERRORED);
            return;
        }

        if (thisMap.spawns().entrySet().stream().noneMatch((entry) -> entry.getKey().equals("spawn_everyone")) && !warnedForSpawns) {
            BlockHunt.LOGGER.info("No default spawn point was found for this map.");
            warnedForSpawns = true;
        }

        this.spawnLogic.resetPlayer(player, GameType.ADVENTURE);
        this.spawnLogic.spawnPlayer(player, null);
    }
}