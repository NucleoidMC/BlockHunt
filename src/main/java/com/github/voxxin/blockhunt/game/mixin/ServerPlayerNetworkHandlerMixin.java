package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.BlockHunt;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerCommonNetworkHandler.class)
public class ServerPlayerNetworkHandlerMixin {

    @Inject(at = @At("HEAD"), method = "send", cancellable = true)
    public void sendPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerPlayNetworkHandler handler && BlockHunt.shouldCancel(packet, handler)) {
            ci.cancel();
        }
    }

    @ModifyVariable(at = @At("HEAD"), method = "send", argsOnly = true)
    public Packet<?> sendPacket(Packet<?> packet) {
        //noinspection ConstantValue
        if (((Object) this) instanceof ServerPlayNetworkHandler handler && packet instanceof BundleS2CPacket bundleS2CPacket) {
            List<Packet<? super ClientPlayPacketListener>> packets = new ArrayList<>();
            boolean modified = false;

            for (Packet<? super ClientPlayPacketListener> child : bundleS2CPacket.getPackets()) {
                if (BlockHunt.shouldCancel(child, handler)) {
                    modified = true;
                } else {
                    packets.add(child);
                }
            }

            if (modified) {
                return new BundleS2CPacket(packets);
            }
        }

        return packet;
    }
}
