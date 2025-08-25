package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.*;

public class SpecialZangeki {

    public static void performZangeki(ServerPlayer player, int length, int width, int height, float damage) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ No valid player or server level.");
            return;
        }

        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 endVec = eyePos.add(lookDir.scale(512));

        // 🔍 First check if player is looking at an entity
        double maxDistance = 64; // you can adjust this
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(maxDistance)).inflate(1.0);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, searchBox, entity -> 
            !entity.equals(player) && entity.isPickable());

        LivingEntity closestEntity = null;
        double closestDist = Double.MAX_VALUE;

        for (LivingEntity target : targets) {
            AABB targetBox = target.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = targetBox.clip(eyePos, endVec);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closestEntity = target;
                }
            }
        }

        Vec3 targetLocation;

        if (closestEntity != null) {
            targetLocation = closestEntity.position();
            YouAreMySpecialChat.chat("🎯 Targeted entity: " + closestEntity.getName().getString());
        } else {
            // Fall back to block targeting
            HitResult result = level.clip(new ClipContext(
                    eyePos,
                    endVec,
                    ClipContext.Block.OUTLINE,
                    ClipContext.Fluid.ANY,
                    player
            ));

            if (result.getType() != HitResult.Type.BLOCK) {
                YouAreMySpecialChat.chat("❌ No block or entity targeted.");
                return;
            }

            targetLocation = result.getLocation();
            YouAreMySpecialChat.chat("⚔️ Zangeki block slash!");
        }

        BlockPos center = BlockPos.containing(targetLocation);
        Random rand = new Random(center.asLong());
        boolean diagonal = rand.nextBoolean(); // true = right-diagonal (/), false = left-diagonal (\)

        Vec3 forward = lookDir;
        Vec3 side = new Vec3(-forward.z, 0, forward.x).normalize();
        Vec3 diag = diagonal ? side.add(forward).normalize() : side.scale(-1).add(forward).normalize();

        List<List<BlockPos>> slashSteps = new ArrayList<>();

        for (int i = 0; i < length; i++) {
            Vec3 base = Vec3.atCenterOf(center).add(diag.scale(i));
            List<BlockPos> row = new ArrayList<>();

            for (int dx = -width / 2; dx <= width / 2; dx++) {
                for (int dy = -height / 2; dy <= height / 2; dy++) {
                    Vec3 offset = base.add(side.scale(dx)).add(0, dy, 0);
                    BlockPos pos = BlockPos.containing(offset);
                    row.add(pos);
                }
            }

            slashSteps.add(row);
        }

        animateZangeki(server, level, player, slashSteps, 0, damage);
    }


    private static void animateZangeki(MinecraftServer server, ServerLevel level, ServerPlayer player, List<List<BlockPos>> steps, int step, float dmg) {
        if (step >= steps.size()) {
            YouAreMySpecialChat.chat("✅ Zangeki complete!");
            return;
        }

        List<BlockPos> currentRow = steps.get(step);

        for (BlockPos pos : currentRow) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(Blocks.BEDROCK) && !state.is(Blocks.BARRIER)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
            }

            // Damage entities
            AABB box = new AABB(pos).inflate(1.0);
            List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);
            for (LivingEntity entity : entities) {
                if (!entity.equals(player)) {
                    entity.hurt(player.damageSources().playerAttack(player), dmg);
                    level.sendParticles(ParticleTypes.SWEEP_ATTACK, entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0, 0, 0, 0);
                }
            }
        }

        // Play a sweeping sword sound per step
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.8f + (step % 3) * 0.1f);

        // Schedule next step with a delay for dramatic effect
        server.execute(() -> animateZangeki(server, level, player, steps, step + 1, dmg));
    }
}
