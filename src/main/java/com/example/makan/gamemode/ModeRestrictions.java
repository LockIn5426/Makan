package com.example.makan.gamemode;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "makan")
public class ModeRestrictions {

    @SubscribeEvent
    public static void onBlockBreak(PlayerEvent.BreakSpeed event) {
        event.getEntity().getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
            // if (cap.getMode() == CustomGameMode.BUILDER) {
            //     event.setNewSpeed(0); // can't break blocks
            // }
        });
    }
}
