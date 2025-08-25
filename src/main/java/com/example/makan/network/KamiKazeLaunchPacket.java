package com.example.makan.gamemode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

public class KamiKazeLaunchPacket {

    public KamiKazeLaunchPacket() {}

    public static void encode(KamiKazeLaunchPacket msg, FriendlyByteBuf buf) {}

    public static KamiKazeLaunchPacket decode(FriendlyByteBuf buf) {
        return new KamiKazeLaunchPacket();
    }

    public static void handle(KamiKazeLaunchPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                    if (cap.getMode() == CustomGameMode.KAMIKAZE) {
                        Vec3 look = player.getLookAngle().normalize();
                        player.setDeltaMovement(look.scale(2.5)); // adjust speed
                        player.hurtMarked = true;
                        KamiKazeHandler.setLaunched(player, true);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
