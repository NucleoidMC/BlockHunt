package com.github.voxxin.blockhunt.game.map;

import com.github.voxxin.blockhunt.BlockHunt;
import com.github.voxxin.blockhunt.game.BlockHuntConfig;
import com.github.voxxin.blockhunt.game.util.BlockHuntAnimation;
import com.github.voxxin.blockhunt.game.util.BlockHuntAnimationPoints;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.gamerules.GameRules;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.level.generator.TemplateChunkGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public record BlockHuntMap(Map<String, Vec3> spawns, ArrayList<Object> noInteractList,
                           ArrayList<Object> allowedDisguises, ArrayList<BlockHuntAnimation> animations, RuntimeLevelConfig levelConfig) {
    public Vec3 getSpawnPos() {
        return this.spawns().getOrDefault("spawn_everyone", this.spawns().get("spawn_hider"));
    }
    
    public static BlockHuntMap from(GameOpenContext<BlockHuntConfig> context) throws IOException {
        var server = context.server();
        var config = context.config();

        var template = MapTemplateSerializer.loadFromResource(server, config.mapConfig().id());
        var metadata = template.getMetadata();

        var levelConfig = new RuntimeLevelConfig().setGenerator(new TemplateChunkGenerator(server, template))
                .setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0)
                .setGameRule(GameRules.FIRE_DAMAGE, false)
                .setGameRule(GameRules.FREEZE_DAMAGE, false)
                .setGameRule(GameRules.MOB_GRIEFING, false)
                .setGameRule(GameRules.SPAWN_MOBS, false)
                .setGameRule(GameRules.RANDOM_TICK_SPEED, 0)
                .setGameRule(GameRules.WATER_SOURCE_CONVERSION, false)
                .setGameRule(GameRules.LAVA_SOURCE_CONVERSION, false)
                .setGameRule(GameRules.ADVANCE_WEATHER, false)
                .setGameRule(GameRules.ADVANCE_TIME, false)
                .setGameRule(GameRules.SHOW_DEATH_MESSAGES, false)
                .setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false)
                .setDifficulty(Difficulty.HARD);

        var noInteractList = new ArrayList<>();
        var allowedDisguises = new ArrayList<>();
        ArrayList<BlockHuntAnimation> animations = new ArrayList<>();
        Map<String, Vec3> spawns = new HashMap<>();


        metadata.getRegions().forEach((region) -> {
            switch (region.getMarker()) {
                case "spawn_hider" -> {
                    System.out.println("spawn_hider");
                    BlockPos min = region.getBounds().min();
                    BlockPos max = region.getBounds().max();
                    int lowestY = Math.min(max.getY(), min.getY());

                    Vec3 tempPos = region.getBounds().center();
                    spawns.put("spawn_hider", new Vec3(tempPos.x(), lowestY, tempPos.z()));
                }
                case "spawn_seeker" -> {
                    System.out.println("spawn_seeker");
                    BlockPos min = region.getBounds().min();
                    BlockPos max = region.getBounds().max();
                    int lowestY = Math.min(max.getY(), min.getY());

                    Vec3 tempPos = region.getBounds().center();
                    spawns.put("spawn_seeker", new Vec3(tempPos.x(), lowestY, tempPos.z()));
                }
                case "spawn_everyone" -> {
                    System.out.println("spawn_everyone");
                    BlockPos min = region.getBounds().min();
                    BlockPos max = region.getBounds().max();
                    int lowestY = Math.min(max.getY(), min.getY());

                    Vec3 tempPos = region.getBounds().center();
                    spawns.put("spawn_everyone", new Vec3(tempPos.x(), lowestY, tempPos.z()));
                }
                case "map_no_interact" -> {
                    BlockPos min = region.getBounds().min();
                    BlockPos max = region.getBounds().max();

                    int lowestY = Math.min(max.getY(), min.getY());

                    for (int x = min.getX(); x <= max.getX(); x++) {
                        for (int z = min.getZ(); z <= max.getZ(); z++) {
                            noInteractList.add(new BlockPos(x, lowestY, z));
                        }
                    }
                }
                case "map_disguises" -> {
                    BlockPos min = region.getBounds().min();
                    int minY = min.getY();

                    BlockPos max = region.getBounds().max();
                    for (int x = min.getX(); x <= max.getX(); x++) {
                        for (int z = min.getZ(); z <= max.getZ(); z++) {
                            allowedDisguises.add(new BlockPos(x, minY, z));
                        }
                    }
                }
            }

            String animationPoint = region.getMarker().contains("animation_") ? region.getMarker() : null;
            if (animationPoint != null) {
                animationPoint = animationPoint.replace("animation_", "");
                String[] animationParts;
                animationParts = animationPoint.split("_");
                if (animationParts.length != 2) {
                    return;
                }

                BlockHuntAnimation newAnimation =
                        new BlockHuntAnimation(Identifier.fromNamespaceAndPath(config.mapConfig().id().getNamespace(), animationParts[0]),
                        animationParts[1].contains("play") ?
                                new BlockHuntAnimationPoints(region.getBounds().min(), region.getBounds().max(), true) : null,
                        animationParts[1].contains("settings") ? region.getBounds().max() : null);

                for (BlockHuntAnimation animation : animations) {
                    if (animation.animationName.getPath().equals(animationParts[0])) {
                        if (animationParts[1].contains("play")) animation.setAnimationPlayPoint(new BlockHuntAnimationPoints(region.getBounds().min(), region.getBounds().max(), true));
                        if (animationParts[1].contains("settings")) animation.setSettingsPoint(region.getBounds().max());
                        if (animationParts[1].contains("f")) {
                            try {
                                int frame = Integer.parseInt(animationParts[1].replace("f", ""));
                                BlockHuntAnimationPoints point = new BlockHuntAnimationPoints(region.getBounds().min(), region.getBounds().max(), false);
                                animation.setFrame(point, frame);
                            } catch (Exception e) {
                                BlockHunt.LOGGER.error("Error while parsing animation frame" + "\n" + e.getMessage());
                            }
                        }
                    }
                }

                String[] finalAnimationParts = animationParts;
                if (animations.stream().noneMatch((animation) -> animation.animationName.getPath().equals(finalAnimationParts[0]))) {
                    if (animationParts[1].contains("play")) newAnimation.setAnimationPlayPoint(new BlockHuntAnimationPoints(region.getBounds().min(), region.getBounds().max(), true));
                    if (animationParts[1].contains("settings")) newAnimation.setSettingsPoint(region.getBounds().max());
                    if (animationParts[1].contains("f")) {
                        try {
                            int frame = Integer.parseInt(animationParts[1].replace("f", ""));
                            BlockHuntAnimationPoints point = new BlockHuntAnimationPoints(region.getBounds().min(), region.getBounds().max(), false);
                            newAnimation.setFrame(point, frame);
                        } catch (Exception e) {
                            BlockHunt.LOGGER.error("Error while parsing animation frame" + "\n" + e.getMessage());
                        }
                    }

                    animations.add(newAnimation);
                }
            }
        });

        return new BlockHuntMap(spawns, noInteractList, allowedDisguises, animations, levelConfig);
    }
}
