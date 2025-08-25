package com.example.makan.gamemode;

import com.example.makan.YouAreMySpecial.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class MoguraAbilities implements AbilityHandler {

    @Override
    public void ability1(ServerPlayer player) {
        SpecialMountain.generateMountainWithAnimation(player, 40, 60);
    }

    @Override
    public void ability2(ServerPlayer player) {
        SpecialRavine.createAnimatedRavine(player, 50);
    }

    @Override
    public void ability3(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();

        // Base position
        BlockPos basePos = player.blockPosition().below();
        int radius = 2; // 5x5 pillar → radius 2
        int height = 8; // Height of the pillar

        // Build pillar upward from the ground
        for (int y = 0; y < height; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) <= radius && Math.abs(z) <= radius) {
                        BlockPos blockPos = basePos.offset(x, y, z);
                        level.setBlockAndUpdate(blockPos, Blocks.STONE.defaultBlockState());
                    }
                }
            }
        }

        // Move player up so they’re above the pillar & safe
        double safeY = basePos.getY() + height + 1;
        Vec3 playerPos = player.position();
        player.teleportTo(playerPos.x, safeY, playerPos.z);
    }
}
