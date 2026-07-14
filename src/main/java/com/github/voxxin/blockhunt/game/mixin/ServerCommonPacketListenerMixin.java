package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.BlockHunt;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerCommonPacketListenerImpl.class)
public class ServerCommonPacketListenerMixin {

    @Inject(at = @At("HEAD"), method = "send*", cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerGamePacketListenerImpl handler && BlockHunt.shouldCancel(packet, handler)) {
            ci.cancel();
        }
    }

    @ModifyVariable(at = @At("HEAD"), method = "send*", argsOnly = true)
    public Packet<?> sendPacket(Packet<?> packet) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerGamePacketListenerImpl handler && packet instanceof ClientboundBundlePacket bundleS2CPacket) {
            List<Packet<? super ClientGamePacketListener>> packets = new ArrayList<>();
            boolean modified = false;

            for (Packet<? super ClientGamePacketListener> child : bundleS2CPacket.subPackets()) {
                if (BlockHunt.shouldCancel(child, handler)) {
                    modified = true;
                } else {
                    packets.add(child);
                }
            }

            if (modified) {
                return new ClientboundBundlePacket(packets);
            }
        }

        return packet;
    }
}
