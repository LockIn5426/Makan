package com.example.makan.gamemode;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KamiKazeHandler {
    private static final Map<UUID, Boolean> launched = new ConcurrentHashMap<>();

    public static void setLaunched(ServerPlayer player, boolean value) {
        launched.put(player.getUUID(), value);
    }

    public static boolean isLaunched(ServerPlayer player) {
        return launched.getOrDefault(player.getUUID(), false);
    }

    public static void handleKamiKaze(ServerPlayer player) {
        Vec3 motion = player.getDeltaMovement();

        // Hover mode before launch so that you can move normally without explosions going on constantly.
        if (!isLaunched(player)) {
            player.setDeltaMovement(motion.x * 0.5, 0, motion.z * 0.5);
            player.fallDistance = 0;
            return;
        }

        double speed = motion.length();
        if (speed < 0.05) return; // too slow to matter

        // Predict where the player will be next tick
        AABB predictedBox = player.getBoundingBox().move(motion).inflate(0.1);

        // Check collisions
        boolean hitBlock = player.level().getBlockCollisions(player, predictedBox).iterator().hasNext();
        boolean hitEntity = !player.level().getEntities(player, predictedBox).isEmpty();

        // Determine angle of impact
        double angleDeg = Math.toDegrees(Math.acos(motion.y / speed)); 
        boolean verticalImpact = angleDeg >= 135; // coming from above

        // If ground hit with steep downward angle OR hit entity → explode
        if ((hitBlock && motion.y < -0.2 && verticalImpact) || hitEntity) {
            float explosionPower = Mth.clamp((float) speed * 4f, 2.0f, 10.0f);

            player.level().explode(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                explosionPower,
                false,
                Level.ExplosionInteraction.BLOCK
            );

            setLaunched(player, false);
            player.setDeltaMovement(Vec3.ZERO);
            return;
        }

        // If wall hit but not steep downward → bounce
        if (hitBlock) {
            player.setDeltaMovement(new Vec3(-motion.x * 0.6, motion.y, -motion.z * 0.6));
        }
    }

}
