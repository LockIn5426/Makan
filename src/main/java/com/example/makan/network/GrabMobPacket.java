package com.example.makan.network;

import com.example.makan.telekinesis.TelekinesisHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GrabMobPacket {
    private final int entityId;

    public GrabMobPacket(int entityId) { this.entityId = entityId; }
    public GrabMobPacket(FriendlyByteBuf buf) { this.entityId = buf.readVarInt(); }

    public static void encode(GrabMobPacket pkt, FriendlyByteBuf buf) { buf.writeVarInt(pkt.entityId); }
    public static GrabMobPacket decode(FriendlyByteBuf buf) { return new GrabMobPacket(buf); }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;
            Entity e = sp.level().getEntity(entityId);
            if (e instanceof LivingEntity le && le.isAlive()) {
                TelekinesisHandler.serverGrabMob(sp, le);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

