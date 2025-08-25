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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.tags.FluidTags;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecialIce {

    public static void cast(ServerPlayer player, int radius) {
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
                ClipContext.Fluid.ANY,   // ✅ allow hitting water
                player
        ));

        if (result.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("❌ No valid target."));
            return;
        }

        BlockHitResult blockHit = (BlockHitResult) result;
        BlockPos hitPos = blockHit.getBlockPos();

        BlockState state = level.getBlockState(hitPos);
        boolean isWater = state.is(Blocks.WATER) || level.getFluidState(hitPos).is(FluidTags.WATER);

        // If we hit the air just above the surface, check the block below
        if (!isWater) {
            BlockPos below = hitPos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(Blocks.WATER) || level.getFluidState(below).is(FluidTags.WATER)) {
                hitPos = below;
                isWater = true;
            }
        }

        if (isWater) {
            player.sendSystemMessage(Component.literal("❄️ Freezing water into ice..."));
            freezeWaterWithAnimation(server, level, player, hitPos, radius);
        } else {
            player.sendSystemMessage(Component.literal("❄️ Summoning ice spikes!"));
            summonIceSpikes(server, level, player, hitPos, radius);
        }
    }


    private static void freezeWaterWithAnimation(MinecraftServer server, ServerLevel level, ServerPlayer player, BlockPos center, int radius) {
        Map<Integer, List<BlockPos>> rings = new TreeMap<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > radius * radius) continue;

                BlockPos pos = center.offset(dx, 0, dz);
                if (level.getBlockState(pos).is(Blocks.WATER)) {
                    int ring = (int) Math.sqrt(distSq);
                    rings.computeIfAbsent(ring, k -> new ArrayList<>()).add(pos);
                }
            }
        }

        AtomicInteger ringIndex = new AtomicInteger(0);
        List<Integer> ringKeys = new ArrayList<>(rings.keySet());

        Runnable animateRing = new Runnable() {
            @Override
            public void run() {
                if (ringIndex.get() >= ringKeys.size()) {
                    player.sendSystemMessage(Component.literal("✅ Water frozen into ice!"));
                    return;
                }

                int ring = ringKeys.get(ringIndex.getAndIncrement());
                for (BlockPos pos : rings.get(ring)) {
                    level.setBlock(pos, Blocks.ICE.defaultBlockState(), 2);
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateRing);
    }

    private static void summonIceSpikes(MinecraftServer server, ServerLevel level, ServerPlayer player, BlockPos center, int count) {
        Random random = new Random();
        List<List<BlockPos>> spikeStages = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // pick an outward angle
            double angle = (2 * Math.PI / count) * i + random.nextDouble() * 0.5;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);

            // base is around the center
            int baseDist = 1 + random.nextInt(2);
            int baseX = center.getX() + (int)Math.round(dirX * baseDist);
            int baseZ = center.getZ() + (int)Math.round(dirZ * baseDist);
            int baseY = center.getY();

            List<BlockPos> spike = new ArrayList<>();
            int height = 4 + random.nextInt(5);
            double tiltFactor = 0.3 + random.nextDouble() * 0.4; // how much it leans outward

            for (int h = 0; h < height; h++) {
                // shift position outward each layer
                double fx = baseX + dirX * h * tiltFactor;
                double fz = baseZ + dirZ * h * tiltFactor;
                int fy = baseY + h;

                // tapering radius
                int thickness = Math.max(1, 3 - h / 2);
                for (int ox = -thickness; ox <= thickness; ox++) {
                    for (int oz = -thickness; oz <= thickness; oz++) {
                        if (ox * ox + oz * oz <= thickness * thickness) {
                            BlockPos pos = new BlockPos(
                                (int)Math.round(fx) + ox,
                                fy,
                                (int)Math.round(fz) + oz
                            );
                            spike.add(pos);
                        }
                    }
                }
            }

            spikeStages.add(spike);
        }

        AtomicInteger step = new AtomicInteger(0);

        Runnable animateSpikes = new Runnable() {
            @Override
            public void run() {
                if (step.get() >= spikeStages.stream().mapToInt(List::size).max().orElse(0)) {
                    player.sendSystemMessage(Component.literal("✅ Ice spikes complete!"));
                    return;
                }

                for (List<BlockPos> spike : spikeStages) {
                    if (step.get() < spike.size()) {
                        BlockPos pos = spike.get(step.get());
                        level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 2);
                    }
                }

                step.getAndIncrement();

                server.execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateSpikes);
    }

}
