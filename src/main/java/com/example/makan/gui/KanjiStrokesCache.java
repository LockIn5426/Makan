package com.example.makan.gui;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.awt.geom.Point2D;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class KanjiStrokesCache {

    private static final KanjiStrokesCache INSTANCE = new KanjiStrokesCache();
    private final Map<String, List<List<Point2D.Float>>> kanjiStrokes = new HashMap<>();

    private KanjiStrokesCache() {}

    public static KanjiStrokesCache get() {
        return INSTANCE;
    }

    /**
     * Load all kanji strokes from JSON into memory. Call once on world load.
     */
    public void loadAllStrokes() {
        try {
            var resLoc = new ResourceLocation("makan", "kanji_strokes.json");
            var optRes = Minecraft.getInstance().getResourceManager().getResource(resLoc);
            if (optRes.isEmpty()) {
                System.err.println("❌ kanji_strokes.json not found: " + resLoc);
                return;
            }

            try (InputStream stream = optRes.get().open();
                 InputStreamReader reader = new InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8)) {

                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

                for (String key : root.keySet()) {
                    JsonArray strokesArray = root.getAsJsonArray(key);
                    List<List<Point2D.Float>> strokes = new ArrayList<>();

                    for (JsonElement strokeElem : strokesArray) {
                        List<Point2D.Float> strokePoints = new ArrayList<>();
                        for (JsonElement pointElem : strokeElem.getAsJsonArray()) {
                            JsonObject pt = pointElem.getAsJsonObject();
                            float x = pt.get("x").getAsFloat();
                            float y = pt.get("y").getAsFloat();
                            strokePoints.add(new Point2D.Float(x, y));
                        }
                        strokes.add(strokePoints);
                    }

                    kanjiStrokes.put(key, strokes);
                }

                System.out.println("✅ Loaded kanji strokes for " + kanjiStrokes.size() + " kanji.");

            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load kanji strokes: " + e);
        }
    }

    public List<List<Point2D.Float>> getStrokes(String kanji) {
        return kanjiStrokes.getOrDefault(
                java.text.Normalizer.normalize(kanji, java.text.Normalizer.Form.NFC),
                Collections.emptyList()
        );
    }
}
