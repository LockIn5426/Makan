package com.example.makan.gui;

import com.example.makan.Makan;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Drawing screen for the Grimoire that uses the cached kanji strokes.
 */
public class GrimoireDrawingScreen extends Screen {

    private final String kanji;

    private final List<String> kanjiChars = new ArrayList<>();
    private int currentKanjiIndex = 0;

    // Raw & scaled strokes for current kanji
    private List<List<Point2D.Float>> rawExampleStrokes = new ArrayList<>();
    private final List<List<Point2D.Float>> exampleStrokes = new ArrayList<>();

    // Player strokes
    private final List<List<Point2D.Float>> strokes = new ArrayList<>();
    private List<Point2D.Float> currentStroke = null;

    // Animation state
    private int currentExampleStrokeIndex = 0;
    private float animationProgress = 0f;
    private long lastUpdateTime = 0L;
    private boolean waitingForUser = false;

    public GrimoireDrawingScreen(String kanji) {
        super(Component.literal("Practice Kanji"));
        this.kanji = kanji;

        // Split kanji into characters
        for (int i = 0; i < kanji.length(); i++) {
            kanjiChars.add(String.valueOf(kanji.charAt(i)));
        }

        loadKanjiStrokes();
    }

    private void loadKanjiStrokes() {
        rawExampleStrokes.clear();

        if (currentKanjiIndex >= kanjiChars.size()) return;

        String normalized = java.text.Normalizer.normalize(kanjiChars.get(currentKanjiIndex),
                java.text.Normalizer.Form.NFC);

        List<List<Point2D.Float>> cached = KanjiStrokesCache.get().getStrokes(normalized);
        if (cached != null) {
            for (List<Point2D.Float> stroke : cached) {
                // Make a copy for scaling
                List<Point2D.Float> copy = new ArrayList<>();
                for (Point2D.Float pt : stroke) {
                    copy.add(new Point2D.Float(pt.x, pt.y));
                }
                rawExampleStrokes.add(copy);
            }
        } else {
            System.err.println("⚠ Kanji not found in cache: " + normalized);
        }
    }

    @Override
    protected void init() {
        super.init();
        computeExampleStrokes();
    }

    private void computeExampleStrokes() {
        exampleStrokes.clear();
        if (rawExampleStrokes.isEmpty()) return;

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        for (List<Point2D.Float> stroke : rawExampleStrokes) {
            for (Point2D.Float pt : stroke) {
                minX = Math.min(minX, pt.x);
                minY = Math.min(minY, pt.y);
                maxX = Math.max(maxX, pt.x);
                maxY = Math.max(maxY, pt.y);
            }
        }

        float boxW = maxX - minX;
        float boxH = maxY - minY;
        if (boxW <= 0 || boxH <= 0) return;

        float scale = Math.min(width * 0.6f / boxW, height * 0.6f / boxH);
        float offsetX = (width - (boxW * scale)) / 2f - minX * scale;
        float offsetY = (height - (boxH * scale)) / 2f - minY * scale;

        for (List<Point2D.Float> rawStroke : rawExampleStrokes) {
            List<Point2D.Float> scaled = new ArrayList<>();
            for (Point2D.Float pt : rawStroke) {
                scaled.add(new Point2D.Float(pt.x * scale + offsetX, pt.y * scale + offsetY));
            }
            exampleStrokes.add(scaled);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();

        // Animate current stroke if not waiting for user
        if (!waitingForUser && currentExampleStrokeIndex < exampleStrokes.size()) {
            if (lastUpdateTime == 0L) lastUpdateTime = now;
            float delta = (now - lastUpdateTime) / 1000f;
            lastUpdateTime = now;

            float speed = 0.9f; // stroke animation speed
            animationProgress += delta * speed;

            if (animationProgress >= 1f) {
                animationProgress = 1f;
                waitingForUser = true;
            }
        }

        // Draw example strokes
        for (int s = 0; s < exampleStrokes.size(); s++) {
            List<Point2D.Float> stroke = exampleStrokes.get(s);

            int maxSegments;
            if (s < currentExampleStrokeIndex) {
                maxSegments = stroke.size() - 1;
            } else if (s == currentExampleStrokeIndex) {
                maxSegments = Math.max(1, (int) (animationProgress * (stroke.size() - 1)));
            } else {
                maxSegments = 0;
            }

            for (int i = 1; i <= maxSegments; i++) {
                Point2D.Float p1 = stroke.get(i - 1);
                Point2D.Float p2 = stroke.get(i);
                drawLine(p1.x, p1.y, p2.x, p2.y, 0xFFAAAAAA, 3);
            }
        }

        // Draw player strokes
        int lineThickness = 5;
        int outlineThickness = lineThickness + 4;

        for (List<Point2D.Float> stroke : strokes) {
            for (int i = 1; i < stroke.size(); i++) {
                Point2D.Float p1 = stroke.get(i - 1);
                Point2D.Float p2 = stroke.get(i);
                drawLine(p1.x, p1.y, p2.x, p2.y, 0xFFFFFFFF, outlineThickness);
                drawLine(p1.x, p1.y, p2.x, p2.y, 0xFF000000, lineThickness);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        // Debug overlay
        guiGraphics.drawString(font,
                "Stroke " + (currentExampleStrokeIndex + 1) + "/" + exampleStrokes.size() +
                        (waitingForUser ? " (Your turn)" : " (Animating)"),
                10, 10, 0xFFFFFF, false);
    }

    private void drawLine(float x1, float y1, float x2, float y2, int color, float thickness) {
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

        Tesselator t = Tesselator.getInstance();
        BufferBuilder b = t.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float gCol = ((color >> 8) & 0xFF) / 255f;
        float bCol = (color & 0xFF) / 255f;

        b.vertex(x1a, y1a, 0).color(r, gCol, bCol, a).endVertex();
        b.vertex(x2a, y2a, 0).color(r, gCol, bCol, a).endVertex();
        b.vertex(x2b, y2b, 0).color(r, gCol, bCol, a).endVertex();
        b.vertex(x1b, y1b, 0).color(r, gCol, bCol, a).endVertex();

        t.end();
        RenderSystem.disableBlend();
    }

    // --- Mouse input ---
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            currentStroke = new ArrayList<>();
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            strokes.add(currentStroke);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && currentStroke != null) {
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && currentStroke != null) {
            currentStroke.add(new Point2D.Float((float) mouseX, (float) mouseY));
            currentStroke = null;

            if (waitingForUser) {
                waitingForUser = false;
                animationProgress = 0f;
                currentExampleStrokeIndex++;

                // Finished all strokes?
                if (currentExampleStrokeIndex >= exampleStrokes.size()) {
                    currentKanjiIndex++;
                    if (currentKanjiIndex < kanjiChars.size()) {
                        currentExampleStrokeIndex = 0;
                        strokes.clear();
                        loadKanjiStrokes();
                        computeExampleStrokes();
                    } else {
                        Minecraft.getInstance().setScreen(null); // close screen
                        Grimoire3DRenderer.setInactive();
                        Makan.storedKanjiSlots.set(
                        Makan.selectedSlot,
                        Makan.storedKanjiSlots.get(Makan.selectedSlot) + kanji
                    );
                    }
                }
                lastUpdateTime = 0L;
            }

            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
        Grimoire3DRenderer.setInactive();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
