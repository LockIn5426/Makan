package com.example.makan.gamemode;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class GameModeCapability implements INBTSerializable<CompoundTag> {
    public static final Capability<GameModeCapability> INSTANCE = 
        CapabilityManager.get(new CapabilityToken<>() {});

    private CustomGameMode mode = CustomGameMode.SURVIVAL_LIKE;

    public CustomGameMode getMode() {
        return mode;
    }

    public void setMode(CustomGameMode mode) {
        this.mode = mode;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Mode", mode.name());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        if (nbt.contains("Mode")) {
            this.mode = CustomGameMode.valueOf(nbt.getString("Mode"));
        }
    }
}
