package com.example.makan;

import com.example.makan.YouAreMySpecial.*;

import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import com.example.makan.gamemode.*;
import com.example.makan.items.*;
import com.example.makan.telekinesis.*;

@Mod.EventBusSubscriber(modid = Makan.MODID)
public class ServerEvents {

    // Staff gains 1 durability per N ticks
    private static final int CHARGE_INTERVAL_TICKS = 20; // configurable
    // Scan radius for dropped items around players
    private static final double DROPPED_ITEM_SCAN_RADIUS = 5.0;

    @SubscribeEvent
    public static void onMobDrop(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;

        Level level = event.getEntity().level();
        if (level.isClientSide) return;

        // 37% drop chance
        double dropChance = 0.37;

        if (level.random.nextDouble() < dropChance) {
            ItemStack drop = new ItemStack(ModItems.ORACLE_BONE.get());

            event.getDrops().add(new ItemEntity(
                level,
                event.getEntity().getX(),
                event.getEntity().getY(),
                event.getEntity().getZ(),
                drop
            ));
        }
    }


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
    
        player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
        if (cap.getMode() == CustomGameMode.KAJIN) {
            KajinFlightHandler.handleFlight(player);
            KajinFlightHandler.passiveEffects(player);
        } else if (cap.getMode() == CustomGameMode.KAMIKAZE) {
            KamiKazeHandler.handleKamiKaze(player);
        } else if (cap.getMode() == CustomGameMode.MOGURA) {
            MoguraHandler.handle(player);
        } else if (cap.getMode() == CustomGameMode.NENDOU) {
            if (TelekinesisHandler.heldClone != null && TelekinesisHandler.heldClone.isAlive()) {
                ServerPlayer owner = player.getServer().getPlayerList()
                    .getPlayers()
                    .stream()
                    .filter(p -> p.getUUID().equals(TelekinesisHandler.heldClone.getOwnerUUID()))
                    .findFirst()
                    .orElse(null);



                if (owner != null) {
                    TelekinesisHandler.serverUpdateHeldClone(owner);
                }
            }
        }else {
            // Only clear keys if we actually had them stored
            if (KajinFlightHandler.hasKeys(player)) {
                KajinFlightHandler.clearKeys(player);
            }
        }
    });
        
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(GameModeCapability.INSTANCE).ifPresent(cap -> {
                if (cap.getMode() == CustomGameMode.KAMIKAZE) {
                    DamageSource source = event.getSource();
                    if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FALL)) {
                        event.setCanceled(true);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SpecialDeathTeleport.recordDeath(player);
        }
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide()) return;

        long gameTime = event.level.getGameTime();

        for (Player player : event.level.players()) {

            // --- Held staff charging if player is on fire/lava ---
            boolean playerOnFire = player.isOnFire() || player.isInLava();
            if (playerOnFire) {
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack held = player.getItemInHand(hand);
                    if (held.getItem() instanceof OracleBoneStaff) {
                        if (gameTime % CHARGE_INTERVAL_TICKS == 0) {
                            held.setDamageValue(Math.max(0, held.getDamageValue() - 1));
                        }
                    }
                }
            }

            // --- Scan nearby dropped items ---
            AABB scanBox = player.getBoundingBox().inflate(DROPPED_ITEM_SCAN_RADIUS);
            for (ItemEntity itemEntity : event.level.getEntitiesOfClass(ItemEntity.class, scanBox)) {
                ItemStack stack = itemEntity.getItem();
                Item item = stack.getItem();

                BlockPos itemPos = itemEntity.blockPosition(); // position of item
                boolean inFire = event.level.getBlockState(itemPos).is(Blocks.FIRE) ||
                                event.level.getBlockState(itemPos).is(Blocks.LAVA);

                if (!inFire) continue;

                // Charge dropped OracleBoneStaff using NBT
                if (item instanceof OracleBoneStaff) {
                    stack.getOrCreateTag(); // ensure NBT exists
                    int chargeTicks = stack.getOrCreateTag().getInt("chargeTicks");
                    chargeTicks++;
                    if (chargeTicks >= CHARGE_INTERVAL_TICKS) {
                        stack.setDamageValue(Math.max(0, stack.getDamageValue() - 1));
                        chargeTicks = 0;
                    }
                    stack.getTag().putInt("chargeTicks", chargeTicks);
                }
                // Transform Oracle Bones to Activated Oracle Bones (preserve stack count)
                else if (stack.is(ModItems.ORACLE_BONE.get())) {
                    int count = stack.getCount();
                    itemEntity.discard();
                    event.level.addFreshEntity(new ItemEntity(
                            event.level,
                            itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                            new ItemStack(ModItems.ACTIVATED_ORACLE_BONE.get(), count)
                    ));
                }
            }
        }
    }


}
