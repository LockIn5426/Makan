package com.example.makan.items;

import com.example.makan.Makan;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.RegistryObject;

public class MakanCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Makan.MODID);

    public static final RegistryObject<CreativeModeTab> MAKAN_TAB = CREATIVE_MODE_TABS.register("makantab",
        () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ModItems.ORACLE_BONE.get()))
            .title(Component.literal("Makan Tab")) // Tab label
            .displayItems((params, output) -> {
                output.accept(ModItems.ORACLE_BONE.get());
		        output.accept(ModItems.ACTIVATED_ORACLE_BONE.get());
                output.accept(ModItems.ORACLE_BONE_STAFF.get());
                output.accept(ModItems.GRIMOIRE.get());
                // Add more items here
            })
            .build()
    );

    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }
}
