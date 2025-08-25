package com.example.makan.telekinesis;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PlaceTelekinesisPacket {

    public PlaceTelekinesisPacket() {}

    public static void encode(PlaceTelekinesisPacket pkt, FriendlyByteBuf buf) {}

    public static PlaceTelekinesisPacket decode(FriendlyByteBuf buf) { return new PlaceTelekinesisPacket(); }

    public static void handle(PlaceTelekinesisPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = ctx.get().getSender();
            if (player != null) {
                TelekinesisHandler.serverPlaceBlocks(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
