package com.github.voxxin.blockhunt;

import com.github.voxxin.blockhunt.game.BlockHuntConfig;
import com.github.voxxin.blockhunt.game.BlockHuntWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;
import xyz.nucleoid.plasmid.api.game.GameTypes;

import java.util.ArrayList;
import java.util.List;

public class BlockHunt implements ModInitializer {

    public static final String ID = "blockhunt";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BlockHuntConfig> TYPE = GameTypes.register(
            Identifier.fromNamespaceAndPath(ID, "blockhunt"),
            BlockHuntConfig.CODEC,
            BlockHuntWaiting::open
    );
    @Override
    public void onInitialize() {
    }

    public static Identifier id(String value) {
        return Identifier.fromNamespaceAndPath(ID, value);
    }
    public static List<Integer> deniedIDs = new ArrayList<>();

    public static boolean shouldCancel(Packet<?> packet, ServerGamePacketListenerImpl handler) {
        if (packet instanceof ClientboundSetEquipmentPacket entityEquipmentUpdateS2CPacket) {
            int packetID = entityEquipmentUpdateS2CPacket.getEntity();
            return handler.player.level().players().stream().noneMatch(p -> p.getId() == packetID) || BlockHunt.deniedIDs.contains(packetID);
        }

        return false;
    }
}
