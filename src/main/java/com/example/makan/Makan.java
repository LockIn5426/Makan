package com.example.makan;

import com.example.makan.items.*;
import com.example.makan.network.*;
import com.example.makan.gui.*;
import com.example.makan.particles.*;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.example.makan.entities.ModEntities;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod(Makan.MODID)
public class Makan {
    public static final String MODID = "makan";

    // Now a list of strings for the 3 custom spell slots
    public static final List<String> storedKanjiSlots = new ArrayList<>(3);

    // Current selected slot index
    public static int selectedSlot = 0;

    @SuppressWarnings("removal")
    public Makan() {
        MinecraftForge.EVENT_BUS.register(this);

        // Initialize the slots with empty strings
        for (int i = 0; i < 3; i++) {
            storedKanjiSlots.add("");
        }
        
        var bus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(bus);
        MakanCreativeTabs.register(bus);
        ModMessages.register();
        ModEntities.register(bus);
        ParticleRegistry.PARTICLE_TYPES.register(bus);

        // Register our keybind and overlay handler
        MinecraftForge.EVENT_BUS.register(new SpellHotbarHandler());

        // ✅ Add client setup listener to load kanji strokes
        bus.addListener(this::clientSetup);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        // This runs once after client resources are initialized
        Minecraft.getInstance().execute(() -> {
            KanjiStrokesCache.get().loadAllStrokes();
            System.out.println("✅ Kanji strokes loaded in client setup");
        });
    }
}
