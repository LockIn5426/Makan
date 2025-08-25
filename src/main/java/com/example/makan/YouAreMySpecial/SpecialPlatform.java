package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.commands.CommandSourceStack;

public class SpecialPlatform {

    private static final double DEFAULT_DISTANCE = 100;

    // Public static method to trigger platform creation from server context
    public static void spawnPlatform(ServerPlayer player) {
        spawnPlatform(player, DEFAULT_DISTANCE);
    }

    // Overloaded method with custom distance
    public static void spawnPlatform(ServerPlayer player, double distance) {
        new SpecialPlatform(player).spawn(distance);
    }

    private final ServerPlayer player;
    private final ServerLevel level;

    // Constructor using ServerPlayer
    public SpecialPlatform(ServerPlayer player) {
        this.player = player;
        this.level = player.serverLevel();
    }

    private void spawn(double distance) {
        if (player == null || level == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("❌ You must be in a world."));
            return;
        }

        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 targetPos = eye.add(look.scale(distance));

        BlockPos targetBlock = findGroundAt(targetPos);
        if (targetBlock == null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("❌ Couldn't find ground below target point."));
            return;
        }

        buildPlatform(targetBlock);
    }

    private BlockPos findGroundAt(Vec3 target) {
        int x = (int) Math.floor(target.x);
        int z = (int) Math.floor(target.z);
        int yTop = level.getMaxBuildHeight() - 1;

        for (int y = yTop; y > 0; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) {
                return pos.above();
            }
        }

        return null;
    }

    private void buildPlatform(BlockPos center) {
        int baseX = center.getX();
        int baseY = center.getY();
        int baseZ = center.getZ();
        int r = 8;

        String fillStone = String.format(
            "fill %d %d %d %d %d %d stone replace air",
            baseX - r, baseY, baseZ - r,
            baseX + r, baseY + r, baseZ + r
        );

        String fillGrass = String.format(
            "fill %d %d %d %d %d %d grass_block replace stone",
            baseX - r, baseY + r, baseZ - r,
            baseX + r, baseY + r, baseZ + r
        );

        CommandSourceStack source = player.createCommandSourceStack();

        level.getServer().getCommands().performPrefixedCommand(source, fillStone);
        level.getServer().getCommands().performPrefixedCommand(source, fillGrass);
    }
}
