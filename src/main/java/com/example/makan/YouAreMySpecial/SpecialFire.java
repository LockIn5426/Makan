package com.example.makan.YouAreMySpecial;

import com.example.makan.particles.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class SpecialFire {

    public static void cast(ServerPlayer player, float damage, int fireTicks) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ Invalid player or server.");
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 endVec = eyePos.add(lookDir.scale(64)); // Max range

        // Entity targeting
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(64)).inflate(1.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> !entity.equals(player) && entity.isPickable());

        LivingEntity target = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity entity : entities) {
            AABB hitbox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> intersection = hitbox.clip(eyePos, endVec);
            if (intersection.isPresent()) {
                double dist = eyePos.distanceTo(intersection.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    target = entity;
                }
            }
        }

        Vec3 hitPos;

        if (target != null) {
            hitPos = target.position().add(0, target.getBbHeight() / 2, 0); // middle of target
            target.hurt(player.damageSources().playerAttack(player), damage);
            target.setSecondsOnFire(fireTicks);

            // Sound
            level.playSound(null, target.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 2.0f, 1.0f);
            YouAreMySpecialChat.chat("🔥 Special Fire hit: " + target.getName().getString());
        } else {
            HitResult result = level.clip(new ClipContext(
                    eyePos,
                    endVec,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.NONE,
                    player
            ));

            if (result.getType() == HitResult.Type.BLOCK) {
                hitPos = result.getLocation();
                level.playSound(null, BlockPos.containing(hitPos), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
                YouAreMySpecialChat.chat("🔥 Special Fire landed on block.");
            } else {
                YouAreMySpecialChat.chat("❌ No target hit.");
                return;
            }
        }

        // ✨ Draw particles along the attack path
        double distance = eyePos.distanceTo(hitPos);
        int steps = (int) (distance * 4); // 4 particle clusters per block

        Vec3 startPos = eyePos.add(lookDir.scale(4)); // offset from camera
        Vec3 step = hitPos.subtract(startPos).scale(1.0 / steps);

        for (int i = 0; i <= steps; i++) {
            Vec3 pos = startPos.add(step.scale(i));
            level.sendParticles(
                ParticleRegistry.CUSTOM_PARTICLE.get(),
                pos.x, pos.y, pos.z,
                5,        // count per step (more = thicker beam)
                0.25,     // X spread
                0.25,     // Y spread
                0.25,     // Z spread
                0.01      // particle speed
            );
        }
    }
}
