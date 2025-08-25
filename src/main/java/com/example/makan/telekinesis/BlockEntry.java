package com.example.makan.telekinesis;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.BuiltInRegistries;

public class BlockEntry {
    public final int dx, dy, dz;
    public final BlockState state;

    public BlockEntry(int dx, int dy, int dz, BlockState state) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.state = state;
    }

    public CompoundTag toNBT() {
        CompoundTag t = new CompoundTag();
        t.putInt("dx", dx);
        t.putInt("dy", dy);
        t.putInt("dz", dz);
        t.putString("block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        return t;
    }

    public static BlockEntry fromNBT(CompoundTag t) {
        int dx = t.getInt("dx");
        int dy = t.getInt("dy");
        int dz = t.getInt("dz");
        Block block = BuiltInRegistries.BLOCK.get(new ResourceLocation(t.getString("block")));
        return new BlockEntry(dx, dy, dz, block.defaultBlockState());
    }
}
