package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.BlockHunt;
import com.github.voxxin.blockhunt.game.map.BlockHuntMap;
import com.github.voxxin.blockhunt.game.util.BlockHuntAnimation;
import com.github.voxxin.blockhunt.game.util.BlockHuntBlock;
import com.github.voxxin.blockhunt.game.util.ext.WorldExt;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.TeamColor;
import xyz.nucleoid.plasmid.api.game.GameCloseReason;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.api.game.player.JoinOffer;
import xyz.nucleoid.plasmid.api.game.player.PlayerSet;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.util.PlayerRef;
import xyz.nucleoid.stimuli.event.EventResult;
import xyz.nucleoid.stimuli.event.block.BlockPunchEvent;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.github.voxxin.blockhunt.BlockHunt.deniedIDs;

public class BlockHuntActive {
    private final BlockHuntConfig config;

    public final GameSpace gameSpace;
    private final BlockHuntMap gameMap;
    private final Object2ObjectMap<PlayerRef, BlockHuntPlayer> participants;
    private final BlockHuntSpawnLogic spawnLogic;
    private final BlockHuntStageManager stageManager;
    private final boolean ignoreWinState;
    private final GlobalWidgets widgets;
    private BlockHuntSidebar sidebar;
    private final ServerLevel level;

    private static PlayerTeam seekersTeam = null;
    private static PlayerTeam hidersTeam = null;
    private static PlayerTeam spectatorTeam = null;

    private static final ArrayList<Block> deniedBlockInteractions = new ArrayList<>();

    private static final ArrayList<Block> allowedMapDisguises = new ArrayList<>();
    private BlockHuntActive(GameSpace gameSpace, ServerLevel level, BlockHuntMap map, GlobalWidgets widgets, BlockHuntConfig config, Set<PlayerRef> participants) {
        this.gameSpace = gameSpace;
        this.config = config;
        this.gameMap = map;
        this.spawnLogic = new BlockHuntSpawnLogic(gameSpace, level, map);
        this.participants = new Object2ObjectOpenHashMap<>();
        this.level = level;

        for (PlayerRef player : participants) {
            this.participants.put(player, new BlockHuntPlayer(level, player));
        }

        this.stageManager = new BlockHuntStageManager();
        this.ignoreWinState = this.participants.size() <= 1;
        this.widgets = widgets;
    }

    public static void open(GameSpace gameSpace, ServerLevel level, BlockHuntMap map, BlockHuntConfig config) {
        gameSpace.setActivity(game -> {
            Set<PlayerRef> participants = gameSpace.getPlayers().stream()
                    .map(PlayerRef::of)
                    .collect(Collectors.toSet());
            GlobalWidgets widgets = GlobalWidgets.addTo(game);
            BlockHuntActive active = new BlockHuntActive(gameSpace, level, map, widgets, config, participants);

            allowedMapDisguises.clear();
            deniedBlockInteractions.clear();

            map.noInteractList().forEach((blockPos) -> {
                deniedBlockInteractions.add(level.getBlockState((BlockPos) blockPos).getBlock());
            });

            map.allowedDisguises().forEach((blockPos) -> {
                allowedMapDisguises.add(level.getBlockState((BlockPos) blockPos).getBlock());
            });

            if (allowedMapDisguises.size() == 0) {
                BlockHunt.LOGGER.fatal("No allowed disguises found! Please setup a 'map_disguises' region for your map.");
                gameSpace.close(GameCloseReason.ERRORED);
            }

            // Create teams

            if (level.getScoreboard().getPlayerTeam("seekers") != null) level.getScoreboard().removePlayerTeam(level.getScoreboard().getPlayerTeam("seekers"));
            if (level.getScoreboard().getPlayerTeam("hiders") != null) level.getScoreboard().removePlayerTeam(level.getScoreboard().getPlayerTeam("hiders"));
            if (level.getScoreboard().getPlayerTeam("specs") != null) level.getScoreboard().removePlayerTeam(level.getScoreboard().getPlayerTeam("specs"));

            seekersTeam = level.getScoreboard().addPlayerTeam("seekers");
            hidersTeam = level.getScoreboard().addPlayerTeam("hiders");
            spectatorTeam = level.getScoreboard().addPlayerTeam("specs");

            level.getScoreboard().getPlayerTeam(seekersTeam.getName()).setCollisionRule(Team.CollisionRule.NEVER);

            level.getScoreboard().getPlayerTeam(hidersTeam.getName()).setCollisionRule(Team.CollisionRule.NEVER);
            level.getScoreboard().getPlayerTeam(hidersTeam.getName()).setSeeFriendlyInvisibles(false);
            level.getScoreboard().getPlayerTeam(hidersTeam.getName()).setNameTagVisibility(Team.Visibility.NEVER);

            level.getScoreboard().getPlayerTeam(seekersTeam.getName()).setColor(Optional.of(TeamColor.RED));
            level.getScoreboard().getPlayerTeam(seekersTeam.getName()).setPlayerPrefix(Component.nullToEmpty("(⚔) "));
            level.getScoreboard().getPlayerTeam(hidersTeam.getName()).setColor(Optional.of(TeamColor.YELLOW));
            level.getScoreboard().getPlayerTeam(hidersTeam.getName()).setPlayerPrefix(Component.nullToEmpty("(\uD83D\uDEE1) "));


            // Game Rules
            game.setRule(GameRuleType.FALL_DAMAGE, EventResult.DENY);
            game.setRule(GameRuleType.PICKUP_ITEMS, EventResult.DENY);
            game.setRule(GameRuleType.THROW_ITEMS, EventResult.DENY);
            game.setRule(GameRuleType.CRAFTING, EventResult.DENY);
            game.setRule(GameRuleType.FIRE_TICK, EventResult.DENY);
            game.setRule(GameRuleType.FLUID_FLOW, EventResult.DENY);
            game.setRule(GameRuleType.HUNGER, EventResult.DENY);
            game.setRule(GameRuleType.MODIFY_ARMOR, EventResult.DENY);
            //game.setRule(GameRuleType.PLACE_BLOCKS, EventResult.DENY);


            // Unique Gamerules

            game.listen(GameActivityEvents.ENABLE, active::onOpen);

            game.listen(GamePlayerEvents.OFFER, JoinOffer::accept);
            game.listen(GamePlayerEvents.ACCEPT, (offer) -> offer.teleport(level, map.getSpawnPos()));
            game.listen(GamePlayerEvents.ADD, active::addPlayer);
            game.listen(GamePlayerEvents.REMOVE, active::removePlayer);

            game.listen(GameActivityEvents.TICK, active::tick);

            game.listen(PlayerDamageEvent.EVENT, active::onPlayerDamage);
            game.listen(PlayerDeathEvent.EVENT, active::onPlayerDeath);

            game.listen(BlockUseEvent.EVENT, active::allowInteraction);
            game.listen(BlockPunchEvent.EVENT, active::blockAttack);
        });
    }

    private void onOpen() {
        //Sets default start time if present
        if (this.gameMap.animations().stream().anyMatch(animation -> animation.animationName.getPath().equals("seeker"))) {
            BlockHuntAnimation seekerAnimation = this.gameMap.animations().stream().filter(animation -> animation.animationName.getPath().equals("seeker")).findFirst().orElseGet(null);
            seekerAnimation.settings.setLevel(level);
            seekerAnimation.settings.tick();
            if (seekerAnimation.settings.startTime == 0) {
                BlockHunt.LOGGER.info("No start time set for seeker released animation. Defaulting to 1 minute.");
                int mins = 1200;
                this.stageManager.onOpen(this.level.getGameTime(), this.config, mins);
            } else this.stageManager.onOpen(this.level.getGameTime(), this.config, (long) seekerAnimation.settings.startTime);
        } else {
            BlockHunt.LOGGER.fatal("No seeker released animation found. Please add one to your map.");
            gameSpace.close(GameCloseReason.ERRORED);
        }
        BlockHunt.deniedIDs.clear();

        PlayerSet players = this.gameSpace.getPlayers();

        ServerPlayer firstSeeker = players.stream().toList().get(new Random().nextInt(players.stream().toList().size()));
        assert firstSeeker != null;
        this.participants.get(PlayerRef.of(firstSeeker)).setTeam(seekersTeam);
        players.stream().iterator().forEachRemaining((ServerPlayer player) -> {
            if (player != firstSeeker) {
                BlockHuntPlayer blockHuntPlayer = this.participants.get(PlayerRef.of(player));
                blockHuntPlayer.setTeam(hidersTeam);

                Block block = allowedMapDisguises.get(new Random().nextInt(allowedMapDisguises.size()));
                BlockHuntBlock blockEntity = getEntityFromBlock(block);
                level.addFreshEntity(blockEntity);
                blockHuntPlayer.setDisguise(blockEntity, block);
                blockHuntPlayer.updateTimeBar(null);
                deniedIDs.add(player.getId());
            }
        });

        for (PlayerRef ref : this.participants.keySet()) {
            ref.ifOnline(this.level, this::spawnParticipant);
        }

        this.sidebar = new BlockHuntSidebar(widgets, config.mapConfig().id(), this.level, this.stageManager);
    }

    private void onClose() {
    }

    private InteractionResult allowInteraction(ServerPlayer serverPlayerEntity, InteractionHand hand, BlockHitResult blockHitResult) {
        if (level == null) return InteractionResult.CONSUME;

        Block clickedBlock = level.getBlockState(blockHitResult.getBlockPos()).getBlock();
        BlockHuntPlayer player = this.participants.get(PlayerRef.of(serverPlayerEntity));

        boolean isSameBlock = player.prevBlockhitResult != null && player.prevBlockhitResult.equals(blockHitResult.getBlockPos());
        player.prevBlockhitResult = blockHitResult.getBlockPos();

        for (Block block : allowedMapDisguises) {

            if (block == clickedBlock && serverPlayerEntity.isAlliedTo(hidersTeam) && player.getDisguiseB() == clickedBlock && !isSameBlock) {
                serverPlayerEntity.sendSystemMessage(
                        Component.translatable("event.blockhunt.already_block", clickedBlock.getName().withStyle(ChatFormatting.AQUA))
                                .withStyle(ChatFormatting.GREEN),
                        false);

                return InteractionResult.SUCCESS;
            } else if (block == clickedBlock && serverPlayerEntity.isAlliedTo(hidersTeam) && player.getDisguiseB() != clickedBlock && !isSameBlock) {
                player.setDisguise(clickedBlock);
                player.updateTimeBar(true);

                serverPlayerEntity.sendSystemMessage(
                        Component.translatable("event.blockhunt.block_changed", clickedBlock.getName().withStyle(ChatFormatting.AQUA))
                                .withStyle(ChatFormatting.GREEN),
                        false);

                return InteractionResult.SUCCESS;
            }
        }

        for (Block block : deniedBlockInteractions) {

            if (block == clickedBlock) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.PASS;
    }

    private EventResult blockAttack(ServerPlayer serverPlayerEntity, Direction direction, BlockPos blockPos) {
        BlockPos blockPosHit = ((BlockHitResult) serverPlayerEntity.pick(5, 0, false)).getBlockPos();
        BlockHuntPlayer player = this.participants.get(PlayerRef.of(serverPlayerEntity));

        participants.keySet().stream().iterator().forEachRemaining((ref) -> {
            ServerPlayer serverPlayer = ref.getEntity(level);
            BlockHuntPlayer blockHuntPlayer = participants.get(ref);

            if (blockHuntPlayer.getPositionHidden() != null && blockHuntPlayer.getPositionHidden().equals(blockPosHit) && player.getTeam() == seekersTeam) {
                serverPlayerEntity.attack(serverPlayer);
            }
        });

        return EventResult.DENY;
    }

    private void addPlayer(ServerPlayer player) {
        if (level.getGameTime() < stageManager.finishTime && level.getGameTime() > stageManager.startTime) {
            this.spawnLogic.resetPlayer(player, GameType.SPECTATOR);
        }
    }

    private void removePlayer(ServerPlayer player) {
        player.removeAllEffects();
        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR || player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL) player.setGameMode(GameType.ADVENTURE);
        BlockHuntPlayer blockHuntPlayer = this.participants.get(PlayerRef.of(player));
        blockHuntPlayer.resetDisguise();
        blockHuntPlayer.removeTimeBar();
        blockHuntPlayer.setTeam(spectatorTeam);
        blockHuntPlayer.setHidden(false);
        blockHuntPlayer.lastPosition = null;
        if (blockHuntPlayer.getTimeBar() != null) this.level.getServer().getCustomBossEvents().remove(blockHuntPlayer.getTimeBar());
        blockHuntPlayer.removeTimeBar();
        this.participants.remove(PlayerRef.of(player));
        if (deniedIDs.contains(player.getId())) {
            deniedIDs.remove((Integer) player.getId());
        }
    }

    private EventResult onPlayerDamage(ServerPlayer player, DamageSource source, float amount) {
        if (source.getDirectEntity() == null) return EventResult.DENY;

        if (source.getDirectEntity().isAlwaysTicking()) {
            if (!player.isAlliedTo(source.getEntity())) {
                BlockHuntPlayer thisPlayer = this.participants.get(PlayerRef.of(player));
                if (thisPlayer.isHidden()) {
                    ((WorldExt) this.level).blockHunt$setBlockState(thisPlayer.getPositionHidden(), Blocks.AIR.defaultBlockState());
                    thisPlayer.updateTimeBar(true);
                    thisPlayer.setHidden(false);
                }
                return EventResult.ALLOW;
            }
        }

        return EventResult.DENY;
    }

    private EventResult onPlayerDeath(ServerPlayer player, DamageSource source) {
        if (source.getDirectEntity().isAlwaysTicking()) {
            BlockHuntPlayer thisPlayer = this.participants.get(PlayerRef.of(player));
            BlockHuntPlayer thisAttacker = this.participants.get(PlayerRef.of((ServerPlayer) source.getEntity()));

            thisPlayer.playerDeath();
            player.setGameMode(GameType.SPECTATOR);

            if (player.isAlliedTo(hidersTeam)) {
                this.broadcastMessage(
                        Component.literal("")
                                .append(
                                        Component.literal(" ! "
                                        ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                                )
                                .append(
                                        Component.translatable("event.blockhunt.hider_found",
                                                        source.getEntity().getName().copy().withStyle(ChatFormatting.RED),
                                                        player.getName().copy().withStyle(ChatFormatting.YELLOW))
                                                .withStyle(ChatFormatting.WHITE)
                                )
                );


                this.participants.get(PlayerRef.of(player)).setTeam(seekersTeam);
                this.participants.get(PlayerRef.of(player)).removeTimeBar();
                this.participants.get(PlayerRef.of(player)).resetDisguise();
                this.participants.get(PlayerRef.of(player)).setHidden(false);
                deniedIDs.remove((Integer) player.getId());
                ((ServerPlayer) source.getEntity()).setHealth(20);
            } else if (player.isAlliedTo(seekersTeam)) {
                this.broadcastMessage(
                        Component.literal("")
                                .append(
                                        Component.literal(" ! "
                                        ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                                )
                                .append(
                                        Component.translatable("event.blockhunt.seeker_killed",
                                                player.getName().copy().withStyle(ChatFormatting.YELLOW),
                                                source.getEntity().getName().copy().withStyle(ChatFormatting.RED)
                                        ).withStyle(ChatFormatting.WHITE)
                                )
                );
            }
        }

        return EventResult.DENY;
    }

    private void spawnParticipant(ServerPlayer player) {
        BlockHuntPlayer participant = this.participants.get(PlayerRef.of(player));

        if (participant.getTeam() == null) {
            participant.setTeam(hidersTeam);
        }
        this.spawnLogic.resetPlayer(player, GameType.SURVIVAL);
        participant.loadPlayerInventory();
        this.spawnLogic.spawnPlayer(player, participant);
    }



    private void tick() {
        long time = this.level.getGameTime();
        BlockHuntStageManager.IdleTickResult result = this.stageManager.tick(time, gameSpace);

        switch (result) {
            case CONTINUE_TICK:
                break;
            case TICK_FINISHED:
                return;
            case GAME_FINISHED:
                this.broadcastWin(this.checkWinResult());
                return;
            case GAME_CLOSED:
                this.gameSpace.close(GameCloseReason.FINISHED);
                return;
        }

        if (gameSpace.getPlayers().size() == 1) this.gameSpace.close(GameCloseReason.CANCELED);

        this.participants.keySet().forEach(ref -> {
            if (!ref.isOnline(this.level)) {
                BlockHuntPlayer blockHuntPlayer = this.participants.get(ref);
                blockHuntPlayer.resetDisguise();
                blockHuntPlayer.removeTimeBar();
                blockHuntPlayer.setTeam(spectatorTeam);
                blockHuntPlayer.setHidden(false);
                blockHuntPlayer.lastPosition = null;
                this.participants.remove(ref);
            }
        });

        for (BlockHuntAnimation animation : this.gameMap.animations()) {
            animation.setLevel(this.level);
            animation.tick(this.stageManager.startTime, time);
        }

        this.participants.keySet().forEach(ref -> ref.ifOnline(this.level, player -> {
            BlockHuntPlayer blockHuntPlayer = this.participants.get(ref);
            PlayerSet players = this.gameSpace.getPlayers();

            if (blockHuntPlayer.getDisguiseE() != null && !blockHuntPlayer.isHidden()) {
                blockHuntPlayer.getDisguiseE().setPos(player.position().subtract(0.5, 0, 0.5));
            } else if (blockHuntPlayer.getDisguiseE() != null && blockHuntPlayer.isHidden()) blockHuntPlayer.getDisguiseE().setPos(new Vec3(0, 0,0));

            if (blockHuntPlayer.lastPosition != null && blockHuntPlayer.getTeam() == hidersTeam) {
                Vec3i roundedPos = new Vec3i((int) Math.floor(player.position().x), (int) Math.floor(player.position().y), (int) Math.floor(player.position().z));
                Vec3i roundedOldPos = new Vec3i((int) Math.floor(blockHuntPlayer.lastPosition.x), (int) Math.floor(blockHuntPlayer.lastPosition.y), (int) Math.floor(blockHuntPlayer.lastPosition.z));
                if ((!roundedPos.equals(roundedOldPos) && blockHuntPlayer.getTimeUntilHidden() != 1F) || (!level.getBlockState(BlockPos.containing(blockHuntPlayer.lastPosition)).getBlock().equals(Blocks.AIR) && blockHuntPlayer.getPositionHidden() != player.blockPosition())) {
                    if (blockHuntPlayer.isHidden()) {
                        ((WorldExt) level).blockHunt$setBlockState(blockHuntPlayer.getPositionHidden(), Blocks.AIR.defaultBlockState());
                    }
                    blockHuntPlayer.updateTimeBar(true);
                    blockHuntPlayer.setHidden(false);
                    blockHuntPlayer.lastRealSecond = 0;
                }
            }

            if (blockHuntPlayer.getTeam() == hidersTeam && blockHuntPlayer.lastPosition != null && blockHuntPlayer.lastRealSecond == 20 && !blockHuntPlayer.isHidden()) {
                Vec3i roundedPos = new Vec3i((int) Math.floor(player.position().x), (int) Math.floor(player.position().y), (int) Math.floor(player.position().z));
                Vec3i roundedOldPos = new Vec3i((int) Math.floor(blockHuntPlayer.lastPosition.x), (int) Math.floor(blockHuntPlayer.lastPosition.y), (int) Math.floor(blockHuntPlayer.lastPosition.z));
                blockHuntPlayer.updateTimeBar(!roundedPos.equals(roundedOldPos));
            }
            if (blockHuntPlayer.getTimeUntilHidden() <= 0 && blockHuntPlayer.getTeam() == hidersTeam && !blockHuntPlayer.isHidden()) {

                ((WorldExt) level).blockHunt$setBlockState(player.blockPosition(), blockHuntPlayer.getDisguiseB().defaultBlockState());
                blockHuntPlayer.setHidden(true);
            }

            if (blockHuntPlayer.respawnTicks == 1) spawnParticipant(player);
            if (blockHuntPlayer.respawnTicks != 0) blockHuntPlayer.respawnTicks--;

            blockHuntPlayer.lastPosition = player.position();

            if (blockHuntPlayer.lastRealSecond == 20) {
                blockHuntPlayer.lastRealSecond = 0;
            } else blockHuntPlayer.lastRealSecond++;
        }));

        if (this.level.getScoreboard().getPlayerTeam(hidersTeam.getName()).getPlayers().size() == 0) this.broadcastWin(this.checkWinResult());
        this.sidebar.tick();
    }

    private BlockHuntBlock getEntityFromBlock(Block block) {
        BlockHuntBlock blockEntity = new BlockHuntBlock(EntityTypes.BLOCK_DISPLAY, this.level);
        blockEntity.setBlockState(block.defaultBlockState());
        return blockEntity;
    }

    private void broadcastMessage(String message) {
        this.gameSpace.getPlayers().sendMessage(Component.nullToEmpty(message));
    }

    private void broadcastMessage(Component message) {
        this.gameSpace.getPlayers().sendMessage(message);
    }

    private void broadcastWin(WinResult result) {
        PlayerTeam winningTeam = result.getWinningTeam();

        AtomicInteger seekers = new AtomicInteger();

        participants.values().stream().iterator().forEachRemaining(player -> {
            if (player.getTeam() != seekersTeam) {
                seekers.getAndIncrement();
            }
        });

        PlayerSet players = this.gameSpace.getPlayers();
        players.sendMessage(Component.literal("").append(
                                Component.literal(" ! ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                        ).append(
                                Component.translatable("event.blockhunt.win", winningTeam.getDisplayName().copy().withStyle(ChatFormatting.YELLOW))
                                        .withStyle(ChatFormatting.WHITE)
                        ).withStyle(ChatFormatting.WHITE)

                        .append("\n")
                        .append(Component.literal("  - ").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))

                        .append(
                                winningTeam == seekersTeam ? Component.translatable("event.blockhunt.win_hider",

                                        Component.literal(
                                                String.valueOf(seekers.get()).formatted(ChatFormatting.YELLOW)
                                        ))

                                        : Component.translatable("event.blockhunt.win_seeker",

                                        Component.literal(String.valueOf(this.participants.size() - 1))
                                                .withStyle(ChatFormatting.YELLOW)
                                )
                        )


        );
        players.playSound(SoundEvents.VILLAGER_YES);
        gameSpace.close(GameCloseReason.FINISHED);
    }

    private WinResult checkWinResult() {
        if (this.ignoreWinState) {
            return WinResult.no();
        }

        if (participants.values().stream().anyMatch(player -> player.getTeam() == hidersTeam)) {
            return WinResult.win(hidersTeam);
        } else {
            return WinResult.win(seekersTeam);
        }
    }

    static class WinResult {
        final PlayerTeam winningTeam;
        final boolean win;

        private WinResult(PlayerTeam winningTeam, boolean win) {
            this.winningTeam = winningTeam;
            this.win = win;
        }

        static WinResult no() {
            return new WinResult(null, false);
        }

        static WinResult win(PlayerTeam winningTeam) {
            return new WinResult(winningTeam, true);
        }

        public boolean isWin() {
            return this.win;
        }

        public PlayerTeam getWinningTeam() {
            return this.winningTeam;
        }
    }
}