package com.example.makan.telekinesis;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GrabTelekinesisPacket {
    private final BlockPos center;
    private final int radius;

    public GrabTelekinesisPacket(BlockPos center, int radius) {
        this.center = center;
        this.radius = radius;
    }

    public static void encode(GrabTelekinesisPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.center);
        buf.writeInt(msg.radius);
    }

    public static GrabTelekinesisPacket decode(FriendlyByteBuf buf) {
        return new GrabTelekinesisPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(GrabTelekinesisPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            TelekinesisHandler.serverGrabBlocks(ctx.get().getSender(), msg.center, msg.radius);
        });
        ctx.get().setPacketHandled(true);
    }
}
