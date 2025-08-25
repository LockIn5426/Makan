package com.example.makan.entities;

import com.example.makan.Makan;
import com.example.makan.entities.GodzillaEntity;
import com.example.makan.telekinesis.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.resources.ResourceLocation;


public class ModEntities {

        public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
                DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Makan.MODID);

        public static final RegistryObject<EntityType<GodzillaEntity>> GODZILLA =
                ENTITY_TYPES.register("godzilla",
                        () -> EntityType.Builder.of(GodzillaEntity::new, MobCategory.MONSTER)
                                .sized(5.0f, 10.0f) // Width, Height
                                .build(Makan.MODID + ":godzilla"));

        public static final RegistryObject<EntityType<TelekinesisEntity>> TELEKINESIS_BLOB =
                ENTITY_TYPES.register("telekinesis_blob", () ->
                        EntityType.Builder.<TelekinesisEntity>of(TelekinesisEntity::new, MobCategory.MISC)
                                .sized(1.0f, 1.0f)
                                .build("telekinesis_blob"));

        public static final RegistryObject<EntityType<CloneEntity>> CLONE =
            ENTITY_TYPES.register("clone",
                    () -> EntityType.Builder.<CloneEntity>of(CloneEntity::new, MobCategory.MONSTER)
                            .sized(0.6f, 1.8f)
                            .setTrackingRange(64)
                            .setUpdateInterval(3)
                            .build("clone"));




        public static void register(IEventBus eventBus) {
                ENTITY_TYPES.register(eventBus);
        }
}
