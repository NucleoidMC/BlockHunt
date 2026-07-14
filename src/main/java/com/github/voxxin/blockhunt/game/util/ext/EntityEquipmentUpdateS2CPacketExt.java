package com.github.voxxin.blockhunt.game.util.ext;

import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.Level;

public interface EntityEquipmentUpdateS2CPacketExt {
    void blockHunt$apply(ClientGamePacketListener clientPlayPacketListener, Level world);
}
