package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.BlockHuntBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.DisplayEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DisplayEntity.BlockDisplayEntity.class)
public abstract class BlockDisplayEntityMixin {


    @Shadow @Nullable public abstract DisplayEntity.BlockDisplayEntity.Data getData();

    @Shadow @Nullable private DisplayEntity.BlockDisplayEntity.Data data;

    @Inject(at = @At("HEAD"), method = "getBlockState", cancellable = true)
    public void blockHunt$getBlockState(CallbackInfoReturnable<BlockState> cir) {
        if (((Object) this) instanceof BlockHuntBlock) {
            DisplayEntity.BlockDisplayEntity.Data data = ((DisplayEntity.BlockDisplayEntity.Data) this.getData());
            cir.setReturnValue(this.data != null && data.blockState() != null ? this.data.blockState() : Blocks.AIR.getDefaultState());
        }
    }
}
