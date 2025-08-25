package com.example.makan.items;

import com.example.makan.gui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class OracleBoneStaff extends Item {

    public OracleBoneStaff() {
        super(new Item.Properties()
                .stacksTo(1)           // Not stackable
                .durability(250)       // Durability value
        );
    }

    @Override
    public boolean isFireResistant() {
        return true; // Cannot burn in fire/lava
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 🔒 If fully depleted, do nothing
        if (stack.getDamageValue() >= stack.getMaxDamage()) {
            if (level.isClientSide()) {
                player.displayClientMessage(Component.literal("The staff is depleted and needs recharging."), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new DrawingScreen());
        } else {
            // Increase damage but don’t destroy
            int newDamage = Math.min(stack.getDamageValue() + 1, stack.getMaxDamage());
            stack.setDamageValue(newDamage);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        if (remaining <= 0) {
            tooltip.add(Component.literal("Charge: Depleted"));
        } else {
            tooltip.add(Component.literal("Charge: " + remaining + "/" + stack.getMaxDamage()));
        }
    }
}
