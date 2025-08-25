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

public class ActivatedOracleBoneItem extends Item {
    public ActivatedOracleBoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            DrawingScreen screen = new DrawingScreen();
            Minecraft.getInstance().setScreen(screen);
        } else {
            // Decrease item count by 1 on the server side
            stack.shrink(1);
        }

        return InteractionResultHolder.success(stack);
    }

}