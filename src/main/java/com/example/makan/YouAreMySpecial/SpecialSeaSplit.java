package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecialSeaSplit {

    // Checks if the block is water or any sea plant
    private static boolean isSeaBlock(BlockState state) {
        return state.getBlock() == Blocks.WATER
            || state.getBlock() == Blocks.KELP
            || state.getBlock() == Blocks.KELP_PLANT
            || state.getBlock() == Blocks.SEAGRASS
            || state.getBlock() == Blocks.TALL_SEAGRASS;
    }

    public static void splitSeaWithAnimation(ServerPlayer player, int width, int length) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ Player or server level is null.");
            return;
        }

        MinecraftServer server = player.getServer();
        ServerLevel level = player.serverLevel();

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 endVec = eyePos.add(lookDir.scale(512));

        HitResult result = level.clip(new ClipContext(
            eyePos,
            endVec,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.ANY,
            player
        ));

        if (result.getType() != HitResult.Type.BLOCK) {
            YouAreMySpecialChat.chat(player, "❌ No valid water block targeted.");
            return;
        }

        Vec3 loc = result.getLocation();
        BlockPos center = new BlockPos((int) loc.x, (int) loc.y, (int) loc.z);
        YouAreMySpecialChat.chat(player, "🌊 Splitting sea from: " + center);

        Vec3 forward = lookDir.normalize();
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();

        List<List<BlockPos>> rows = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();

        for (int dz = 0; dz <= length; dz++) {
            Vec3 stepForward = center.getCenter().add(forward.scale(dz));
            List<BlockPos> row = new ArrayList<>();

            for (int dx = -width / 2; dx <= width / 2; dx++) {
                Vec3 step = stepForward.add(side.scale(dx));
                BlockPos surface = new BlockPos((int) step.x, center.getY(), (int) step.z);
                int y = surface.getY();

                while (y > 1) {
                    BlockPos pos = new BlockPos(surface.getX(), y, surface.getZ());
                    BlockState state = level.getBlockState(pos);

                    if (isSeaBlock(state)) {
                        if (visited.add(pos)) {
                            row.add(pos);

                            // Also remove nearby sea blocks in a 3x3x3 area
                            for (int ox = -1; ox <= 1; ox++) {
                                for (int oy = -1; oy <= 1; oy++) {
                                    for (int oz = -1; oz <= 1; oz++) {
                                        BlockPos neighbor = pos.offset(ox, oy, oz);
                                        BlockState nState = level.getBlockState(neighbor);
                                        if (visited.add(neighbor) && isSeaBlock(nState)) {
                                            row.add(neighbor);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (!state.isAir()) {
                        break;
                    }

                    y--;
                }
            }

            rows.add(row);
        }

        YouAreMySpecialChat.chat(player, "⏳ Building path forward...");

        AtomicInteger currentStep = new AtomicInteger(0);

        Runnable animateNext = new Runnable() {
            @Override
            public void run() {
                if (currentStep.get() >= rows.size()) {
                    YouAreMySpecialChat.chat(player, "✅ Sea bridge complete!");
                    return;
                }

                List<BlockPos> currentRow = rows.get(currentStep.getAndIncrement());

                for (BlockPos pos : currentRow) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);

                    // Optionally add barriers to stop water from refilling
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = -1; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) == 1) {
                                    BlockPos neighbor = pos.offset(dx, dy, dz);
                                    if (isSeaBlock(level.getBlockState(neighbor))) {
                                        level.setBlock(neighbor, Blocks.BARRIER.defaultBlockState(), 2);
                                    }
                                }
                            }
                        }
                    }
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(60);
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateNext);
    }
}
