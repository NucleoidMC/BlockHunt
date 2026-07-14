package com.github.voxxin.blockhunt.game;

import com.github.voxxin.blockhunt.game.util.BlockHuntBlock;
import com.github.voxxin.blockhunt.game.util.BlockHuntBossBar;
import com.github.voxxin.blockhunt.game.util.BlockHuntTitle;
import it.unimi.dsi.fastutil.objects.ReferenceSortedSets;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.plasmid.api.util.PlayerRef;

public class BlockHuntPlayer {
    private final ServerLevel level;
    private final PlayerRef playerRef;
    private final ServerPlayer player;
    private BlockHuntBossBar.HideTimeBossbar bossBar;
    private PlayerTeam team = null;
    private Object[] disguise = new Object[2];
    private boolean isHidden;
    public Vec3 lastPosition;
    public int respawnTicks = 0;
    private BlockPos positionHidden = null;
    public BlockPos prevBlockhitResult;

    public int lastRealSecond = 0;

    public BlockHuntPlayer(ServerLevel level, PlayerRef player) {
        this.level = level;
        this.playerRef = player;
        this.player = player.getEntity(level);
    }

    public void setTeam(PlayerTeam team) {
        level.getScoreboard().addPlayerToTeam(player.getScoreboardName(), team);
        this.team = team;
    }

    public PlayerTeam getTeam() {
        return this.team;
    }

    public void setDisguise(BlockHuntBlock disguise) {
        if (disguise == null) return;

        this.disguise = new Object[]{disguise, disguise.getBlockState().getBlock()};
    }

    public void setDisguise(Block block) {
        if (this.disguise == null) return;
        BlockHuntBlock disguiseEntity = (BlockHuntBlock) this.disguise[0];
        disguiseEntity.setBlockState(block.defaultBlockState());

        this.disguise = new Object[]{disguiseEntity, block};
    }

    public void setDisguise(BlockHuntBlock disguise, Block block) {
        this.disguise = new Object[]{disguise, block};
    }

    public void resetDisguise() {
        if (this.disguise[0] != null) ((BlockHuntBlock) this.disguise[0]).kill(level);
        this.disguise = new Object[]{null, null};
    }

    public BlockHuntBlock getDisguiseE() {
        return (BlockHuntBlock) this.disguise[0];
    }

    public Block getDisguiseB() {
        return (Block) this.disguise[1];
    }

    public void setHidden(boolean isHidden) {
        if (isHidden) positionHidden = player.blockPosition();
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
                this.player.removeAllEffects();
                this.player.addEffect(new MobEffectInstance(MobEffects.SPEED, MobEffectInstance.INFINITE_DURATION, 0, false, false));

                this.player.getInventory().clearContent();
                this.player.getInventory().setItem(0, new ItemStack(Items.IRON_SWORD));
                this.player.setItemSlot(EquipmentSlot.FEET, itemWName(Items.CHAINMAIL_BOOTS, Component.nullToEmpty("§f§lHeavy Boots")));
                this.player.setItemSlot(EquipmentSlot.LEGS, itemWName(Items.CHAINMAIL_LEGGINGS, Component.nullToEmpty("§f§lHeavy Pants")));
                this.player.setItemSlot(EquipmentSlot.CHEST, itemWName(Items.CHAINMAIL_CHESTPLATE, Component.nullToEmpty("§f§lHeavy Chestplate")));
                this.player.setItemSlot(EquipmentSlot.HEAD, itemWName(Items.CHAINMAIL_HELMET, Component.nullToEmpty("§f§lHeavy Helmet")));
            }
            case "hiders" -> {
                this.player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, MobEffectInstance.INFINITE_DURATION, 2, false, false));
                this.player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, MobEffectInstance.INFINITE_DURATION, 0, false, false));


                this.player.getInventory().clearContent();
                this.player.getInventory().setItem(0, new ItemStack(Items.STONE_SWORD));
                this.player.setItemSlot(EquipmentSlot.FEET, itemWName(Items.LEATHER_BOOTS, Component.nullToEmpty("§f§lSneaky Boots")));
                this.player.setItemSlot(EquipmentSlot.LEGS, itemWName(Items.LEATHER_LEGGINGS, Component.nullToEmpty("§f§lSneaky Pants")));
                this.player.setItemSlot(EquipmentSlot.CHEST, itemWName(Items.LEATHER_CHESTPLATE, Component.nullToEmpty("§f§lSneaky Chestplate")));
                this.player.setItemSlot(EquipmentSlot.HEAD, itemWName(Items.LEATHER_HELMET, Component.nullToEmpty("§f§lSneaky Helmet")));
            }
        }

        for (ItemStack item : this.player.getInventory().getNonEquipmentItems()) {
            item.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            item.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        }

        for (var slot : EquipmentSlot.values()) {
            var item = this.player.getItemBySlot(slot);
            item.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
            item.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ReferenceSortedSets.emptySet()));
        }
    }

    private ItemStack itemWName(Item stack, Component text) {
        ItemStack itemStack = new ItemStack(stack);
        itemStack.set(DataComponents.ITEM_NAME, text);
        return itemStack;
    }

    public void playerDeath() {
        BlockHuntTitle.sendTitle(this.player,
                Component.literal("")
                .append(
                        Component.translatable("event.blockhunt.death")
                                .withStyle(ChatFormatting.RED)
                ),
                Component.literal("")
                        .append(
                        Component.translatable("event.blockhunt.death_time", 5)
                                .withStyle(ChatFormatting.GRAY)
                        ),
                0, 0, 0
        );
        respawnTicks = 100;
    }
}
