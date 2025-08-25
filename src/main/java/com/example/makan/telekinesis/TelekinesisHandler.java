package com.example.makan.telekinesis;

import com.example.makan.entities.CloneEntity;
import com.example.makan.entities.ModEntities;
import com.example.makan.network.*;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.world.phys.Vec3;

import java.util.*;

@Mod.EventBusSubscriber
public class TelekinesisHandler {

    public static TelekinesisEntity heldEntity = null; // block telekinesis
    public static CloneEntity heldClone = null;        // clone telekinesis
    public static final Set<LivingEntity> heldMobs = new HashSet<>();         // mob telekinesis

    // Server-side: per-player selected mobs (store entity IDs for simplicity)
    private static final Map<UUID, Set<Integer>> selectedMobIdsPerPlayer = new HashMap<>();

    public static Set<Integer> getOrCreateSelectedSet(ServerPlayer player) {
        return selectedMobIdsPerPlayer.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
    }

    // === CLIENT SIDE: Right click in air ===
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handleMobTelekinesisRightClickClient(event); // delegates mob logic
        // Blocks/clones logic lives below, unchanged
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // Confirm block selection => compute holdDistance locally, then tell server to grab
        if (TelekinesisClientHandler.selectingRadius) {
            TelekinesisClientHandler.selectingRadius = false;

            BlockPos center = TelekinesisClientHandler.currentTargetPos;
            if (center != null) {
                Vec3 eye = player.getEyePosition();
                float dist = (float) eye.distanceTo(Vec3.atCenterOf(center));
                dist = Math.max(2.0f, Math.min(100.0f, dist));
                TelekinesisClientHandler.holdDistance = dist;

                ModMessages.INSTANCE.sendToServer(
                        new GrabTelekinesisPacket(center, TelekinesisClientHandler.selectionRadius)
                );
            }
            return;
        }

        // === While holding blocks ===
        if (heldEntity != null) {
            if (player.isShiftKeyDown()) {
                ModMessages.INSTANCE.sendToServer(new PlaceTelekinesisPacket());
            } else {
                ModMessages.INSTANCE.sendToServer(new ThrowTelekinesisPacket());
            }
        }
        // === While holding clone ===
        else if (heldClone != null) {
            if (player.isShiftKeyDown()) {
                ModMessages.INSTANCE.sendToServer(new PlaceClonePacket());
            }
        }
        // Mob handled in handleMobTelekinesisRightClickClient
    }

    /**
     * CLIENT-SIDE helper for mob telekinesis:
     * - If holding a mob: RightClick = throw, Shift+RightClick = place
     * - If NOT holding a mob, but there are selected mobs: try to grab the one under crosshair
     */
    public static void handleMobTelekinesisRightClickClient(PlayerInteractEvent.RightClickEmpty event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // If holding mobs → throw/place all
        if (!TelekinesisHandler.heldMobs.isEmpty()) {
            if (player.isShiftKeyDown()) {
                ModMessages.INSTANCE.sendToServer(new PlaceMobPacket());
            } else {
                ModMessages.INSTANCE.sendToServer(new ThrowMobPacket());
            }
            return;
        }

        // Otherwise → grab all selected mobs
        if (!TelekinesisClientHandler.selectedMobIdsClient.isEmpty()) {
            for (int mobId : TelekinesisClientHandler.selectedMobIdsClient) {
                ModMessages.INSTANCE.sendToServer(new GrabMobPacket(mobId));
            }
        }

    }

    // === SERVER SIDE: Grab clone ===
    public static void serverGrabClone(ServerPlayer player, CloneEntity clone) {
        if (clone == null || !clone.isAlive()) return;
        heldClone = clone;
        clone.setNoAi(true);
        clone.setInvulnerable(true);
    }

    // === SERVER SIDE: Update held clone ===
    public static void serverUpdateHeldClone(ServerPlayer player) {
        if (heldClone != null && heldClone.isAlive()) {
            double distance = Math.max(2.0, Math.min(100.0, TelekinesisClientHandler.holdDistance));
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle().scale(distance);
            Vec3 target = eye.add(look);
            heldClone.teleportTo(target.x, target.y, target.z);
        }
    }

    // === SERVER SIDE: Place clone → teleport player ===
    public static void serverPlaceClone(ServerPlayer player) { 
        if (heldClone == null || !heldClone.isAlive()) return; 
        Vec3 clonePos = heldClone.position(); 
        player.teleportTo(clonePos.x, clonePos.y, clonePos.z); 
        heldClone.discard(); 
        heldClone = null; 
    }

    // === SERVER SIDE: MOB telekinesis ===
    public static void serverGrabMob(ServerPlayer player, LivingEntity mob) {
        if (mob == null || !mob.isAlive()) return;
        heldMobs.add(mob);
        // mob.setNoAi(true);
        // mob.setInvulnerable(true);
    }

    public static void serverUpdateHeldMobs(ServerPlayer player) {
        if (heldMobs.isEmpty()) return;

        double distance = Math.max(2.0, Math.min(100.0, TelekinesisClientHandler.holdDistance));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().scale(distance);
        Vec3 target = eye.add(look);

        // Spread mobs a bit so they don’t overlap (example: circle around target)
        int i = 0;
        int n = heldMobs.size();
        for (LivingEntity mob : new HashSet<>(heldMobs)) {
            if (!mob.isAlive()) {
                heldMobs.remove(mob);
                continue;
            }

            double angle = (2 * Math.PI * i) / Math.max(1, n);
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;
            mob.teleportTo(target.x + offsetX, target.y, target.z + offsetZ);
            i++;
        }
    }

    public static void serverPlaceMobs(ServerPlayer player) {
        double distance = Math.max(2.0, Math.min(100.0, TelekinesisClientHandler.holdDistance));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().scale(distance);
        Vec3 target = eye.add(look);

        int i = 0;
        int n = heldMobs.size();
        for (LivingEntity mob : new HashSet<>(heldMobs)) {
            if (!mob.isAlive()) continue;

            // Spread around player’s crosshair like when holding
            double angle = (2 * Math.PI * i) / Math.max(1, n);
            double offsetX = Math.cos(angle) * 1.5;
            double offsetZ = Math.sin(angle) * 1.5;
            Vec3 finalPos = target.add(offsetX, 0, offsetZ);

            // Snap them to the ground at that spot
            // Snap them to the ground at that spot
            BlockPos ground = player.level().getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                BlockPos.containing(finalPos)
            );

            mob.teleportTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5);


            // Stop any downward momentum from carry phase
            mob.setDeltaMovement(Vec3.ZERO);
            mob.fallDistance = 0f;

            i++;
        }

        heldMobs.clear();
    }


    public static void serverThrowMobs(ServerPlayer player) {
        for (LivingEntity mob : heldMobs) {
            if (mob.isAlive()) {
                // mob.setNoAi(false);
                // mob.setInvulnerable(false);
                mob.setDeltaMovement(player.getLookAngle().scale(2.0));
                mob.hurtMarked = true;
            }
        }
        heldMobs.clear();
    }


    // === SERVER SIDE: Grab blocks (unchanged) ===
    public static void serverGrabBlocks(ServerPlayer player, BlockPos center, int radius) {
        ServerLevel level = (ServerLevel) player.level();
        List<BlockEntry> blocksToPick = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if ((dx * dx + dy * dy + dz * dz) <= (radius * radius)) {
                        BlockPos pos = center.offset(dx, dy, dz);
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && level.getBlockEntity(pos) == null) {
                            blocksToPick.add(new BlockEntry(dx, dy, dz, state));
                        }
                    }
                }
            }
        }

        for (BlockEntry be : blocksToPick) {
            level.setBlock(center.offset(be.dx, be.dy, be.dz), Blocks.AIR.defaultBlockState(), 3);
        }

        if (!blocksToPick.isEmpty()) {
            Vec3 spawnPos = Vec3.atCenterOf(center);
            TelekinesisEntity tele = new TelekinesisEntity(ModEntities.TELEKINESIS_BLOB.get(), level, spawnPos, blocksToPick);
            tele.setOwner(player);
            level.addFreshEntity(tele);
            heldEntity = tele;

            ListTag blocksNBT = new ListTag();
            for (BlockEntry entry : blocksToPick) blocksNBT.add(entry.toNBT());

            ModMessages.INSTANCE.send(
                    PacketDistributor.TRACKING_ENTITY.with(() -> tele),
                    new TelekinesisSyncPacket(tele.getId(), blocksNBT)
            );
            ModMessages.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new TelekinesisSyncPacket(tele.getId(), blocksNBT)
            );
        }
    }

    public static void serverThrow(ServerPlayer player) {
        if (heldEntity != null && heldEntity.isAlive()) {
            heldEntity.throwEntity(player.getLookAngle(), 2.0);
            heldEntity = null;
        }
    }

    public static void serverPlaceBlocks(ServerPlayer player) {
        if (heldEntity == null || !heldEntity.isAlive()) return;

        Level level = player.level();
        double distance = Math.max(2.0, Math.min(100.0, TelekinesisClientHandler.holdDistance));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().scale(distance);
        BlockPos targetPos = new BlockPos(
                (int) Math.floor(eye.x + look.x),
                (int) Math.floor(eye.y + look.y),
                (int) Math.floor(eye.z + look.z)
        );

        for (BlockEntry entry : heldEntity.getCarriedBlocks()) {
            BlockPos placePos = targetPos.offset(entry.dx, entry.dy, entry.dz);
            level.setBlock(placePos, entry.state, 3);
        }

        heldEntity.discard();
        heldEntity = null;
    }

    // === SERVER EVENT TICK HOOK ===
    @Mod.EventBusSubscriber
    public static class ServerEvents {
        @SubscribeEvent
        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
            if (!(event.player instanceof ServerPlayer sp)) return;

            serverUpdateHeldClone(sp);
            serverUpdateHeldMobs(sp);
        }
    }
}
