package com.github.voxxin.blockhunt.game.util;

import com.github.voxxin.blockhunt.game.util.ext.DisplayEntityExt;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mutable;

public class BlockHuntBlock extends DisplayEntity.BlockDisplayEntity {

    BlockDisplayEntity blockDisplayEntity = this;

    public BlockHuntBlock(EntityType<?> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setTeleportDuration(1);
    }

//    public void setBlockState(BlockState state) {
//        ((DisplayEntityExt) this).blockHunt$setState(state);
//    }
//
//    public BlockState getBlockState() {
//        Data data = this.getData();
//        if (data != null && data.blockState() != null) {
//            return data.blockState();
//        } else {
//            return Blocks.AIR.getDefaultState();
//        }
//    }

}
