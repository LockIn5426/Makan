package com.example.makan.gamemode;

import com.example.makan.network.ModMessages;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkDirection;

@Mod.EventBusSubscriber(modid = "makan")
public class GameModeCommand {

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("choujin")
                .then(Commands.literal("survivallike")
                    .executes(ctx -> setMode(ctx, CustomGameMode.SURVIVAL_LIKE)))
                .then(Commands.literal("kajin")
                    .executes(ctx -> setMode(ctx, CustomGameMode.KAJIN)))
                .then(Commands.literal("kamikaze")
                    .executes(ctx -> setMode(ctx, CustomGameMode.KAMIKAZE)))
                .then(Commands.literal("mogura")
                    .executes(ctx -> setMode(ctx, CustomGameMode.MOGURA)))
                .then(Commands.literal("nendou")
                    .executes(ctx -> setMode(ctx, CustomGameMode.NENDOU)))

        );
    }

    private static int setMode(CommandContext<CommandSourceStack> ctx, CustomGameMode mode) 
        throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> cap.setMode(mode));
        
        // Send sync packet to client
        ModMessages.INSTANCE.sendTo(
            new SyncGameModePacket(mode.getId()), 
            player.connection.connection, 
            NetworkDirection.PLAY_TO_CLIENT
        );

        player.sendSystemMessage(Component.literal("Custom mode set to " + mode));
        return Command.SINGLE_SUCCESS;
    }
}
