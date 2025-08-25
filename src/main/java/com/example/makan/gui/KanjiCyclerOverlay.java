package com.example.makan.gui;

import com.example.makan.Makan;
import com.example.makan.KeyBinding;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

public class KanjiCyclerOverlay extends Screen {
    private int currentIndex = 0;
    private static final ResourceLocation SLOT_TEXTURE = new ResourceLocation("textures/gui/container/inventory.png");

    public KanjiCyclerOverlay() {
        super(Component.literal("Kanji Selector"));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        if (!DrawingScreen.lastMatches.isEmpty()) {
            String currentKanji = DrawingScreen.lastMatches.get(currentIndex).kanji;

            int centerX = width / 2;
            int centerY = height / 2;

            // Draw inventory slot
            guiGraphics.blit(SLOT_TEXTURE, centerX - 9, centerY - 9, 7, 83, 18, 18);

            // Draw kanji in center of slot
            guiGraphics.drawCenteredString(this.font, currentKanji, centerX, centerY - 4, 0xFFFFFF);

        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBinding.CYCLING_KEY.matches(keyCode, scanCode)) {
            if (!DrawingScreen.lastMatches.isEmpty()) {
                currentIndex = (currentIndex + 1) % DrawingScreen.lastMatches.size();

                if (!Makan.storedKanjiSlots.get(Makan.selectedSlot).isEmpty()) {
                    String current = Makan.storedKanjiSlots.get(Makan.selectedSlot);
                    current = current.substring(0, current.length() - 1) 
                            + DrawingScreen.lastMatches.get(currentIndex).kanji;
                    Makan.storedKanjiSlots.set(Makan.selectedSlot, current);
                }
                    /*
                } else {
                    // If slot is empty, just set it to the new kanji
                    Makan.storedKanjiSlots.set(Makan.selectedSlot, DrawingScreen.lastMatches.get(currentIndex).kanji);
                }
                    */
            }
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 || button == 1) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
