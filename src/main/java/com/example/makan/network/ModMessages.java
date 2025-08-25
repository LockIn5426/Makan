package com.example.makan.network;

import com.example.makan.gamemode.*;
import com.example.makan.telekinesis.*;
import com.example.makan.items.*;
import com.example.makan.YouAreMySpecial.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import java.util.Optional;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;

import java.util.function.Supplier;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("youaremyspecial", "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    private static int nextId() {
        return packetId++;
    }

    public static void register() {
        INSTANCE.registerMessage(
                nextId(),
                ExecuteSpecialCommandPacket.class,
                ExecuteSpecialCommandPacket::encode,
                ExecuteSpecialCommandPacket::decode,
                ExecuteSpecialCommandPacket::handle
        );
        INSTANCE.registerMessage(
                nextId(),
                SyncGameModePacket.class,
                SyncGameModePacket::encode,
                SyncGameModePacket::decode,
                SyncGameModePacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        INSTANCE.registerMessage(
                nextId(),
                ChoujinAbilityPacket.class,
                ChoujinAbilityPacket::encode,
                ChoujinAbilityPacket::decode,
                ChoujinAbilityPacket::handle
        );
        INSTANCE.registerMessage(
                nextId(),
                KajinFlightPacket.class,
                KajinFlightPacket::encode,
                KajinFlightPacket::decode,
                KajinFlightPacket::handle
        );
        INSTANCE.registerMessage(
                nextId(),
                KamiKazeLaunchPacket.class,
                KamiKazeLaunchPacket::encode,
                KamiKazeLaunchPacket::decode,
                KamiKazeLaunchPacket::handle
        );
        INSTANCE.registerMessage(
                nextId(),
                MoguraBurrowPacket.class,
                MoguraBurrowPacket::encode,
                MoguraBurrowPacket::decode,
                MoguraBurrowPacket::handle
        );
        INSTANCE.registerMessage(
                nextId(),
                TelekinesisSyncPacket.class,
                TelekinesisSyncPacket::encode,
                TelekinesisSyncPacket::decode,
                TelekinesisSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                GrabTelekinesisPacket.class,
                GrabTelekinesisPacket::encode,
                GrabTelekinesisPacket::decode,
                GrabTelekinesisPacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                ThrowTelekinesisPacket.class,
                ThrowTelekinesisPacket::encode,
                ThrowTelekinesisPacket::decode,
                ThrowTelekinesisPacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                PlaceTelekinesisPacket.class,
                PlaceTelekinesisPacket::encode,
                PlaceTelekinesisPacket::decode,
                PlaceTelekinesisPacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                PlaceClonePacket.class,
                PlaceClonePacket::encode,
                PlaceClonePacket::decode,
                PlaceClonePacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(), 
                SelectedMobsSyncPacket.class,
                SelectedMobsSyncPacket::encode,
                SelectedMobsSyncPacket::decode,
                SelectedMobsSyncPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                PlaceMobPacket.class,
                PlaceMobPacket::encode,
                PlaceMobPacket::decode,
                PlaceMobPacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                GrabMobPacket.class,
                GrabMobPacket::encode,
                GrabMobPacket::decode,
                GrabMobPacket::handle
        );
        ModMessages.INSTANCE.registerMessage(
                nextId(),
                ThrowMobPacket.class,
                ThrowMobPacket::encode,
                ThrowMobPacket::decode,
                ThrowMobPacket::handle
        );
    }


    public static class ExecuteSpecialCommandPacket {
    private final String command;
    private final int requiredBones;
    private final boolean isSpecial;

    public ExecuteSpecialCommandPacket(String command, int requiredBones, boolean isSpecial) {
        this.command = command;
        this.requiredBones = requiredBones;
        this.isSpecial = isSpecial;
    }

    public static void encode(ExecuteSpecialCommandPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.command);
        buf.writeInt(msg.requiredBones);
        buf.writeBoolean(msg.isSpecial);
    }

    public static ExecuteSpecialCommandPacket decode(FriendlyByteBuf buf) {
        String command = buf.readUtf();
        int bones = buf.readInt();
        boolean isSpecial = buf.readBoolean();
        return new ExecuteSpecialCommandPacket(command, bones, isSpecial);
    }

    public static void handle(ExecuteSpecialCommandPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // CASE 1: Player is holding the OracleBoneStaff in either hand
            ItemStack staffItem = player.getMainHandItem();
            InteractionHand staffHand = InteractionHand.MAIN_HAND;

            if (!(staffItem.getItem() instanceof OracleBoneStaff)) {
                staffItem = player.getOffhandItem();
                staffHand = InteractionHand.OFF_HAND;
            }

            boolean usedStaff = false;

            if (staffItem.getItem() instanceof OracleBoneStaff && !player.isCreative() && !player.isSpectator()) {
                int currentDamage = staffItem.getDamageValue();
                int maxDamage = staffItem.getMaxDamage();
                int remaining = maxDamage - currentDamage;

                // ❌ Not enough charge → block spell
                if (remaining < msg.requiredBones) {
                    player.sendSystemMessage(Component.literal(
                            "⚠ Not enough charge! Need " + msg.requiredBones + ", but only " + remaining + " left."
                    ));
                    return;
                }

                // ✅ Deduct charge
                staffItem.setDamageValue(currentDamage + msg.requiredBones);
                usedStaff = true;

                player.sendSystemMessage(Component.literal(
                        "✔ Staff charge reduced by " + msg.requiredBones + "."
                ));
            }

            // CASE 2: If no usable staff, fallback to activated oracle bones in inventory
            if (!usedStaff) {
                int available = findOracleBonesInInventory(player);
                if (!player.isCreative() && !player.isSpectator()) {
                    if (available < msg.requiredBones) {
                        player.sendSystemMessage(Component.literal(
                                "⚠ Not enough oracle bones! Need " + msg.requiredBones
                        ));
                        return;
                    }

                    // Consume bones
                    int remainingToRemove = msg.requiredBones;
                    for (ItemStack stack : player.getInventory().items) {
                        if (stack.isEmpty() || stack.getItem() != ModItems.ACTIVATED_ORACLE_BONE.get())
                            continue;

                        int remove = Math.min(stack.getCount(), remainingToRemove);
                        stack.shrink(remove);
                        remainingToRemove -= remove;
                        if (remainingToRemove <= 0) break;
                    }

                    player.sendSystemMessage(Component.literal(
                            "✔ Consumed " + msg.requiredBones + " oracle bones."
                    ));
                }
            }

            // If we reach here, cost was paid → execute the command
            if (msg.isSpecial) {
                SpecialCommands.executeDynamicSpecial(player, msg.command);
            } else {
                MinecraftServer server = player.getServer();
                if (server != null) {
                    server.getCommands().performPrefixedCommand(
                            player.createCommandSourceStack(), msg.command
                    );
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }


    private static int findOracleBonesInInventory(ServerPlayer player) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == ModItems.ACTIVATED_ORACLE_BONE.get()) {
                total += stack.getCount();
            }
        }
        return total;
    }
}


}

