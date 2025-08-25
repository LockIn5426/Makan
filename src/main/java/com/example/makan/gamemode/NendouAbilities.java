package com.example.makan.gamemode;

import com.example.makan.entities.CloneEntity;
import com.example.makan.entities.ModEntities;
import com.example.makan.network.*;
import com.example.makan.telekinesis.*;

import net.minecraftforge.network.PacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NendouAbilities implements AbilityHandler {

    @Override
    public void ability1(ServerPlayer player) {
        TelekinesisClientHandler.selectingRadius = !TelekinesisClientHandler.selectingRadius;

        if (TelekinesisClientHandler.selectingRadius) {
            if (TelekinesisClientHandler.selectionRadius < 1) {
                TelekinesisClientHandler.selectionRadius = 3;
            }
            player.sendSystemMessage(Component.literal("Telekinesis selection started. Scroll to set radius, right-click to pick up."));
        } else {
            player.sendSystemMessage(Component.literal("Telekinesis selection canceled."));
        }
    }

    @Override
    public void ability2(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        double reach = 50.0;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(reach));

        // Search for entities along ray
        AABB searchBox = new AABB(eye, end).inflate(1.5);
        LivingEntity best = null;
        double bestDistSq = reach * reach;

        for (LivingEntity le : player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != player)) {

            // Ray vs AABB check
            var box = le.getBoundingBox().inflate(0.3); // padding for easier targeting
            var hitOpt = box.clip(eye, end);

            if (hitOpt.isPresent()) {
                double distSq = eye.distanceToSqr(hitOpt.get());
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = le;
                }
            }
        }

        if (best == null) {
            player.sendSystemMessage(Component.literal("No mob in sight to toggle."));
            return;
        }

        var set = TelekinesisHandler.getOrCreateSelectedSet(player);
        if (set.contains(best.getId())) {
            set.remove(best.getId());
            player.sendSystemMessage(Component.literal("Deselected: " + best.getName().getString()));
        } else {
            set.add(best.getId());
            player.sendSystemMessage(Component.literal("Selected: " + best.getName().getString()));
        }

        // Sync the full selection list to this client
        ModMessages.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SelectedMobsSyncPacket(set)
        );
    }


    @Override
    public void ability3(ServerPlayer player) {
        if (player.level().isClientSide()) return;

        ServerLevel world = (ServerLevel) player.level();

        // Spawn clone
        CloneEntity clone = new CloneEntity(ModEntities.CLONE.get(), world);
        clone.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        clone.setOwnerUUID(player.getUUID());
        clone.tame(player);
        clone.setPersistenceRequired();
        clone.finalizeSpawn(world, world.getCurrentDifficultyAt(clone.blockPosition()), MobSpawnType.EVENT, null, null);
        world.addFreshEntity(clone);

        player.sendSystemMessage(Component.literal("Clone spawned!"));

        // Default mob/clone hold distance and grab clone immediately
        TelekinesisClientHandler.holdDistance = 4.0f;
        TelekinesisHandler.serverGrabClone(player, clone);
    }
}
