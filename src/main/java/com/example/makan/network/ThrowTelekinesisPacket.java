package com.example.makan.telekinesis;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThrowTelekinesisPacket {
    public static void encode(ThrowTelekinesisPacket msg, FriendlyByteBuf buf) {}

    public static ThrowTelekinesisPacket decode(FriendlyByteBuf buf) {
        return new ThrowTelekinesisPacket();
    }

    public static void handle(ThrowTelekinesisPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TelekinesisHandler.serverThrow(ctx.get().getSender());
        });
        ctx.get().setPacketHandled(true);
    }
}
