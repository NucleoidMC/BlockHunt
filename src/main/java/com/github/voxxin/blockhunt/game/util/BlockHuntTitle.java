package com.github.voxxin.blockhunt.game.util;

import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class BlockHuntTitle {
    public static void sendTitle(ServerPlayer player, Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (fadeInTicks == 0) fadeOutTicks = 2 * 20;
        if (stayTicks == 0) stayTicks = 3 * 20;
        if (fadeOutTicks == 0) fadeOutTicks = 2 * 20;

        ServerGamePacketListenerImpl networkHandler = player.connection;
        networkHandler.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
        networkHandler.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}
