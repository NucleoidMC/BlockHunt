package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.BlockHuntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.BlockDisplay.class)
public abstract class BlockDisplayEntityMixin {


    @Shadow @Nullable public abstract Display.BlockDisplay.BlockRenderState blockRenderState();

    @Shadow @Nullable private Display.BlockDisplay.BlockRenderState blockRenderState;

    @Inject(at = @At("HEAD"), method = "getBlockState", cancellable = true)
    public void blockHunt$getBlockState(CallbackInfoReturnable<BlockState> cir) {
        if (((Object) this) instanceof BlockHuntBlock) {
            Display.BlockDisplay.BlockRenderState data = ((Display.BlockDisplay.BlockRenderState) this.blockRenderState());
            cir.setReturnValue(this.blockRenderState != null && data.blockState() != null ? this.blockRenderState.blockState() : Blocks.AIR.defaultBlockState());
        }
    }
}
