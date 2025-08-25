package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SpecialTeleport {

    public static void teleportToNearestPlayer(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("❌ Server not found."));
            return;
        }

        ServerLevel level = player.serverLevel();
        List<ServerPlayer> players = level.players();

        // Exclude self
        players.removeIf(p -> p.getUUID().equals(player.getUUID()));

        if (players.isEmpty()) {
            player.sendSystemMessage(Component.literal("❌ No other players online."));
            return;
        }

        // Find nearest player
        Optional<ServerPlayer> nearest = players.stream()
                .min(Comparator.comparingDouble(p -> p.distanceToSqr(player)));

        if (nearest.isPresent()) {
            ServerPlayer target = nearest.get();
            BlockPos targetPos = target.blockPosition();

            // Teleport player to the target player's position
            player.teleportTo(level, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            player.sendSystemMessage(Component.literal("🌀 Teleported to nearest player: " + target.getName().getString()));
        } else {
            player.sendSystemMessage(Component.literal("❌ Could not find a nearest player."));
        }
    }
}
