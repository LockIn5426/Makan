package com.example.makan.YouAreMySpecial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class SpecialGlide {

    private static final Map<UUID, Integer> glideTicksRemaining = new HashMap<>();
    private static final Set<UUID> glideActive = new HashSet<>();

    public static void startGlideMode(ServerPlayer player, int durationTicks) {
        if (player == null) return;

        UUID id = player.getUUID();

        if (glideActive.contains(id)) return;

        // Launch player upward and forward
        Vec3 look = player.getLookAngle().normalize();
        Vec3 launch = look.scale(0.6).add(0, 0.8, 0);
        player.setDeltaMovement(launch);
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.fallDistance = 0;

        // Activate Elytra-like glide
        glideActive.add(id);
        glideTicksRemaining.put(id, durationTicks);
        player.startFallFlying();

        System.out.println("✅ Glide started for " + player.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (!(event.player instanceof ServerPlayer player) || event.phase != TickEvent.Phase.END) return;

        UUID id = player.getUUID();

        // Not gliding? Clean up and return
        if (!glideActive.contains(id)) return;

        // Countdown
        int ticksLeft = glideTicksRemaining.getOrDefault(id, 0);
        if (ticksLeft <= 0 || player.onGround() || player.isShiftKeyDown()) {
            glideActive.remove(id);
            glideTicksRemaining.remove(id);
            System.out.println("🛑 Glide ended for " + player.getName().getString());
            return;
        }

        glideTicksRemaining.put(id, ticksLeft - 1);

        // Apply custom glide motion
        Vec3 look = player.getLookAngle().normalize();
        Vec3 motion = player.getDeltaMovement();

        Vec3 newMotion = new Vec3(
            motion.x * 0.98 + look.x * 0.07,
            Math.max(motion.y - 0.02, -0.1),
            motion.z * 0.98 + look.z * 0.07
        );

        player.setDeltaMovement(newMotion);
        player.hurtMarked = true;
        player.hasImpulse = true;
        player.fallDistance = 0;

        // Spawn particles for fun
        player.serverLevel().sendParticles(ParticleTypes.CLOUD,
            player.getX(), player.getY(), player.getZ(),
            5, 0.1, 0.1, 0.1, 0.01);
    }
}
