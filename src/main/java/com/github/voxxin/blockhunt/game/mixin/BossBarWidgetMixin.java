package com.github.voxxin.blockhunt.game.mixin;

import com.github.voxxin.blockhunt.game.util.ext.BossBarWidgetExt;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerBossBar.class)
public class BossBarWidgetMixin implements BossBarWidgetExt {

    @Override
    public void blockHunt$addSinglePlayer(ServerPlayerEntity player) {
        ServerBossBar bar = (ServerBossBar) (Object) this;
        bar.clearPlayers();
        bar.addPlayer(player);
    }
}
