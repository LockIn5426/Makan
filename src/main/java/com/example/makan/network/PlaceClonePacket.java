package com.example.makan.network;

import com.example.makan.telekinesis.TelekinesisHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class PlaceClonePacket {

    public PlaceClonePacket() {}

    public static void encode(PlaceClonePacket msg, FriendlyByteBuf buf) {}

    public static PlaceClonePacket decode(FriendlyByteBuf buf) {
        return new PlaceClonePacket();
    }

    public static void handle(PlaceClonePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                TelekinesisHandler.serverPlaceClone(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
