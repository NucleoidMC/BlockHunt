package com.github.voxxin.blockhunt.game;

import eu.pb4.sidebars.api.lines.SidebarLine;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import xyz.nucleoid.plasmid.api.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.api.game.common.widget.SidebarWidget;

import java.util.ArrayList;

public class BlockHuntSidebar {
    private final SidebarWidget sidebar;
    private static final ArrayList<SidebarLine> lines = new ArrayList<>();
    private final World world;
    private final BlockHuntStageManager stageManager;
    public BlockHuntSidebar(GlobalWidgets widgets, Identifier map_name, World world, BlockHuntStageManager stageManager) {
        this.sidebar = widgets.addSidebar(Text.translatable("gameType.blockhunt.standard").formatted(Formatting.GOLD));
        this.sidebar.setDefaultNumberFormat(BlankNumberFormat.INSTANCE);
        this.world = world;
        this.stageManager = stageManager;

        String mapName = map_name.toString().replaceAll("blockhunt:", "").replaceAll("_", " ");
        mapName = mapName.substring(0, 1).toUpperCase() + mapName.substring(1);

        long seekersDurationTick = stageManager.seekersRelease - stageManager.startTime;
        long totalSecondsR = seekersDurationTick / 20;
        String seekersRelease = String.format("%d:%02d", totalSecondsR / 60, totalSecondsR % 60);

        int hiderCount = world.getScoreboard().getTeam("hiders").getPlayerList().size();
        int seekerCount = world.getScoreboard().getTeam("seekers").getPlayerList().size();

        int totalGameSeconds = (stageManager.gameTime[0] / 20) + (stageManager.gameTime[1] / 20);
        String gameTime = String.format("%d:%02d", totalGameSeconds / 60, totalGameSeconds % 60);

        lines.clear();
        lines.add(SidebarLine.create(0, Text.literal("")));
        lines.add(SidebarLine.create(1, Text.literal("").append(Text.translatable("sidebar.blockhunt.map_name", Text.literal(mapName).formatted(Formatting.AQUA)))));
        lines.add(SidebarLine.create(2, Text.literal("")));
        lines.add(SidebarLine.create(3, Text.literal("").append(Text.translatable("sidebar.blockhunt.hider_count", Text.literal(String.valueOf(hiderCount)).formatted(Formatting.GREEN)))));
        lines.add(SidebarLine.create(4, Text.literal("")));
        lines.add(SidebarLine.create(5, Text.literal("").append(Text.translatable("sidebar.blockhunt.seeker_count", Text.literal(String.valueOf(seekerCount)).formatted(Formatting.GREEN)))));
        lines.add(SidebarLine.create(7, Text.literal("")));
        lines.add(SidebarLine.create(8, Text.literal("").append(Text.translatable("sidebar.blockhunt.game_time", Text.literal(gameTime).formatted(Formatting.GREEN)))));
        lines.add(SidebarLine.create(9, Text.literal("")));
        lines.add(SidebarLine.create(10, Text.literal("").append(Text.translatable("sidebar.blockhunt.seeker_countdown", Text.literal(seekersRelease).formatted(Formatting.GREEN)))));
        lines.add(SidebarLine.create(11, Text.literal("")));

        for (SidebarLine line : lines) {
            this.sidebar.setLine(line);
        }
    }

    public void tick() {
        long currentTick = this.world.getTime();
        long finishTick = stageManager.finishTime;
        long gameDurationTicks = finishTick - currentTick;
        long totalSeconds = gameDurationTicks / 20;
        int minutes = (int) (totalSeconds / 60);
        int seconds = (int) (totalSeconds % 60);
        String formattedGameTime = String.format("%d:%02d", minutes, seconds);

        this.world.getPlayers().forEach(playerEntity -> sidebar.addPlayer((ServerPlayerEntity) playerEntity));

        lines.set(3, SidebarLine.create(3, Text.literal("").append(Text.translatable("sidebar.blockhunt.hider_count", Text.literal(String.valueOf(this.world.getScoreboard().getTeam("hiders").getPlayerList().size())).formatted(Formatting.GREEN)))));
        lines.set(5, SidebarLine.create(5, Text.literal("").append(Text.translatable("sidebar.blockhunt.seeker_count", Text.literal(String.valueOf(this.world.getScoreboard().getTeam("seekers").getPlayerList().size())).formatted(Formatting.GREEN)))));
        lines.set(8, SidebarLine.create(8, Text.literal("").append(Text.translatable("sidebar.blockhunt.game_time", Text.literal(formattedGameTime).formatted(Formatting.GREEN)))));

        if (!stageManager.seekersReleased) {
            long seekersDurationTick = stageManager.seekersRelease - currentTick;
            long totalSecondsR = seekersDurationTick / 20;
            int minutesR = (int) (totalSecondsR / 60);
            int secondsR = (int) (totalSecondsR % 60);
            String formattedSeekerTime = String.format("%d:%02d", minutesR, secondsR);

            lines.set(lines.size() - 2, SidebarLine.create(10, Text.literal("").append(Text.translatable("sidebar.blockhunt.seeker_countdown", Text.literal(formattedSeekerTime).formatted(Formatting.GREEN)))));
            lines.set(lines.size() - 1, SidebarLine.create(11, Text.literal("")));
        } else if (lines.size() == 11) {
            world.getPlayers().forEach(player -> ((ServerPlayerEntity) player).playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f));
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
