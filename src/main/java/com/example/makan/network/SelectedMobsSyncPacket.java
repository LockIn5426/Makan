package com.example.makan.network;

import com.example.makan.telekinesis.TelekinesisClientHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class SelectedMobsSyncPacket {
    private final int[] ids;

    public SelectedMobsSyncPacket(Set<Integer> ids) {
        this.ids = ids.stream().mapToInt(Integer::intValue).toArray();
    }

    public SelectedMobsSyncPacket(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        this.ids = new int[n];
        for (int i = 0; i < n; i++) ids[i] = buf.readVarInt();
    }

    public static void encode(SelectedMobsSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.ids.length);
        for (int id : pkt.ids) buf.writeVarInt(id);
    }

    public static SelectedMobsSyncPacket decode(FriendlyByteBuf buf) {
        return new SelectedMobsSyncPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> {
                TelekinesisClientHandler.selectedMobIdsClient.clear();
                for (int id : ids) TelekinesisClientHandler.selectedMobIdsClient.add(id);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}

