package com.example.makan.network;

import com.example.makan.telekinesis.TelekinesisHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ThrowMobPacket {
    public ThrowMobPacket() {}
    public ThrowMobPacket(FriendlyByteBuf buf) {}

    public static void encode(ThrowMobPacket pkt, FriendlyByteBuf buf) {}
    public static ThrowMobPacket decode(FriendlyByteBuf buf) { return new ThrowMobPacket(buf); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) TelekinesisHandler.serverThrowMobs(sp);
        });
        ctx.get().setPacketHandled(true);
    }
}

