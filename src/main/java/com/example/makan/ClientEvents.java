package com.example.makan;

import com.example.makan.YouAreMySpecial.*;
import com.example.makan.entities.*;
import com.example.makan.particles.*;
import com.example.makan.gamemode.*;
import com.example.makan.telekinesis.*;
import com.example.makan.network.*;
import com.example.makan.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;

import java.util.*;

public class ClientEvents {

    @Mod.EventBusSubscriber(modid = Makan.MODID, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        private static boolean lastShift = false;
        private static boolean lastOnGround = true;
        private static boolean isBurrowed = false;

        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (KeyBinding.CASTING_KEY.consumeClick()) {
                DrawingScreen.cast();
                Makan.storedKanjiSlots.set(Makan.selectedSlot, "");
            }

            if (KeyBinding.CYCLING_KEY.consumeClick()) {
                if (Minecraft.getInstance().screen == null && !DrawingScreen.lastMatches.isEmpty()) {
                    Minecraft.getInstance().setScreen(new KanjiCyclerOverlay());
                }
            }
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (KeyBinding.ABILITY1_KEY.consumeClick()) {
                ModMessages.INSTANCE.sendToServer(new ChoujinAbilityPacket(1));
            }
            if (KeyBinding.ABILITY2_KEY.consumeClick()) {
                ModMessages.INSTANCE.sendToServer(new ChoujinAbilityPacket(2));
            }
            if (KeyBinding.ABILITY3_KEY.consumeClick()) {
                ModMessages.INSTANCE.sendToServer(new ChoujinAbilityPacket(3));
            }
            
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player == null) return;
            boolean forward = mc.options.keyUp.isDown();
            boolean back = mc.options.keyDown.isDown();
            boolean left = mc.options.keyLeft.isDown();
            boolean right = mc.options.keyRight.isDown();
            boolean boost = mc.options.keySprint.isDown();
            boolean ascend = mc.options.keyJump.isDown();
            boolean shift = mc.options.keyShift.isDown();
            boolean onGround = player.onGround();
            LazyOptional<GameModeCapability> capOptional = player.getCapability(GameModeCapability.INSTANCE);
            if (capOptional.isPresent()) {
                capOptional.ifPresent(cap -> {
                    if (cap.getMode() == CustomGameMode.KAJIN) {
                        // Send packet every tick when in GOD_MODE
                        ModMessages.INSTANCE.sendToServer(new KajinFlightPacket(forward, back, left, right, boost, ascend));
                    } 
                    if (cap.getMode() == CustomGameMode.MOGURA) {
                        // Enter spectator (burrow) when pressing shift on ground
                        if (isBurrowed && Minecraft.getInstance().gameMode.getPlayerMode() != GameType.SPECTATOR) {
                            isBurrowed = false;
                        }

                        // Enter spectator (burrow) when pressing shift on ground
                        if (shift && !lastShift && onGround && !isBurrowed) {
                            ModMessages.INSTANCE.sendToServer(new MoguraBurrowPacket(true));
                            isBurrowed = true;
                        }
                    }
                });
            }
            lastShift = shift;
            lastOnGround = onGround;
        }

        @SubscribeEvent
        public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
            if (event.getHand() != InteractionHand.MAIN_HAND) return;
            if (!(event.getEntity() instanceof LocalPlayer player)) return;

            if (!player.getMainHandItem().isEmpty()) return;

            player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                if (cap.getMode() == CustomGameMode.KAMIKAZE) {
                    ModMessages.INSTANCE.sendToServer(new KamiKazeLaunchPacket());
                }
            });
        }


    }

    @Mod.EventBusSubscriber(modid = Makan.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ClientModBusEvents {

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            event.register(KeyBinding.CASTING_KEY);
            event.register(KeyBinding.TURN_LEFT_KEY);
            event.register(KeyBinding.TURN_RIGHT_KEY);
            event.register(KeyBinding.CYCLING_KEY);
            event.register(KeyBinding.NEXT_SLOT_KEY);
            event.register(KeyBinding.ABILITY1_KEY);
            event.register(KeyBinding.ABILITY2_KEY);
            event.register(KeyBinding.ABILITY3_KEY);
        }


        @SubscribeEvent
        public static void onRegisterRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GODZILLA.get(), GodzillaRenderer::new);
            event.registerEntityRenderer(ModEntities.TELEKINESIS_BLOB.get(), TelekinesisRenderer::new);
            event.registerEntityRenderer(ModEntities.CLONE.get(), CloneRenderer::new);
        }

    }

}
