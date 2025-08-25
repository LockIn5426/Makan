package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

import java.util.Random;

public class SpecialHorde {

    public static void spawnHorde(ServerPlayer player, int monsterCount) {
        if (player == null || player.serverLevel() == null) {
            YouAreMySpecialChat.chat("❌ No valid player or server level.");
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        Vec3 endVec = eyePos.add(lookDir.scale(128));

        HitResult result = level.clip(new ClipContext(
                eyePos,
                endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.ANY,
                player
        ));

        if (result.getType() != HitResult.Type.BLOCK) {
            YouAreMySpecialChat.chat("❌ No valid block targeted.");
            return;
        }

        BlockPos targetPos = BlockPos.containing(result.getLocation());
        YouAreMySpecialChat.chat("👹 Spawning horde at: " + targetPos);

        Random rand = new Random();

        for (int i = 0; i < monsterCount; i++) {
            // Random offset around the target position
            int x = targetPos.getX() + rand.nextInt(20) - 10;
            int y = targetPos.getY();
            int z = targetPos.getZ() + rand.nextInt(20) - 10;

            BlockPos spawnPos = new BlockPos(x, y, z);

            // Choose a random monster type
            EntityType<? extends Mob> monsterType = getRandomMonsterType(rand);

            if (monsterType != null) {
                Mob mob = monsterType.create(level);
                if (mob != null) {
                    mob.moveTo(spawnPos, rand.nextFloat() * 360F, 0);
                    level.addFreshEntityWithPassengers(mob);
                }
            }
        }

        YouAreMySpecialChat.chat("✅ Spawned " + monsterCount + " monsters.");
    }

    private static EntityType<? extends Mob> getRandomMonsterType(Random rand) {
        // You can add or remove entity types here as needed
        EntityType<? extends Mob>[] options = new EntityType[]{
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.SPIDER,
                EntityType.CREEPER,
                EntityType.HUSK,
                EntityType.STRAY
        };
        return options[rand.nextInt(options.length)];
    }
}
