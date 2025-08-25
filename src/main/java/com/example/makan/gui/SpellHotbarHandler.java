package com.example.makan.gui;

import com.example.makan.Makan;
import com.example.makan.KeyBinding;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Makan.MODID, value = Dist.CLIENT)
public class SpellHotbarHandler {

    // Scroll textures
    private static final ResourceLocation SCROLL_TEXTURE = new ResourceLocation(Makan.MODID, "textures/gui/scroll.png");
    private static final ResourceLocation SELECTED_SCROLL_TEXTURE = new ResourceLocation(Makan.MODID, "textures/gui/selected_scroll.png");

    // Sizing constants
    private static final int HOTBAR_SLOT_SIZE = 24;
    private static final int SCROLL_WIDTH = 32;
    private static final int SCROLL_HEIGHT = 64;
    private static final int KANJI_VERTICAL_SPACING = 10;

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (KeyBinding.NEXT_SLOT_KEY.isDown() && Minecraft.getInstance().player != null) {
            Makan.selectedSlot = (Makan.selectedSlot + 1) % Makan.storedKanjiSlots.size();
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        GuiGraphics gui = event.getGuiGraphics();
        PoseStack poseStack = gui.pose();
        Font font = mc.font;

        // Draw custom hotbar
        int x = mc.getWindow().getGuiScaledWidth() / 2 - ((HOTBAR_SLOT_SIZE * Makan.storedKanjiSlots.size()) / 2);
        int y = mc.getWindow().getGuiScaledHeight() - HOTBAR_SLOT_SIZE - 4;

        for (int i = 0; i < Makan.storedKanjiSlots.size(); i++) {
            String display = Makan.storedKanjiSlots.get(i).isEmpty() ? "-" : "*";
            int textColor = (i == Makan.selectedSlot) ? 0xFFFFAA00 : 0xFFFFFFFF;

            gui.drawCenteredString(
                font,
                display,
                x + (i * HOTBAR_SLOT_SIZE) + HOTBAR_SLOT_SIZE / 2,
                y + (HOTBAR_SLOT_SIZE / 2) - 40,
                textColor
            );
        }

        // Draw scrolls in reverse order (slot 0 is rightmost)
        int startX = mc.getWindow().getGuiScaledWidth() - SCROLL_WIDTH - 8;
        int startY = 8;

        for (int i = 0; i < Makan.storedKanjiSlots.size(); i++) {
            String kanjiStr = Makan.storedKanjiSlots.get(i);
            if (kanjiStr.isEmpty()) continue;

            // Reverse placement
            int scrollX = startX - ((Makan.storedKanjiSlots.size() - 1 - i) * (SCROLL_WIDTH + 4));
            int scrollY = startY;

            // Use selected scroll texture if this is the selected slot
            ResourceLocation texture = (i == Makan.selectedSlot) ? SELECTED_SCROLL_TEXTURE : SCROLL_TEXTURE;

            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, texture);

            // Draw scroll background
            gui.blit(texture, scrollX, scrollY, 0, 0, SCROLL_WIDTH, SCROLL_HEIGHT, SCROLL_WIDTH, SCROLL_HEIGHT);

            // Draw kanji vertically down the scroll
            int textX = scrollX + SCROLL_WIDTH / 2 - font.width("あ") / 2;
            int textY = scrollY + 12;
            for (char c : kanjiStr.toCharArray()) {
                gui.drawString(font, String.valueOf(c), textX, textY, 0x000000);
                textY += KANJI_VERTICAL_SPACING;
            }
        }
    }
}
