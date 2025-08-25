package com.example.makan.gamemode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KajinFlightHandler {
    private static final Map<UUID, boolean[]> keyStates = new ConcurrentHashMap<>();

    // forward, back, left, right, boost, ascend
    public static void updateKeys(ServerPlayer player, boolean forward, boolean back, boolean left, boolean right, boolean boost, boolean ascend) {
        keyStates.put(player.getUUID(), new boolean[]{forward, back, left, right, boost, ascend});
    }

    public static boolean hasKeys(ServerPlayer player) {
        return keyStates.containsKey(player.getUUID());
    }

    public static void clearKeys(ServerPlayer player) {
        keyStates.remove(player.getUUID());
        player.setNoGravity(false);
        player.hurtMarked = true;
    }

    public static void passiveEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.FIRE_RESISTANCE,
                40, // duration in ticks
                0,  // amplifier
                false, // ambient
                true   // showParticles and HUD icon
            ));
            BlockPos below = player.blockPosition().below();
            Level level = player.level();

            if (!level.isClientSide) {
                if (level.getBlockState(below).is(Blocks.WATER)) {
                level.setBlockAndUpdate(below, Blocks.OBSIDIAN.defaultBlockState());
                } else if (!level.getBlockState(below).isAir() && !level.getBlockState(below).is(Blocks.OBSIDIAN) && !level.getBlockState(below).is(Blocks.LAVA)) {
                level.setBlockAndUpdate(below, Blocks.MAGMA_BLOCK.defaultBlockState());
                }
        }
    }

    public static void handleFlight(ServerPlayer player) {
        boolean[] keys = keyStates.get(player.getUUID());
        if (keys == null) {
            player.setNoGravity(false);
            return;
        }

        boolean forward = keys[0];
        boolean back = keys[1];
        boolean left = keys[2];
        boolean right = keys[3];
        boolean boost = keys[4];
        boolean ascend = keys[5];

        double baseSpeed = 0.5;
        double boostMultiplier = 2.0;
        double speed = boost ? baseSpeed * boostMultiplier : baseSpeed;

        Vec3 motion = player.getDeltaMovement();

        // Movement directions
        Vec3 forwardVec = player.getLookAngle().normalize();
        Vec3 strafeVec = new Vec3(forwardVec.z, 0, -forwardVec.x).normalize(); // perpendicular to forward

        if (forward) {
            motion = motion.add(forwardVec.x * speed, forwardVec.y * speed * 0.2, forwardVec.z * speed);
        }
        if (back) {
            motion = motion.add(-forwardVec.x * speed, -forwardVec.y * speed * 0.2, -forwardVec.z * speed);
        }
        if (left) {
            motion = motion.add(strafeVec.x * baseSpeed, 0, strafeVec.z * baseSpeed);
        }
        if (right) {
            motion = motion.add(-strafeVec.x * baseSpeed, 0, -strafeVec.z * baseSpeed);
        }

        if (ascend) {
            motion = new Vec3(motion.x, speed, motion.z);
            player.setNoGravity(true);
        } else {
            player.setNoGravity(false);
        }

        player.setDeltaMovement(motion);
        player.hurtMarked = true;
        player.fallDistance = 0;
    }
}

