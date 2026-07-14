package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.DisplayEntityExt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Display.BlockDisplay.class)
public abstract class DisplayEntityMixin implements DisplayEntityExt {


    @Shadow protected abstract void setBlockState(BlockState state);

    public void blockHunt$setState(BlockState blockState) {
        this.setBlockState(blockState);
    }
}
