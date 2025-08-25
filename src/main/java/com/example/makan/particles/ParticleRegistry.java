package com.example.makan.particles;

import com.example.makan.Makan;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegistry {
    // Tell it what type it will store: SimpleParticleType
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Makan.MODID);

    public static final RegistryObject<SimpleParticleType> CUSTOM_PARTICLE =
        PARTICLE_TYPES.register("custom_particle",
            () -> new SimpleParticleType(true)); // true = always show, even at distance
}
