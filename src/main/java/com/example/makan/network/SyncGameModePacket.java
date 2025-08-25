package com.example.makan.gamemode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.client.Minecraft;

import java.util.function.Supplier;

public class SyncGameModePacket {
    private final int modeId;

    public SyncGameModePacket(int modeId) {
        this.modeId = modeId;
    }

    public static void encode(SyncGameModePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.modeId);
    }

    public static SyncGameModePacket decode(FriendlyByteBuf buf) {
        return new SyncGameModePacket(buf.readInt());
    }

    public static void handle(SyncGameModePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // This runs on client thread:
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                        cap.setMode(CustomGameMode.fromId(msg.modeId));
                    });
                }
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
