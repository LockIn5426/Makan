package com.example.makan.YouAreMySpecial;

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

public class SpecialHallway {

    public static void generateHallway(ServerPlayer player, int length) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("❌ Server not found."));
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        double maxDistance = 128;
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
        BlockPos start = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);

        player.sendSystemMessage(Component.literal("🚪 Generating hollow glass hallway at: " + start));

        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        Map<Integer, List<BlockPlacement>> steps = new TreeMap<>();

        for (int i = 0; i < length; i++) {
            BlockPos segmentCenter = start.offset(
                    (int) Math.round(lookDir.x * i),
                    (int) Math.round(lookDir.y * i),
                    (int) Math.round(lookDir.z * i)
            );

            List<BlockPlacement> segmentBlocks = new ArrayList<>();

            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = 0; dy <= 5; dy++) {
                    BlockPos pos = segmentCenter.offset(dx, dy, 0);
                    level.setBlock(pos, air, 2);
                    if (dx == -2 || dx == 2 || dy == 0 || dy == 5) {
                        segmentBlocks.add(new BlockPlacement(pos, glass));
                    }
                }
            }

            steps.put(i, segmentBlocks);
        }

        int totalBlocks = steps.values().stream().mapToInt(List::size).sum();
        player.sendSystemMessage(Component.literal("⏳ Animating " + totalBlocks + " hallway blocks..."));

        AtomicInteger currentStep = new AtomicInteger(0);
        List<Integer> segments = new ArrayList<>(steps.keySet());

        Runnable animateNextStep = new Runnable() {
            @Override
            public void run() {
                if (currentStep.get() >= segments.size()) {
                    player.sendSystemMessage(Component.literal("✅ Hollow glass hallway complete!"));
                    return;
                }

                int stepIndex = segments.get(currentStep.getAndIncrement());
                List<BlockPlacement> placements = steps.get(stepIndex);

                for (BlockPlacement placement : placements) {
                    level.setBlock(placement.pos, placement.state, 2);
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(40); // delay for animation
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateNextStep);
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
