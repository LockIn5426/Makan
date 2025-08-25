package com.example.makan.entities;

import com.example.makan.Makan;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Makan.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityEvents {

    @SubscribeEvent
    public static void onEntityAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.GODZILLA.get(), GodzillaEntity.createAttributes().build());
        event.put(ModEntities.CLONE.get(), CloneEntity.createAttributes().build());
    }
}
