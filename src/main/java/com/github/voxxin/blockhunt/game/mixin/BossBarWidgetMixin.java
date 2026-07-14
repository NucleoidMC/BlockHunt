package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.BossBarWidgetExt;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerBossEvent.class)
public class BossBarWidgetMixin implements BossBarWidgetExt {

    @Override
    public void blockHunt$addSinglePlayer(ServerPlayer player) {
        ServerBossEvent bar = (ServerBossEvent) (Object) this;
        bar.removeAllPlayers();
        bar.addPlayer(player);
    }
}
