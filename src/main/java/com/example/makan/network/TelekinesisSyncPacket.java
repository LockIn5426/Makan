package com.example.makan.telekinesis;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraftforge.network.NetworkEvent;

import net.minecraft.network.FriendlyByteBuf;
import java.util.function.Supplier;

public class TelekinesisSyncPacket {
    private final int entityId;
    private final ListTag blocksNBT;

    public TelekinesisSyncPacket(int entityId, ListTag blocksNBT) {
        this.entityId = entityId;
        this.blocksNBT = blocksNBT;
    }

    public static void encode(TelekinesisSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        CompoundTag tag = new CompoundTag();
        tag.put("Blocks", pkt.blocksNBT);
        buf.writeNbt(tag);
    }

    public static TelekinesisSyncPacket decode(FriendlyByteBuf buf) {
        int id = buf.readInt();
        CompoundTag tag = buf.readNbt();
        ListTag blocksNBT = tag == null ? new ListTag() : tag.getList("Blocks", 10);
        return new TelekinesisSyncPacket(id, blocksNBT);
    }

    public static void handle(TelekinesisSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;

            var level = player.level();
            if (level == null) return;

            var entity = level.getEntity(pkt.entityId);
            if (entity instanceof TelekinesisEntity tele) {
                tele.setCarriedBlocksFromNBT(pkt.blocksNBT);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
