package com.example.makan.gamemode;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ChoujinAbilityPacket {
    private final int abilityId;

    public ChoujinAbilityPacket(int abilityId) {
        this.abilityId = abilityId;
    }

    public ChoujinAbilityPacket(FriendlyByteBuf buf) {
        this.abilityId = buf.readInt();
    }

    // Encode (write)
    public static void encode(ChoujinAbilityPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.abilityId);
    }

    // Decode (read)
    public static ChoujinAbilityPacket decode(FriendlyByteBuf buf) {
        return new ChoujinAbilityPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                    AbilityHandler handler = AbilityRegistry.getHandler(cap.getMode());
                    if (handler != null) {
                        switch (abilityId) {
                            case 1 -> handler.ability1(player);
                            case 2 -> handler.ability2(player);
                            case 3 -> handler.ability3(player);
                        }
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
