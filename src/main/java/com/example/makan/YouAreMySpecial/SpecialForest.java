package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpecialForest {

    private enum BiomeType {
        FOREST, TAIGA, JUNGLE, PLAINS, SWAMP, DESERT
    }

    private record BiomeProfile(List<Block> saplings, List<Block> vegetation) {}

    private static final Map<BiomeType, BiomeProfile> BIOME_PROFILES = Map.of(
        BiomeType.FOREST, new BiomeProfile(
            List.of(Blocks.OAK_SAPLING, Blocks.BIRCH_SAPLING),
            List.of(Blocks.GRASS, Blocks.FERN, Blocks.DANDELION, Blocks.POPPY)
        ),
        BiomeType.TAIGA, new BiomeProfile(
            List.of(Blocks.SPRUCE_SAPLING),
            List.of(Blocks.FERN, Blocks.GRASS, Blocks.SWEET_BERRY_BUSH)
        ),
        BiomeType.JUNGLE, new BiomeProfile(
            List.of(Blocks.JUNGLE_SAPLING, Blocks.BAMBOO_SAPLING),
            List.of(Blocks.MELON, Blocks.COCOA, Blocks.VINE)
        ),
        BiomeType.PLAINS, new BiomeProfile(
            List.of(Blocks.OAK_SAPLING),
            List.of(Blocks.GRASS, Blocks.TALL_GRASS, Blocks.DANDELION, Blocks.POPPY)
        ),
        BiomeType.SWAMP, new BiomeProfile(
            List.of(Blocks.OAK_SAPLING),
            List.of(Blocks.BLUE_ORCHID, Blocks.BROWN_MUSHROOM, Blocks.RED_MUSHROOM)
        ),
        BiomeType.DESERT, new BiomeProfile(
            List.of(), // no trees
            List.of(Blocks.CACTUS, Blocks.DEAD_BUSH, Blocks.SUGAR_CANE)
        )
    );

    public static void generateForest(ServerPlayer player, int radius, double density) {
        ServerLevel level = player.serverLevel();
        if (level == null) {
            player.sendSystemMessage(Component.literal("❌ Server not found."));
            return;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 endVec = eyePos.add(lookDir.scale(512));

        HitResult result = level.clip(new ClipContext(
                eyePos, endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (result.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("❌ No valid block targeted."));
            return;
        }

        BlockPos center = BlockPos.containing(result.getLocation());
        Random rand = new Random();

        // Pick biome type at random
        BiomeType biome = BiomeType.values()[rand.nextInt(BiomeType.values().length)];
        BiomeProfile profile = BIOME_PROFILES.get(biome);

        player.sendSystemMessage(Component.literal("🌱 Generating " + biome + " forest at " + center));

        int planted = 0;
        int vegetation = 0;

        // scatter placements
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) continue;

                if (rand.nextDouble() > density) continue; // better density scaling

                BlockPos pos = center.offset(dx, 0, dz);
                BlockPos ground = findGround(level, pos);
                if (ground == null) continue;

                BlockPos placePos = ground.above();

                // Trees
                if (!profile.saplings.isEmpty() && rand.nextDouble() < 0.6) {
                    Block saplingBlock = profile.saplings.get(rand.nextInt(profile.saplings.size()));
                    BlockState saplingState = saplingBlock.defaultBlockState();
                    level.setBlock(placePos, saplingState, 2);

                    if (saplingBlock instanceof SaplingBlock sb) {
                        // Boost growth by calling multiple times
                        for (int i = 0; i < 5; i++) {
                            sb.advanceTree(level, placePos, saplingState, RandomSource.create());
                        }
                    }

                    planted++;
                } 
                // Vegetation
                else if (!profile.vegetation.isEmpty()) {
                    Block veg = profile.vegetation.get(rand.nextInt(profile.vegetation.size()));
                    BlockState vegState = veg.defaultBlockState();
                    if (vegState.canSurvive(level, placePos)) {
                        level.setBlock(placePos, vegState, 2);
                        vegetation++;
                    }
                }
            }
        }

        player.sendSystemMessage(Component.literal(
            "✅ Planted " + planted + " trees and " + vegetation + " plants (" + biome + " biome)."
        ));
    }

    private static BlockPos findGround(ServerLevel level, BlockPos start) {
        for (int y = start.getY(); y > level.getMinBuildHeight(); y--) {
            BlockPos pos = new BlockPos(start.getX(), y, start.getZ());
            if (!level.getBlockState(pos).isAir()) return pos;
        }
        return null;
    }
}
