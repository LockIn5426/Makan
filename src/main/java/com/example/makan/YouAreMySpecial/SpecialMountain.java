package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import com.example.makan.YouAreMySpecial.SpecialCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecialMountain {

    public static void generateMountainWithAnimation(ServerPlayer player, int radius, int peakHeight) {
    MinecraftServer server = player.getServer();
    if (server == null) {
        player.sendSystemMessage(Component.literal("❌ Server not found."));
        return;
    }

    ServerLevel level = player.serverLevel();
    Vec3 eyePos = player.getEyePosition(1.0F);
    Vec3 lookDir = player.getLookAngle().normalize();
    double maxDistance = 512;
    Vec3 endVec = eyePos.add(lookDir.scale(maxDistance));

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

    Vec3 hitVec = result.getLocation();
    BlockPos center = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);

    player.sendSystemMessage(Component.literal("⛰️ Animating mountain generation at: " + center));

    long seed = System.currentTimeMillis();
    Map<Integer, List<BlockPlacement>> layers = new TreeMap<>();

    for (int dx = -radius; dx <= radius; dx++) {
        for (int dz = -radius; dz <= radius; dz++) {
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq > radius * radius) continue;

            double falloff = 1.0 - (Math.sqrt(distanceSq) / radius);
            double noise = (OpenSimplex2.noise2(seed, (center.getX() + dx) * 0.05, (center.getZ() + dz) * 0.05) + 1) / 2.0;
            int columnHeight = (int) ((falloff * 0.8 + 0.2) * noise * peakHeight);

            for (int dy = 0; dy <= columnHeight; dy++) {
                BlockPos pos = center.offset(dx, dy, dz);

                BlockState block;
                if (dy == columnHeight) {
                    block = (center.getY() + dy >= 120) ? Blocks.SNOW_BLOCK.defaultBlockState() : Blocks.GRASS_BLOCK.defaultBlockState();
                } else if (dy >= columnHeight - 3) {
                    block = Blocks.DIRT.defaultBlockState();
                } else {
                    block = Blocks.STONE.defaultBlockState();
                }

                layers.computeIfAbsent(dy, k -> new ArrayList<>()).add(new BlockPlacement(pos, block));
            }
        }
    }

    int totalBlocks = layers.values().stream().mapToInt(List::size).sum();
    player.sendSystemMessage(Component.literal("⏳ Animating " + totalBlocks + " blocks..."));

    AtomicInteger currentLayer = new AtomicInteger(0);
    List<Integer> heights = new ArrayList<>(layers.keySet());

    Runnable animateNextLayer = new Runnable() {
        @Override
        public void run() {
            if (currentLayer.get() >= heights.size()) {
                player.sendSystemMessage(Component.literal("✅ Mountain animation complete!"));
                return;
            }

            int layerY = heights.get(currentLayer.getAndIncrement());
            List<BlockPlacement> placements = layers.get(layerY);

            for (BlockPlacement placement : placements) {
                level.setBlock(placement.pos, placement.state, 2);
            }

            server.execute(() -> {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException ignored) {}
                server.execute(this);
            });
        }
    };

    server.execute(animateNextLayer);
}


    public static class BlockPlacement {
        public final BlockPos pos;
        public final BlockState state;

        public BlockPlacement(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
