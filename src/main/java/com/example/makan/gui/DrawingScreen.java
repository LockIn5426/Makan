package com.example.makan.gui;

import com.example.makan.network.*;
import com.example.makan.Makan;
import com.example.makan.KeyBinding;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import com.google.gson.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;

import java.awt.geom.Point2D;
import java.util.*;

public class DrawingScreen extends Screen {
    private List<List<Point2D.Float>> strokes = new ArrayList<>();
    private List<Point2D.Float> currentStroke = null;

    private long screenOpenedTime;
    private long lastInteractionTime = System.currentTimeMillis();
    private static final long INACTIVITY_THRESHOLD = 5000;

    private static Map<String, KanjiCommand> kanjiCommandMap = new HashMap<>();

    public static List<KanjiMatcher.MatchResult> lastMatches = new ArrayList<>();

    public DrawingScreen() {
        super(Component.literal("Draw on Oracle Bone"));
        screenOpenedTime = System.currentTimeMillis();
        kanjiCommandMap = KanjiCommandLoader.loadCommands(); // now stores KanjiCommand
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int lineThickness = 5;
        int outlineThickness = lineThickness + 4;

        for (List<Point2D.Float> stroke : strokes) {
            for (int i = 1; i < stroke.size(); i++) {
                Point2D.Float p1 = stroke.get(i - 1);
                Point2D.Float p2 = stroke.get(i);
                drawLine(guiGraphics, p1.x, p1.y, p2.x, p2.y, 0xFFFFFFFF, outlineThickness);
                drawLine(guiGraphics, p1.x, p1.y, p2.x, p2.y, 0xFF000000, lineThickness);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawLine(GuiGraphics g, float x1, float y1, float x2, float y2, int color, float thickness) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;

        dx /= len;
        dy /= len;

        float px = -dy * (thickness / 2);
        float py = dx * (thickness / 2);

        float x1a = x1 + px, y1a = y1 + py;
        float x1b = x1 - px, y1b = y1 - py;
        float x2a = x2 + px, y2a = y2 + py;
        float x2b = x2 - px, y2b = y2 - py;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float gCol = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        buffer.vertex(x1a, y1a, 0).color(r, gCol, b, a).endVertex();
        buffer.vertex(x2a, y2a, 0).color(r, gCol, b, a).endVertex();
        buffer.vertex(x2b, y2b, 0).color(r, gCol, b, a).endVertex();
        buffer.vertex(x1b, y1b, 0).color(r, gCol, b, a).endVertex();

        tesselator.end();
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            currentStroke = new ArrayList<>();
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            strokes.add(currentStroke);
            resetInactivityTimer();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (currentStroke != null && button == 0) {
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            resetInactivityTimer();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && currentStroke != null) {
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            currentStroke = null;
            resetInactivityTimer();
            return true;
        } else if (button == 1) {
            long now = System.currentTimeMillis();
            if (now - screenOpenedTime < 200 || strokes.isEmpty()) {
                return true;
            }
            processStrokes();
            onClose();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void resetInactivityTimer() {
        lastInteractionTime = System.currentTimeMillis();
    }

    private void turnPlayer(float yawDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.setYRot(mc.player.getYRot() + yawDelta);
            mc.player.setYHeadRot(mc.player.getYRot());
            mc.player.setYBodyRot(mc.player.getYRot());
        }
    }

    public void processStrokes() {
        try {
            List<KanjiMatcher.KanjiEntry> db = KanjiMatcher.loadKanjiDatabase();
            List<float[]> simplified = KanjiMatcher.simplifyStrokes(strokes);
            List<KanjiMatcher.MatchResult> matches = KanjiMatcher.findMatches(simplified, db);

            lastMatches = matches.subList(0, Math.min(matches.size(), 5));

            if (!lastMatches.isEmpty()) {
                KanjiMatcher.MatchResult top = lastMatches.get(0);
                Makan.storedKanjiSlots.set(
                    Makan.selectedSlot,
                    Makan.storedKanjiSlots.get(Makan.selectedSlot) + top.kanji
                );
                chat("✅ Recognized: " + top.kanji + " (Score: " + String.format("%.2f", top.score) + ")");
            } else {
                chat("❌ No match found.");
            }
        } catch (Exception e) {
            chat("⚠ Error matching strokes: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public static void cast() {
        String stored = Makan.storedKanjiSlots.get(Makan.selectedSlot);
        if (stored == null || stored.isEmpty()) {
            chat("⚠ No kanji stored.");
            return;
        }

        // 🔧 Canonicalize the stored string exactly like loader canonicalized keys
        String kanjiString = KanjiCommandLoader.canon(stored);

        Map<String, KanjiCommand> commands = KanjiCommandLoader.loadCommands();

        KanjiCommand entry = null;
        String matchedKey = null;

        // Longest-match search on canonical data
        for (String key : commands.keySet()) {
            //chat(key);
            if (kanjiString.endsWith(key)) {
                if (matchedKey == null || key.length() > matchedKey.length()) {
                    matchedKey = key;
                    entry = commands.get(key);
                }
            }
        }

        if (entry == null) {
            chat("⚠ No valid mapping for: " + kanjiString);
            return;
        }

        int requiredBones = entry.cost;
        String command = entry.command;
        boolean isSpecial = command.startsWith("@Special:");

        try {
            ModMessages.INSTANCE.sendToServer(
                new ModMessages.ExecuteSpecialCommandPacket(command, requiredBones, isSpecial)
            );
            chat("✅ Executed command for: " + matchedKey);
        } catch (Exception e) {
            chat("⚠ Error parsing command for 「" + matchedKey + "」: " + e.getMessage());
            e.printStackTrace();
        }

        Makan.storedKanjiSlots.set(Makan.selectedSlot, "");
    }




    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (KeyBinding.CASTING_KEY.matches(keyCode, scanCode)) {
            chat("CAST key pressed in screen!");
            processStrokes();
            cast();
            Makan.storedKanjiSlots.set(Makan.selectedSlot, "");
            onClose();
            return true;
        }

        if (KeyBinding.TURN_LEFT_KEY.matches(keyCode, scanCode)) {
            turnPlayer(-15);
            return true;
        }

        if (KeyBinding.TURN_RIGHT_KEY.matches(keyCode, scanCode)) {
            turnPlayer(15);
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static void chat(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            System.out.println("No message to send to chat.");
            return;
        }

        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().player.sendSystemMessage(Component.literal(msg));
        });
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
