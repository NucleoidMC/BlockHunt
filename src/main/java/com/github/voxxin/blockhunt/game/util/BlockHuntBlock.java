package com.github.voxxin.blockhunt.game.util;

import com.github.voxxin.blockhunt.game.util.ext.DisplayEntityExt;
import net.minecraft.world.entity.Display.BlockDisplay;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mutable;

public class BlockHuntBlock extends Display.BlockDisplay {

    BlockDisplay blockDisplayEntity = this;

    public BlockHuntBlock(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setPosRotInterpolationDuration(1);
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
