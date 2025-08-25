package com.example.makan.gamemode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;


public class KamikazeAbilities implements AbilityHandler {    

    // === Ability 1: Levitation (all living entities including players/modded mobs) ===
    @Override
    public void ability1(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(50),
                e -> e != player && e.isAlive());

        for (Entity entity : targets) {
            if (entity instanceof LivingEntity living) {
                living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 100, 10, true, true));
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 2.0f, 1.0f);
    }

    // Position lock ability 
    @Override
    public void ability2(ServerPlayer player) {
        // Lock position — keep them exactly where they started (no movement)
        KamiKazeHandler.setLaunched(player, false);
        player.setDeltaMovement(0, 0, 0);
        player.hurtMarked = true;
    }


    // === Ability 3: Ninjago Airjitzu ===
    @Override
    public void ability3(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        // Keep player 3 blocks above ground
        BlockPos groundPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, player.blockPosition());
        double targetY = groundPos.getY() + 3;
        player.setPos(player.getX(), targetY, player.getZ());

        double radius = 7.0;

        // Particle ring
        for (int i = 0; i < 60; i++) { // more points for smoother circle
            double angle = i * (Math.PI * 2 / 60);
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            level.sendParticles(ParticleTypes.SMOKE, player.getX() + x, player.getY(), player.getZ() + z, 1, 0, 0, 0, 0);
        }

        // Damage all mobs inside radius (not just near player hitbox)
        List<LivingEntity> victims = level.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(radius),
            e -> e != player && e.isAlive()
        );

        for (LivingEntity e : victims) {
            double distSq = e.distanceToSqr(player); // squared distance for efficiency
            if (distSq <= radius * radius) {
                e.hurt(player.damageSources().magic(), 6f);
            }
        }

        // Play sweep sound
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }



}

