package com.example.makan.gui;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.awt.geom.Point2D;

public class KanjiMatcher {
    static final float DIRECTION_THRESHOLD = 51f;
    static final float DIAGONAL_THRESHOLD = 77f;

    public static class KanjiEntry {
        String kanji;
        List<float[]> strokes;

        public KanjiEntry(String kanji, List<float[]> strokes) {
            this.kanji = kanji;
            this.strokes = strokes;
        }
    }

    public static class MatchResult {
        public final String kanji;
        public final float score;

        public MatchResult(String kanji, float score) {
            this.kanji = kanji;
            this.score = score;
        }
    }

    public enum Direction {
        X, N, NE, E, SE, S, SW, W, NW;

        public static Direction fromLine(float[] l) {
            float dx = l[2] - l[0];
            float dy = l[3] - l[1];
            float adx = Math.abs(dx);
            float ady = Math.abs(dy);

            if (adx < DIRECTION_THRESHOLD && ady < DIRECTION_THRESHOLD)
                return X;

            if (adx > ady) {
                boolean diag = ady > (DIAGONAL_THRESHOLD * adx / 256);
                if (dx > 0) return diag ? (dy < 0 ? NE : SE) : E;
                else        return diag ? (dy < 0 ? NW : SW) : W;
            } else {
                boolean diag = adx > (DIAGONAL_THRESHOLD * ady / 256);
                if (dy > 0) return diag ? (dx < 0 ? SW : SE) : S;
                else        return diag ? (dx < 0 ? NW : NE) : N;
            }
        }

        public static boolean isClose(Direction a, Direction b) {
            if (a == X || b == X) return true;
            int diff = Math.abs(a.ordinal() - b.ordinal());
            return a == b || diff == 1 || diff == 7;
        }
    }

    public static List<KanjiEntry> loadKanjiDatabase() throws IOException {
        InputStream stream = KanjiMatcher.class.getClassLoader().getResourceAsStream("data.json");
        if (stream == null) {
            throw new FileNotFoundException("❌ data.json not found in src/main/resources");
        }

        String json = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        List<KanjiEntry> list = new ArrayList<>();

        for (Map.Entry<String, JsonElement> group : root.entrySet()) {
            JsonObject kanjiGroup = group.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : kanjiGroup.entrySet()) {
                String kanji = entry.getKey();
                JsonArray segments = entry.getValue().getAsJsonArray();
                List<float[]> strokes = new ArrayList<>();
                for (JsonElement segment : segments) {
                    JsonArray arr = segment.getAsJsonArray();
                    float[] line = new float[4];
                    for (int i = 0; i < 4; i++) {
                        line[i] = arr.get(i).getAsFloat();
                    }
                    strokes.add(line);
                }
                list.add(new KanjiEntry(kanji, strokes));
            }
        }
        return list;
    }

    public static List<float[]> simplifyStrokes(List<List<Point2D.Float>> rawStrokes) {
        List<float[]> simplified = new ArrayList<>();
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;

        for (List<Point2D.Float> stroke : rawStrokes) {
            for (Point2D.Float p : stroke) {
                if (p.x < minX) minX = p.x;
                if (p.y < minY) minY = p.y;
                if (p.x > maxX) maxX = p.x;
                if (p.y > maxY) maxY = p.y;
            }
        }

        float width = maxX - minX;
        float height = maxY - minY;
        int scaleSize = 1024;
        double scale = Math.max(width, height) == 0 ? 1.0 : (double) scaleSize / Math.max(width, height);

        for (List<Point2D.Float> stroke : rawStrokes) {
            if (stroke.size() < 2) continue;

            Point2D.Float start = stroke.get(0);
            Point2D.Float end = stroke.get(stroke.size() - 1);

            float x1 = (float) ((start.x - minX) * scale);
            float y1 = (float) ((start.y - minY) * scale);
            float x2 = (float) ((end.x - minX) * scale);
            float y2 = (float) ((end.y - minY) * scale);

            simplified.add(new float[]{x1, y1, x2, y2});
        }

        return simplified;
    }

    public static float matchScore(List<float[]> a, List<float[]> b) {
        if (a.size() != b.size()) return -Float.MAX_VALUE;
        float score = 0f;
        for (int i = 0; i < a.size(); i++) {
            Direction da = Direction.fromLine(a.get(i));
            Direction db = Direction.fromLine(b.get(i));
            if (da == db) score += 1.0f;
            else if (Direction.isClose(da, db)) score += 0.7f;
        }
        return score / a.size() * 100;
    }

    public static List<MatchResult> findMatches(List<float[]> input, List<KanjiEntry> db) {
        List<MatchResult> results = new ArrayList<>();
        for (KanjiEntry entry : db) {
            float score = matchScore(input, entry.strokes);
            if (score > 0) results.add(new MatchResult(entry.kanji, score));
        }
        results.sort((a, b) -> Float.compare(b.score, a.score));
        return results;
    }
}
