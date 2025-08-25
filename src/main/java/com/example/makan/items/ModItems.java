package com.example.makan.items;

import com.example.makan.Makan;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, Makan.MODID);

    public static final RegistryObject<Item> ORACLE_BONE = ITEMS.register("oracle_bone",
        () -> new Item(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<ActivatedOracleBoneItem> ACTIVATED_ORACLE_BONE = ITEMS.register("activated_oracle_bone",
    	() -> new ActivatedOracleBoneItem(new Item.Properties().stacksTo(64)));

    public static final RegistryObject<OracleBoneStaff> ORACLE_BONE_STAFF = ITEMS.register("oracle_bone_staff",
        () -> new OracleBoneStaff());

    public static final RegistryObject<Grimoire> GRIMOIRE = ITEMS.register("grimoire",
        () -> new Grimoire());


    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}