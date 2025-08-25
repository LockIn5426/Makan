package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import net.minecraft.server.level.ServerPlayer;

public class SpecialFlight {

    /**
     * Grants the player temporary flight for a specified duration (in milliseconds).
     *
     * @param player         The server-side player to grant flight to.
     * @param durationMillis Duration to allow flight, in milliseconds.
     */
    public static void grantTemporaryFlight(ServerPlayer player, long durationMillis) {
        if (player == null || player.getServer() == null) {
            return;
        }

        YouAreMySpecialChat.chat(player, "✅ Granted flight for " + (durationMillis / 1000) + " seconds.");

        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        // Schedule flight removal after duration
        long ticks = durationMillis / 50; // 20 ticks/sec, so 50ms/tick
        player.getServer().execute(() -> scheduleFlightRemoval(player, ticks, 0));
    }

    private static void scheduleFlightRemoval(ServerPlayer player, long totalTicks, long currentTick) {
        if (currentTick >= totalTicks || player.isDeadOrDying()) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.fallDistance = 0f;
            player.onUpdateAbilities();
            YouAreMySpecialChat.chat(player, "⏳ Flight expired.");
            return;
        }

        player.getServer().execute(() -> {
            try {
                Thread.sleep(50); // delay 1 tick
            } catch (InterruptedException ignored) {}
            scheduleFlightRemoval(player, totalTicks, currentTick + 1);
        });
    }
}
