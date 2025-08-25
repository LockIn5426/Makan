package com.example.makan.entities;

import com.example.makan.items.ModItems;
import com.example.makan.procedures.GodzillaStompProcedure;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityDimensions;

import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.RawAnimation;
import net.minecraft.client.Minecraft;


public class GodzillaEntity extends Monster implements GeoAnimatable {
    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);

    public GodzillaEntity(EntityType<? extends Monster> type, Level world) {
        super(type, world);
        this.xpReward = 5000;
        this.setMaxUpStep(2.0F);
        this.setPersistenceRequired();
        this.setNoAi(false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            //return state.setAndContinue(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                return state.setAndContinue(RawAnimation.begin().then("walk", Animation.LoopType.LOOP));
            } else {
                return state.setAndContinue(RawAnimation.begin().then("idle", Animation.LoopType.LOOP));
            }
        }));
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);

        if (!this.level().isClientSide) {
            // Guaranteed drop of 1 Oracle Bone Staff
            this.spawnAtLocation(new ItemStack(ModItems.ORACLE_BONE_STAFF.get()));
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions) {
        return 35.0f; // roughly half of the height, adjust as needed
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object entity) {
        return this.tickCount + Minecraft.getInstance().getFrameTime();
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, true));
    }

    @Override
    public MobType getMobType() {
        return MobType.UNDEFINED; // instead of MONSTER
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // stays loaded
    }

    // ---------------- Sounds ----------------
    @Override
    public SoundEvent getAmbientSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.ender_dragon.growl"));
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.iron_golem.step")), 1.0F, 0.9F);
    }

    @Override
    public SoundEvent getHurtSound(DamageSource ds) {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.ender_dragon.hurt"));
    }

    @Override
    public SoundEvent getDeathSound() {
        return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("minecraft:entity.ender_dragon.death"));
    }

    // ---------------- Combat ----------------
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL)) return false; // ignore suffocation
        return super.hurt(source, amount);
    }

    // ---------------- Tick (stomp trigger) ----------------
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // Trigger stomp every 200 ticks (~10s)
            if (this.tickCount % 200 == 0) {
                GodzillaStompProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
            }
        }
    }

    // ---------------- Attributes ----------------
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ATTACK_DAMAGE, 50.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 4.0D)
                .add(Attributes.ARMOR, 20.0D);
    }
}

