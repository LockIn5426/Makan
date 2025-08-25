package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpecialDeathTeleport {

    // Stores last death position for each player
    private static final Map<UUID, BlockPos> lastDeathPositions = new HashMap<>();

    // Call this when a player dies
    public static void recordDeath(ServerPlayer player) {
        lastDeathPositions.put(player.getUUID(), player.blockPosition());
    }

    // Call this to teleport the player to their last death spot
    public static void teleportToLastDeath(ServerPlayer player) {
        BlockPos deathPos = lastDeathPositions.get(player.getUUID());
        if (deathPos == null) {
            player.sendSystemMessage(Component.literal("❌ No death position recorded."));
            return;
        }

        ServerLevel level = player.serverLevel();
        player.teleportTo(level, deathPos.getX() + 0.5, deathPos.getY(), deathPos.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.sendSystemMessage(Component.literal("💀 Teleported to your last death spot."));
    }
}
