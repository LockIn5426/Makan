package com.example.makan.gamemode;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class KajinAbilities implements AbilityHandler {

    @Override
    public void ability1(ServerPlayer player) {
        // Fireball
        Vec3 look = player.getLookAngle();
        LargeFireball fireball = new LargeFireball(
            player.level(),
            player,
            look.x, look.y, look.z,
            3 // Explosion power
        );
        fireball.setPos(
            player.getX() + look.x * 2,
            player.getEyeY() + look.y * 2,
            player.getZ() + look.z * 2
        );
        player.level().addFreshEntity(fireball);
    }

    @Override
    public void ability2(ServerPlayer player) {
        // Firebeam (particles + damage in line)
        ServerLevel level = (ServerLevel) player.level();
        Vec3 start = player.getEyePosition();
        Vec3 dir = player.getLookAngle();
        for (int i = 4; i < 20; i++) {
            Vec3 pos = start.add(dir.scale(i));
            level.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 5, 0, 0, 0, 0.01);
            level.getEntities(player, player.getBoundingBox().inflate(20))
                 .forEach(e -> {
                     if (e != player && e.getBoundingBox().inflate(0.5).contains(pos)) {
                         e.setSecondsOnFire(4);
                         e.hurt(player.damageSources().magic(), 6f);
                     }
                 });
        }
    }

    @Override
    public void ability3(ServerPlayer player) {
        // Magma pool
        ServerLevel level = (ServerLevel) player.level();
        int radius = 3;
        Vec3 pos = player.position();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    level.setBlockAndUpdate(player.blockPosition().offset(x, -1, z), Blocks.LAVA.defaultBlockState());
                    //level.setBlockAndUpdate(player.blockPosition().offset(x, 0, z), Blocks.FIRE.defaultBlockState());
                }
            }
        }
    }
}
