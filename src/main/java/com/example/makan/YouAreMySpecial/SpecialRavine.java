package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import com.example.makan.YouAreMySpecial.SpecialCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class SpecialRavine {

    public static void createAnimatedRavine(ServerPlayer player, int length) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ No valid player or server level.");
            return;
        }

        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();

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
            YouAreMySpecialChat.chat("❌ No valid block targeted.");
            return;
        }

        Vec3 loc = result.getLocation();
        BlockPos center = BlockPos.containing(loc);
        YouAreMySpecialChat.chat("🪓 Carving ravine from: " + center);

        Vec3 forward = lookDir;
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        Random rand = new Random(center.asLong());

        List<List<BlockPos>> rows = new ArrayList<>();

        for (int dz = 0; dz <= length; dz++) {
            Vec3 stepForward = Vec3.atCenterOf(center).add(forward.scale(dz));
            BlockPos centerRow = BlockPos.containing(stepForward.x, center.getY(), stepForward.z);
            List<BlockPos> row = new ArrayList<>();

            int localWidth = 4 + rand.nextInt(3); // 4–6 block wide ravine
            int maxDepth = 12 + rand.nextInt(8);  // 12–19 blocks deep

            for (int dx = -localWidth / 2; dx <= localWidth / 2; dx++) {
                Vec3 offset = stepForward.add(side.scale(dx));
                int baseX = (int) offset.x;
                int baseZ = (int) offset.z;

                double noiseFactor = 0.5 + rand.nextDouble(); // 0.5 to 1.5
                int depth = (int) (maxDepth * noiseFactor);

                for (int dy = 0; dy <= depth; dy++) {
                    int y = center.getY() - dy;
                    if (y < level.getMinBuildHeight()) break;

                    BlockPos pos = new BlockPos(baseX, y, baseZ);
                    row.add(pos);
                }
            }

            rows.add(row);
        }

        YouAreMySpecialChat.chat("⏳ Animating natural ravine...");

        scheduleRavineAnimation(server, level, rows, 0);
    }

    private static void scheduleRavineAnimation(MinecraftServer server, ServerLevel level, List<List<BlockPos>> rows, int step) {
        if (step >= rows.size()) {
            YouAreMySpecialChat.chat("✅ Ravine carving complete!");
            return;
        }

        List<BlockPos> currentRow = rows.get(step);
        for (BlockPos pos : currentRow) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        }

        // Delay next row by 3 ticks (~150ms); adjust if needed
        server.execute(() -> scheduleRavineAnimation(server, level, rows, step + 1));
    }
}
