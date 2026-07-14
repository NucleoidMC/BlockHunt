package com.github.voxxin.blockhunt.game;

import eu.pb4.sidebars.api.lines.SidebarLine;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.ArrayList;

public class BlockHuntSidebar {
    private final SidebarWidget sidebar;
    private static final ArrayList<SidebarLine> lines = new ArrayList<>();
    private final Level level;
    private final BlockHuntStageManager stageManager;
    public BlockHuntSidebar(GlobalWidgets widgets, Identifier map_name, Level level, BlockHuntStageManager stageManager) {
        this.sidebar = widgets.addSidebar(Component.translatable("gameType.blockhunt.standard").withStyle(ChatFormatting.GOLD));
        this.sidebar.setDefaultNumberFormat(BlankFormat.INSTANCE);
        this.level = level;
        this.stageManager = stageManager;

        String mapName = map_name.toString().replaceAll("blockhunt:", "").replaceAll("_", " ");
        mapName = mapName.substring(0, 1).toUpperCase() + mapName.substring(1);

        long seekersDurationTick = stageManager.seekersRelease - stageManager.startTime;
        long totalSecondsR = seekersDurationTick / 20;
        String seekersRelease = String.format("%d:%02d", totalSecondsR / 60, totalSecondsR % 60);

        int hiderCount = level.getScoreboard().getPlayerTeam("hiders").getPlayers().size();
        int seekerCount = level.getScoreboard().getPlayerTeam("seekers").getPlayers().size();

        int totalGameSeconds = (stageManager.gameTime[0] / 20) + (stageManager.gameTime[1] / 20);
        String gameTime = String.format("%d:%02d", totalGameSeconds / 60, totalGameSeconds % 60);

        lines.clear();
        lines.add(SidebarLine.create(0, Component.literal("")));
        lines.add(SidebarLine.create(1, Component.literal("").append(Component.translatable("sidebar.blockhunt.map_name", Component.literal(mapName).withStyle(ChatFormatting.AQUA)))));
        lines.add(SidebarLine.create(2, Component.literal("")));
        lines.add(SidebarLine.create(3, Component.literal("").append(Component.translatable("sidebar.blockhunt.hider_count", Component.literal(String.valueOf(hiderCount)).withStyle(ChatFormatting.GREEN)))));
        lines.add(SidebarLine.create(4, Component.literal("")));
        lines.add(SidebarLine.create(5, Component.literal("").append(Component.translatable("sidebar.blockhunt.seeker_count", Component.literal(String.valueOf(seekerCount)).withStyle(ChatFormatting.GREEN)))));
        lines.add(SidebarLine.create(7, Component.literal("")));
        lines.add(SidebarLine.create(8, Component.literal("").append(Component.translatable("sidebar.blockhunt.game_time", Component.literal(gameTime).withStyle(ChatFormatting.GREEN)))));
        lines.add(SidebarLine.create(9, Component.literal("")));
        lines.add(SidebarLine.create(10, Component.literal("").append(Component.translatable("sidebar.blockhunt.seeker_countdown", Component.literal(seekersRelease).withStyle(ChatFormatting.GREEN)))));
        lines.add(SidebarLine.create(11, Component.literal("")));

        for (SidebarLine line : lines) {
            this.sidebar.setLine(line);
        }
    }

    public void tick() {
        long currentTick = this.level.getGameTime();
        long finishTick = stageManager.finishTime;
        long gameDurationTicks = finishTick - currentTick;
        long totalSeconds = gameDurationTicks / 20;
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        String formattedGameTime = String.format("%d:%02d", minutes, seconds);

        this.level.players().forEach(playerEntity -> sidebar.addPlayer((ServerPlayer) playerEntity));

        lines.set(3, SidebarLine.create(3, Component.literal("").append(Component.translatable("sidebar.blockhunt.hider_count", Component.literal(String.valueOf(this.level.getScoreboard().getPlayerTeam("hiders").getPlayers().size())).withStyle(ChatFormatting.GREEN)))));
        lines.set(5, SidebarLine.create(5, Component.literal("").append(Component.translatable("sidebar.blockhunt.seeker_count", Component.literal(String.valueOf(this.level.getScoreboard().getPlayerTeam("seekers").getPlayers().size())).withStyle(ChatFormatting.GREEN)))));
        lines.set(8, SidebarLine.create(8, Component.literal("").append(Component.translatable("sidebar.blockhunt.game_time", Component.literal(formattedGameTime).withStyle(ChatFormatting.GREEN)))));

        if (!stageManager.seekersReleased) {
            long seekersDurationTick = stageManager.seekersRelease - currentTick;
            long totalSecondsR = seekersDurationTick / 20;
            int minutesR = (int) (totalSecondsR / 60);
            int secondsR = (int) (totalSecondsR % 60);
            String formattedSeekerTime = String.format("%d:%02d", minutesR, secondsR);

            lines.set(lines.size() - 2, SidebarLine.create(10, Component.literal("").append(Component.translatable("sidebar.blockhunt.seeker_countdown", Component.literal(formattedSeekerTime).withStyle(ChatFormatting.GREEN)))));
            lines.set(lines.size() - 1, SidebarLine.create(11, Component.literal("")));
        } else if (lines.size() == 11) {
            level.players().forEach(player -> ((ServerPlayer) player).playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0f, 1.0f));
            this.sidebar.removeLine(lines.size());
            lines.removeLast();
            this.sidebar.removeLine(lines.size());
            lines.removeLast();
        }

        for (SidebarLine line : lines) {
            this.sidebar.setLine(line);
        }
    }
}
