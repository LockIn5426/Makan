package com.example.makan.network;

import com.example.makan.telekinesis.TelekinesisHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaceMobPacket {
    public PlaceMobPacket() {}
    public PlaceMobPacket(FriendlyByteBuf buf) {}

    public static void encode(PlaceMobPacket pkt, FriendlyByteBuf buf) {}
    public static PlaceMobPacket decode(FriendlyByteBuf buf) { return new PlaceMobPacket(buf); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null) TelekinesisHandler.serverPlaceMobs(sp);
        });
        ctx.get().setPacketHandled(true);
    }
}

/*
Register in ModMessages as PLAY_TO_SERVER
*/
