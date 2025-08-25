package com.example.makan.gamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.FluidTags;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MoguraHandler {

    // Track players who are burrowed
    private static final Set<UUID> burrowedPlayers = new HashSet<>();

    /**
     * Called each server tick for players in MOGURA mode.
     */
    public static void handle(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // Check if burrowed and try to un-burrow
        if (burrowedPlayers.contains(playerId) &&
            player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {

            Vec3 eyeVec = player.getEyePosition();
            BlockPos eyePos = new BlockPos(
                    Mth.floor(eyeVec.x),
                    Mth.floor(eyeVec.y),
                    Mth.floor(eyeVec.z)
            );

            BlockPos feetPos = player.blockPosition();

            boolean eyeClear = player.level().getBlockState(eyePos).isAir();
            boolean feetClear = player.level().getBlockState(feetPos).isAir();

            boolean inWater = player.level().getFluidState(feetPos).is(FluidTags.WATER);

            if ((eyeClear && feetClear) || inWater) {
                player.setGameMode(GameType.SURVIVAL);
                burrowedPlayers.remove(playerId);
            }
        }
    }

    /**
     * Called when the Mogura burrow packet is received.
     */
    public static void tryBurrow(ServerPlayer player, boolean sneaking) {
        boolean isSpectator = player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;

        if (sneaking) {
            // Enter burrow if on ground and not already burrowed
            if (!isSpectator && player.onGround() && !burrowedPlayers.contains(player.getUUID())) {
                player.teleportTo(player.getX(), player.getY() - 3, player.getZ());
                player.setGameMode(GameType.SPECTATOR);
                burrowedPlayers.add(player.getUUID());
            }
        }
    }
}
