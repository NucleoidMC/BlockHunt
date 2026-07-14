package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.WorldExt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Level.class)
public abstract class LevelMixin implements WorldExt {

    @Shadow public abstract boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth);

    @Override
    public void blockHunt$setBlockState(int x, int y, int z, BlockState blockState) {
        this.setBlock(new BlockPos(x,y,z), blockState, 3, 0);
    }

    @Override
    public void blockHunt$setBlockState(BlockPos blockPos, BlockState blockState) {
        this.setBlock(blockPos, blockState, 3, 0);
    }

}
