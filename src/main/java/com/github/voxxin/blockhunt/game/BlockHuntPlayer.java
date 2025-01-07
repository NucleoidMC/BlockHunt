package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.game.util.BlockHuntBlock;
import com.github.voxxin.blockhunt.game.util.BlockHuntBossBar;
import com.github.voxxin.blockhunt.game.util.BlockHuntTitle;
import net.minecraft.block.Block;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.UnbreakableComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class BlockHuntPlayer {
    private final ServerWorld world;
    private final PlayerRef playerRef;
    private final ServerPlayerEntity player;
    private BlockHuntBossBar.HideTimeBossbar bossBar;
    private Team team = null;
    private Object[] disguise = new Object[2];
    private boolean isHidden;
    public Vec3d lastPosition;
    public int respawnTicks = 0;
    private BlockPos positionHidden = null;
    public BlockPos prevBlockhitResult;

    public int lastRealSecond = 0;

    public BlockHuntPlayer(ServerWorld world, PlayerRef player) {
        this.world = world;
        this.playerRef = player;
        this.player = player.getEntity(world);
    }

    public void setTeam(Team team) {
        world.getScoreboard().addScoreHolderToTeam(player.getNameForScoreboard(), team);
        this.team = team;
    }

    public Team getTeam() {
        return this.team;
    }

    public void setDisguise(BlockHuntBlock disguise) {
        if (disguise == null) return;

        this.disguise = new Object[]{disguise, disguise.getBlockState().getBlock()};
    }

    public void setDisguise(Block block) {
        if (this.disguise == null) return;
        BlockHuntBlock disguiseEntity = (BlockHuntBlock) this.disguise[0];
        disguiseEntity.setBlockState(block.getDefaultState());

        this.disguise = new Object[]{disguiseEntity, block};
    }

    public void setDisguise(BlockHuntBlock disguise, Block block) {
        this.disguise = new Object[]{disguise, block};
    }

    public void resetDisguise() {
        if (this.disguise[0] != null) ((BlockHuntBlock) this.disguise[0]).kill(world);
        this.disguise = new Object[]{null, null};
    }

    public BlockHuntBlock getDisguiseE() {
        return (BlockHuntBlock) this.disguise[0];
    }

    public Block getDisguiseB() {
        return (Block) this.disguise[1];
    }

    public void setHidden(boolean isHidden) {
        if (isHidden) positionHidden = player.getBlockPos();
        else positionHidden = null;

        this.isHidden = isHidden;
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public BlockPos getPositionHidden() {
        return this.positionHidden;
    }

    public void updateTimeBar(@Nullable Boolean moved) {
        if (this.bossBar == null) {
            this.bossBar = new BlockHuntBossBar.HideTimeBossbar();
            this.bossBar.addPlayer(this.player);
        } else {
            if (moved != null) this.bossBar.update(moved);
        }
    }

    public float getTimeUntilHidden() {
        if (this.bossBar == null) return 0;
        return this.bossBar.getTimeUntilHidden();
    }

    public void removeTimeBar() {
        if (bossBar != null) this.bossBar.remove();
    }
    public BlockHuntBossBar.HideTimeBossbar getTimeBar() {
        return this.bossBar;
    }


    public void loadPlayerInventory() {
        if (team == null || team.getName().equals("specs")) return;

        switch (team.getName()) {
            case "seekers" -> {
                this.player.clearStatusEffects();
                this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, StatusEffectInstance.INFINITE, 0, false, false));

                this.player.getInventory().clear();
                this.player.getInventory().setStack(0, new ItemStack(Items.IRON_SWORD));
                this.player.getInventory().armor.set(0, itemWName(Items.CHAINMAIL_BOOTS, Text.of("§f§lHeavy Boots")));
                this.player.getInventory().armor.set(1, itemWName(Items.CHAINMAIL_LEGGINGS, Text.of("§f§lHeavy Pants")));
                this.player.getInventory().armor.set(2, itemWName(Items.CHAINMAIL_CHESTPLATE, Text.of("§f§lHeavy Chestplate")));
                this.player.getInventory().armor.set(3, itemWName(Items.CHAINMAIL_HELMET, Text.of("§f§lHeavy Helmet")));
            }
            case "hiders" -> {
                this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, StatusEffectInstance.INFINITE, 2, false, false));
                this.player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, StatusEffectInstance.INFINITE, 0, false, false));


                this.player.getInventory().clear();
                this.player.getInventory().setStack(0, new ItemStack(Items.STONE_SWORD));
                this.player.getInventory().armor.set(0, itemWName(Items.LEATHER_BOOTS, Text.of("§f§lSneaky Boots")));
                this.player.getInventory().armor.set(1, itemWName(Items.LEATHER_LEGGINGS, Text.of("§f§lSneaky Pants")));
                this.player.getInventory().armor.set(2, itemWName(Items.LEATHER_CHESTPLATE, Text.of("§f§lSneaky Chestplate")));
                this.player.getInventory().armor.set(3, itemWName(Items.LEATHER_HELMET, Text.of("§f§lSneaky Helmet")));
            }
        }

        for (ItemStack item : this.player.getInventory().main) {
            item.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
            item.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        }

        for (ItemStack item : this.player.getInventory().armor) {
            item.set(DataComponentTypes.UNBREAKABLE, new UnbreakableComponent(true));
            item.set(DataComponentTypes.HIDE_TOOLTIP, Unit.INSTANCE);
        }
    }

    private ItemStack itemWName(Item stack, Text text) {
        ItemStack itemStack = new ItemStack(stack);
        itemStack.set(DataComponentTypes.ITEM_NAME, text);
        return itemStack;
    }

    public void playerDeath() {
        BlockHuntTitle.sendTitle(this.player,
                Text.literal("")
                .append(
                        Text.translatable("event.blockhunt.death")
                                .formatted(Formatting.RED)
                ),
                Text.literal("")
                        .append(
                        Text.translatable("event.blockhunt.death_time", 5)
                                .formatted(Formatting.GRAY)
                        ),
                0, 0, 0
        );
        respawnTicks = 100;
    }
}
