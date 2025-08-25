package com.example.makan.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class GodzillaStompProcedure {

    public static void execute(LevelAccessor world, double x, double y, double z, LivingEntity godzilla) {
        if (!(world instanceof ServerLevel level) || godzilla == null) return;

        // Center position of stomp
        BlockPos center = BlockPos.containing(x, y, z);
        int radius = 20;   // stomp blast radius
        int width = 12;     // trench width
        int height = 30;    // vertical clearance
        float damage = 30; // damage dealt to entities
        int minYLimit = 50;

        MinecraftServer server = level.getServer();
        List<List<BlockPos>> radialSteps = new ArrayList<>();

        // Build radial rings of destruction
        for (int i = 0; i < radius; i++) {
            double angleStep = (2 * Math.PI) / (i * 8 + 8);
            List<BlockPos> ring = new ArrayList<>();

            for (double angle = 0; angle < 2 * Math.PI; angle += angleStep) {
                Vec3 base = Vec3.atCenterOf(center).add(
                        Math.cos(angle) * i,
                        0,
                        Math.sin(angle) * i
                );

                for (int dx = -width / 2; dx <= width / 2; dx++) {
                    for (int dy = -height / 2; dy <= height / 2; dy++) {
                        Vec3 offset = base.add(
                                Math.cos(angle + Math.PI / 2) * dx,
                                dy,
                                Math.sin(angle + Math.PI / 2) * dx
                        );
                        BlockPos pos = BlockPos.containing(offset);
                        if (pos.getY() >= minYLimit) {
                            ring.add(pos);
                        }
                    }
                }
            }

            radialSteps.add(ring);
        }

        // Start animation sequence
        animateRadialStomp(server, level, godzilla, radialSteps, 0, damage);
    }

    private static void animateRadialStomp(MinecraftServer server, ServerLevel level, LivingEntity godzilla,
                                           List<List<BlockPos>> steps, int step, float damage) {
        if (step >= steps.size()) return;

        List<BlockPos> currentRow = steps.get(step);

        for (BlockPos pos : currentRow) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER) && !state.is(Blocks.COMMAND_BLOCK)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            }

            // Only damage entities every 2 steps
            if (step % 2 == 0) {
                AABB box = new AABB(pos).inflate(1.5);
                List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != godzilla && e.isAlive());

                for (LivingEntity entity : entities) {
                    DamageSource src = level.damageSources().mobAttack(godzilla);
                    entity.hurt(src, damage);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }

        // Roar sound each wave
        level.playSound(null, godzilla.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL,
                SoundSource.HOSTILE, 3.0f, 0.8f + (step % 3) * 0.1f);

        // Schedule next wave
        server.execute(() -> animateRadialStomp(server, level, godzilla, steps, step + 1, damage));
    }
}
