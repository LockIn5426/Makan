package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecialBridge {

    public static void buildBridgeWithAnimation(ServerPlayer player, int maxDistance) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ Player or level not found.");
            return;
        }

        MinecraftServer server = player.getServer();
        ServerLevel level = player.serverLevel();

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        BlockPos start = BlockPos.containing(eye.add(look)); // Start 1 block ahead

        Map<Integer, List<BlockPlacement>> layers = new LinkedHashMap<>();
        int builtLength = 0;

        for (int i = 0; i < maxDistance; i++) {
            Vec3 stepVec = eye.add(look.scale(i + 1));
            BlockPos center = BlockPos.containing(stepVec.x, stepVec.y - 1, stepVec.z);

            if (!level.getBlockState(center).isAir()) {
                break;
            }

            List<BlockPlacement> layer = new ArrayList<>();

            // Axis alignment
            boolean isZAxis = Math.abs(look.z) > Math.abs(look.x);

            for (int offset = -1; offset <= 1; offset++) {
                BlockPos plankPos = isZAxis ? center.offset(offset, 0, 0) : center.offset(0, 0, offset);
                layer.add(new BlockPlacement(plankPos, Blocks.OAK_PLANKS.defaultBlockState()));
            }

            if (Math.abs(look.x) > 0.5 && Math.abs(look.z) > 0.5) {
                layer.add(new BlockPlacement(center, Blocks.OAK_PLANKS.defaultBlockState()));
            }

            BlockPos leftFence = isZAxis ? center.offset(-2, 1, 0) : center.offset(0, 1, -2);
            BlockPos rightFence = isZAxis ? center.offset(2, 1, 0) : center.offset(0, 1, 2);

            layer.add(new BlockPlacement(leftFence, Blocks.OAK_FENCE.defaultBlockState()));
            layer.add(new BlockPlacement(rightFence, Blocks.OAK_FENCE.defaultBlockState()));

            layer.add(new BlockPlacement(leftFence.below(), Blocks.OAK_LOG.defaultBlockState()));
            layer.add(new BlockPlacement(rightFence.below(), Blocks.OAK_LOG.defaultBlockState()));

            layers.put(i, layer);
            builtLength++;
        }

        if (builtLength == 0) {
            YouAreMySpecialChat.chat(player, "⚠️ Could not build bridge: path blocked.");
            return;
        }

        YouAreMySpecialChat.chat(player, "⏳ Animating bridge over " + builtLength + " blocks...");

        AtomicInteger current = new AtomicInteger(0);
        Runnable animateNext = new Runnable() {
            @Override
            public void run() {
                int step = current.getAndIncrement();
                if (step >= layers.size()) {
                    YouAreMySpecialChat.chat(player, "✅ Bridge complete!");
                    return;
                }

                for (BlockPlacement p : layers.get(step)) {
                    level.setBlock(p.pos, p.state, 2);
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateNext);
    }

    public record BlockPlacement(BlockPos pos, BlockState state) {}
}
