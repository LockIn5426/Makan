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

public class SpecialMansion {

    public static void generateMansionWithAnimation(ServerPlayer player) {
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
        BlockPos corner = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);

        player.sendSystemMessage(Component.literal("🏰 Animating Special Mansion at: " + corner));

        // Woodland Mansion approximate size
        int width = 80;   // X
        int depth = 80;   // Z
        int height = 25;  // Y

        Map<Integer, List<BlockPlacement>> layers = new TreeMap<>();

        // Build rectangular floors + walls + roof
        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < width; dx++) {
                for (int dz = 0; dz < depth; dz++) {
                    BlockPos pos = corner.offset(dx, y, dz);

                    BlockState state;
                    if (y == 0) {
                        state = Blocks.COBBLESTONE.defaultBlockState(); // foundation
                    } else if (y < height - 5) {
                        if (dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1) {
                            state = Blocks.DARK_OAK_LOG.defaultBlockState(); // outer frame
                        } else if (y % 5 == 0) {
                            state = Blocks.SPRUCE_PLANKS.defaultBlockState(); // floor slabs
                        } else {
                            state = Blocks.AIR.defaultBlockState(); // keep hollow inside
                        }
                    } else {
                        // Roof section
                        if (dx == 0 || dz == 0 || dx == width - 1 || dz == depth - 1) {
                            state = Blocks.DARK_OAK_SLAB.defaultBlockState();
                        } else {
                            state = Blocks.DARK_OAK_PLANKS.defaultBlockState();
                        }
                    }

                    // Only schedule if it's not air
                    if (!state.isAir()) {
                        layers.computeIfAbsent(y, k -> new ArrayList<>())
                                .add(new BlockPlacement(pos, state));
                    }
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
                    player.sendSystemMessage(Component.literal("✅ Mansion animation complete!"));
                    return;
                }

                int layerY = heights.get(currentLayer.getAndIncrement());
                List<BlockPlacement> placements = layers.get(layerY);

                for (BlockPlacement placement : placements) {
                    level.setBlock(placement.pos, placement.state, 2);
                }

                // Animate next layer with delay
                server.execute(() -> {
                    try {
                        Thread.sleep(60); // ~1 layer every 60ms
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
