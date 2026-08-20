package com.github.voxxin.blockhunt.game.util;

import com.github.voxxin.blockhunt.game.util.ext.BossBarWidgetExt;
import net.minecraft.world.BossEvent;
import net.minecraft.server.bossevents.CustomBossEvent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public class BlockHuntBossBar extends CustomBossEvent {
    private final ServerBossEvent bossBar;

    public BlockHuntBossBar(Component title) {
        UUID uuid = UUID.randomUUID();
        super(uuid, Identifier.fromNamespaceAndPath("blockhunt", String.valueOf(title)), title, () -> {});
        Component bossbarTitle = Component.literal("")
                .append(title);

        this.bossBar = new ServerBossEvent(uuid, bossbarTitle, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);

    }

    public static class HideTimeBossbar extends CustomBossEvent {
        private final ServerBossEvent widget;
        private float timeUntilHidden = 1F;
        private int timeToHide;
        private boolean hidden = false;

        public HideTimeBossbar(int timeToHide) {
            UUID uuid = UUID.randomUUID();
            super(uuid, Identifier.fromNamespaceAndPath("blockhunt", "blockhunt.bossbar.not_hidden"), Component.translatable("blockhunt.bossbar.not_hidden"), () -> {});
            this.timeToHide = timeToHide;
            Component bossbarTitle = Component.literal("")
                    .append(Component.translatable("bossbar.blockhunt.not_hidden",
                                            Component.literal(String.valueOf(this.timeUntilHidden)).withStyle(ChatFormatting.YELLOW)
                                    )
                                    .withStyle(ChatFormatting.WHITE)
                    );
            this.widget = new ServerBossEvent(uuid, bossbarTitle, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_20);
        }

        public void addPlayer(ServerPlayer player) {
            ((BossBarWidgetExt) widget).blockHunt$addSinglePlayer(player);
        }

        public void update(boolean moved) {
            if (moved) {
                this.timeUntilHidden = 1F;
            } else if (timeUntilHidden > 0) {
                this.timeUntilHidden -= (20F / timeToHide);
            }

            if (this.timeUntilHidden > 0F) {
                this.widget.setColor(BossEvent.BossBarColor.RED);
                this.widget.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
                this.widget.setProgress(this.timeUntilHidden);

                Component newTitle = Component.literal("")
                        .append(Component.translatable("bossbar.blockhunt.not_hidden",
                                                Component.literal(String.valueOf(this.timeUntilHidden)).withStyle(ChatFormatting.YELLOW)
                                        )
                                        .withStyle(ChatFormatting.WHITE)
                        );

                hidden = false;
                this.widget.setName(newTitle);
            } else {
                this.widget.setProgress(1.0F);
                this.widget.setColor(BossEvent.BossBarColor.PINK);
                this.widget.setOverlay(BossEvent.BossBarOverlay.PROGRESS);
                this.widget.setName(Component.translatable("bossbar.blockhunt.hidden"));
                hidden = true;
            }
        }

        public float getTimeUntilHidden() {
            if (hidden) return 0F;
            return this.timeUntilHidden;
        }

        public void remove() {
            this.widget.setVisible(false);
        }
    }
}
