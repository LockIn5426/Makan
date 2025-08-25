package com.example.makan.YouAreMySpecial;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SpecialKill {

    private static final double MAX_DISTANCE = 100.0;

    /**
     * Kills the entity the player is currently looking at (up to 100 blocks).
     */
    public static void killLookedAtEntity(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(MAX_DISTANCE));

        AABB box = player.getBoundingBox().expandTowards(lookVec.scale(MAX_DISTANCE)).inflate(1.0);
        List<Entity> entities = player.level().getEntities(player, box, e -> !e.isSpectator() && e.isPickable());

        AtomicReference<Entity> closest = new AtomicReference<>(null);
        AtomicReference<Double> closestDist = new AtomicReference<>(MAX_DISTANCE);

        for (Entity entity : entities) {
            AABB entityBox = entity.getBoundingBox().inflate(0.3);
            entityBox.clip(eyePos, reachVec).ifPresent(hitVec -> {
                double dist = eyePos.distanceTo(hitVec);
                if (dist < closestDist.get()) {
                    closestDist.set(dist);
                    closest.set(entity);
                }
            });
        }

        if (closest.get() == null) {
            YouAreMySpecialChat.chat(player, "❌ No entity targeted within " + (int) MAX_DISTANCE + " blocks.");
            return;
        }

        closest.get().kill(); // Instantly removes the entity
    }
}
