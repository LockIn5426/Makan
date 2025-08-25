package com.example.makan.items;

import com.example.makan.gui.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.MinecraftForge;

public class Grimoire extends Item {
    public Grimoire() {
        super(new Item.Properties()
                .stacksTo(1)
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            GrimoireScreen screen = new GrimoireScreen();
            Minecraft.getInstance().setScreen(screen);
        }

        return InteractionResultHolder.success(stack);
    }

}