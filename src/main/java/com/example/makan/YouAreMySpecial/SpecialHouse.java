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

public class SpecialHouse {

    public static void spawnHouseLookingAt(ServerPlayer player) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ Player or server level is null.");
            return;
        }

        MinecraftServer server = player.getServer();
        ServerLevel level = player.serverLevel();

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle().normalize();

        BlockPos center = BlockPos.containing(eye.add(look.scale(5)));
        int halfWidth = 3;
        int height = 5;

        YouAreMySpecialChat.chat(player, "⏳ Building house at " + center.getX() + ", " + center.getY() + ", " + center.getZ());

        // Build layers of block placements
        Map<Integer, List<BlockPlacement>> layers = new LinkedHashMap<>();

        for (int y = 0; y < height; y++) {
            List<BlockPlacement> layer = new ArrayList<>();

            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    BlockPos pos = center.offset(dx, y, dz);

                    // Floor
                    if (y == 0) {
                        layer.add(new BlockPlacement(pos, Blocks.OAK_PLANKS.defaultBlockState()));
                        continue;
                    }

                    // Walls
                    boolean edgeX = dx == -halfWidth || dx == halfWidth;
                    boolean edgeZ = dz == -halfWidth || dz == halfWidth;
                    boolean isWall = edgeX || edgeZ;

                    if (isWall) {
                        // Door opening
                        if (dz == 0 && dx == 0 && y <= 1) {
                            continue;
                        }

                        // Glass windows
                        if ((dx == 0 || dz == 0) && y == 2) {
                            layer.add(new BlockPlacement(pos, Blocks.GLASS_PANE.defaultBlockState()));
                        } else {
                            layer.add(new BlockPlacement(pos, Blocks.OAK_LOG.defaultBlockState()));
                        }
                    }
                }
            }

            layers.put(y, layer);
        }

        // Roof (last layer)
        List<BlockPlacement> roof = new ArrayList<>();
        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                BlockPos roofPos = center.offset(dx, height, dz);
                roof.add(new BlockPlacement(roofPos, Blocks.OAK_PLANKS.defaultBlockState()));
            }
        }
        layers.put(height, roof);

        // Door (place after structure)
        BlockPos doorBottom = center;
        BlockPos doorTop = center.above();
        layers.put(height + 1, List.of(
            new BlockPlacement(doorBottom, Blocks.OAK_DOOR.defaultBlockState()),
            new BlockPlacement(doorTop, Blocks.OAK_DOOR.defaultBlockState())
        ));

        // Animate placement
        AtomicInteger index = new AtomicInteger(0);
        Runnable placeNextLayer = new Runnable() {
            @Override
            public void run() {
                int step = index.getAndIncrement();
                if (step >= layers.size()) {
                    YouAreMySpecialChat.chat(player, "✅ House complete!");
                    return;
                }

                for (BlockPlacement bp : layers.get(step)) {
                    level.setBlock(bp.pos, bp.state, 2);
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(100); // delay between layers
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(placeNextLayer);
    }

    public record BlockPlacement(BlockPos pos, BlockState state) {}
}
