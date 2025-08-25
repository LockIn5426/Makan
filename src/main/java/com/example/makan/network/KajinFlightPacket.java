package com.example.makan.gamemode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KajinFlightPacket {
    private final boolean forward;
    private final boolean back;
    private final boolean left;
    private final boolean right;
    private final boolean boost;
    private final boolean ascend;

    public KajinFlightPacket(boolean forward, boolean back, boolean left, boolean right, boolean boost, boolean ascend) {
        this.forward = forward;
        this.back = back;
        this.left = left;
        this.right = right;
        this.boost = boost;
        this.ascend = ascend;
    }

    public static void encode(KajinFlightPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.forward);
        buf.writeBoolean(msg.back);
        buf.writeBoolean(msg.left);
        buf.writeBoolean(msg.right);
        buf.writeBoolean(msg.boost);
        buf.writeBoolean(msg.ascend);
    }

    public static KajinFlightPacket decode(FriendlyByteBuf buf) {
        return new KajinFlightPacket(
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean(),
            buf.readBoolean()
        );
    }

    public static void handle(KajinFlightPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                if (cap.getMode() == CustomGameMode.KAJIN) {
                    KajinFlightHandler.updateKeys(
                        player, msg.forward, msg.back, msg.left, msg.right, msg.boost, msg.ascend
                    );
                }
            });
        });
        ctx.setPacketHandled(true);
    }
}

