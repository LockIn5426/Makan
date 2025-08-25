package com.example.makan.gamemode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MoguraBurrowPacket {
    private final boolean sneaking;

    public MoguraBurrowPacket(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public static void encode(MoguraBurrowPacket msg, net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBoolean(msg.sneaking);
    }

    public static MoguraBurrowPacket decode(net.minecraft.network.FriendlyByteBuf buf) {
        return new MoguraBurrowPacket(buf.readBoolean());
    }

    public static void handle(MoguraBurrowPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                    if (cap.getMode() == CustomGameMode.MOGURA) {
                        MoguraHandler.tryBurrow(player, msg.sneaking);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
