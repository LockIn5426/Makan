package com.example.makan.gui;

import com.google.gson.*;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads Kanji commands from config/kanji_commands.json at runtime.
 * Keys are canonicalized to ensure matching from DrawingScreen works reliably.
 */
public class KanjiCommandLoader {

    private static final String FILE_NAME = "kanji_commands.json";
    public static Map<String, KanjiCommand> cache = null;

    public static Map<String, KanjiCommand> loadCommands() {
        if (cache != null) return cache;

        Map<String, KanjiCommand> result = new HashMap<>();
        File configDir = new File(Minecraft.getInstance().gameDirectory, "config");
        File jsonFile = new File(configDir, FILE_NAME);

        try {
            // Ensure config directory exists
            if (!configDir.exists()) configDir.mkdirs();

            // Create file with default content if missing or empty
            if (!jsonFile.exists() || jsonFile.length() == 0) {
                DrawingScreen.chat("[Makan] Config missing or empty. Creating default " + FILE_NAME);

                try (InputStream in = KanjiCommandLoader.class.getClassLoader().getResourceAsStream("config/" + FILE_NAME)) {
                    if (in != null) {
                        try (OutputStream out = new FileOutputStream(jsonFile)) {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) != -1) out.write(buffer, 0, len);
                        }
                        DrawingScreen.chat("[Makan] Copied default " + FILE_NAME + " from resources/config/ to /config/");
                    } else {
                        // Resource missing → write minimal fallback JSON
                        try (Writer writer = new OutputStreamWriter(new FileOutputStream(jsonFile), StandardCharsets.UTF_8)) {
                            writer.write("{\"kanji_commands\": {\"日\":[10,\"/say sun\"]}}");
                        }
                        DrawingScreen.chat("[Makan] Default resource missing. Wrote minimal fallback JSON.");
                    }
                }
            }

            // Load JSON
            JsonObject root;
            try (Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }

            JsonObject commandsJson = root.getAsJsonObject("kanji_commands");
            if (commandsJson != null) {
                for (Map.Entry<String, JsonElement> e : commandsJson.entrySet()) {
                    String rawKey = e.getKey();
                    String key = canon(rawKey);

                    JsonArray arr = e.getValue().getAsJsonArray();
                    if (arr.size() >= 2) {
                        int cost = arr.get(0).getAsInt();
                        String command = arr.get(1).getAsString();
                        result.put(key, new KanjiCommand(cost, command));
                    }
                }
            }

            cache = result;
            DrawingScreen.chat("[Makan] Loaded " + cache.size() + " Kanji commands from config.");
            return cache;

        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyMap();
        }
    }


    /** Canonicalizes kanji strings for consistent matching. */
    public static String canon(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        n = n.replaceAll("\\p{C}", ""); // remove control chars only
        return n.trim();
    }

}
