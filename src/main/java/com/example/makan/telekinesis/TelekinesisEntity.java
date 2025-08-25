package com.example.makan.telekinesis;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;


import java.util.ArrayList;
import java.util.List;

public class TelekinesisEntity extends Entity {
    private final List<BlockEntry> carriedBlocks = new ArrayList<>();
    private boolean thrown = false;
    private Entity owner;

    public TelekinesisEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public TelekinesisEntity(EntityType<?> type, Level level, Vec3 pos, List<BlockEntry> blocks) {
        this(type, level);
        this.setPos(pos);
        this.carriedBlocks.addAll(blocks);
    }

    public void setCarriedBlocksFromNBT(ListTag blocksNBT) {
        carriedBlocks.clear();
        for (int i = 0; i < blocksNBT.size(); i++) {
            carriedBlocks.add(BlockEntry.fromNBT(blocksNBT.getCompound(i)));
        }
    }

    public void setOwner(Entity owner) {
        this.owner = owner;
    }

    public Entity getOwner() {
        return this.owner;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double size = this.getBoundingBox().getSize();
        size = size * 16.0 * 64.0; // scale with size
        return distance < size * size;
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        thrown = tag.getBoolean("Thrown");
        carriedBlocks.clear();
        ListTag blocksTag = tag.getList("Blocks", 10);
        for (int i = 0; i < blocksTag.size(); i++) {
            carriedBlocks.add(BlockEntry.fromNBT(blocksTag.getCompound(i)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putBoolean("Thrown", thrown);
        ListTag blocksTag = new ListTag();
        for (BlockEntry entry : carriedBlocks) {
            blocksTag.add(entry.toNBT());
        }
        tag.put("Blocks", blocksTag);
    }

    @Override
    public void tick() {
        super.tick();

        if (!thrown && this.owner != null) {
            // Float in front of the telekinetic owner
            Vec3 look = this.owner.getLookAngle();
            Vec3 target = this.owner.position()
                    .add(0, 2, 0)
                    .add(look.scale(TelekinesisClientHandler.holdDistance));
            Vec3 motion = target.subtract(this.position()).scale(0.3);
            this.setDeltaMovement(motion);
        } else if (thrown) {
            // Gravity + air drag
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98).add(0, -0.05, 0));

            // --- Collision check (block + entity) ---
            Vec3 start = this.position();
            Vec3 end = start.add(this.getDeltaMovement());

            // Block collision raytrace
            HitResult blockHit = this.level().clip(new ClipContext(
                    start, end,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    this
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                this.onHit(blockHit);
            }

            // Entity collision raytrace
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    this.level(), this, start, end,
                    this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0),
                    e -> e.isPickable() && e != this.owner
            );

            if (entityHit != null) {
                this.onHit(entityHit);
            }
        }

        // Apply motion
        this.move(MoverType.SELF, this.getDeltaMovement());
    }


    public void throwEntity(Vec3 direction, double speed) {
        this.thrown = true;
        this.setDeltaMovement(direction.normalize().scale(speed));
    }

    private void onHit(HitResult result) {
        if (!thrown || level().isClientSide) return;

        switch (result.getType()) {
            case BLOCK -> explode();
            case ENTITY -> explode();
            default -> {}
        }
    }

    private void explode() {
        if (!level().isClientSide) {
            int blockCount = this.carriedBlocks.size();

            // Explosion strength scales with block count
            // Example: every 5 blocks = +1 power, minimum 1, maximum 12
            float power = Math.min(12.0f, Math.max(1.0f, blockCount / 5.0f));

            level().explode(
                    this,
                    this.getX(), this.getY(), this.getZ(),
                    power,
                    Level.ExplosionInteraction.BLOCK // use NONE if you don't want terrain damage
            );

            this.discard();
        }
    }

    public List<BlockEntry> getCarriedBlocks() {
        return carriedBlocks;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
