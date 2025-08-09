package com.github.voxxin.blockhunt;

import com.github.voxxin.blockhunt.game.BlockHuntConfig;
import com.github.voxxin.blockhunt.game.BlockHuntWaiting;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityEquipmentUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xyz.nucleoid.plasmid.api.game.GameType;

import java.util.ArrayList;
import java.util.List;

public class BlockHunt implements ModInitializer {

    public static final String ID = "blockhunt";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final GameType<BlockHuntConfig> TYPE = GameType.register(
            Identifier.of(ID, "blockhunt"),
            BlockHuntConfig.CODEC,
            BlockHuntWaiting::open
    );
    @Override
    public void onInitialize() {
    }

    public static Identifier id(String value) {
        return Identifier.of(ID, value);
    }
    public static List<Integer> deniedIDs = new ArrayList<>();

    public static boolean shouldCancel(Packet<?> packet, ServerPlayNetworkHandler handler) {
        if (packet instanceof EntityEquipmentUpdateS2CPacket entityEquipmentUpdateS2CPacket) {
            int packetID = entityEquipmentUpdateS2CPacket.getEntityId();
            return handler.player.getWorld().getPlayers().stream().noneMatch(p -> p.getId() == packetID) || BlockHunt.deniedIDs.contains(packetID);
        }

        return false;
    }
}
