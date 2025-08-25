package com.example.makan.YouAreMySpecial;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class YouAreMySpecialChat {

    // ✅ Chat with player
    public static void chat(ServerPlayer player, String msg) {
        if (player == null || msg == null || msg.trim().isEmpty()) {
            System.out.println("Invalid chat request (player or message null).");
            return;
        }

        player.sendSystemMessage(Component.literal(msg));
    }

    // ✅ Console fallback (no player)
    public static void chat(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            System.out.println("No message to send to console.");
            return;
        }

        System.out.println("[YouAreMySpecialChat] " + msg);
    }
}
