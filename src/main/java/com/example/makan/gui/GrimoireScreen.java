package com.example.makan.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.stream.Collectors;

public class GrimoireScreen extends Screen {
    private static final ResourceLocation BACKGROUND =
        new ResourceLocation("makan", "textures/gui/grimoire.png");

    private Map<String, KanjiCommand> commands = new HashMap<>();
    private List<Map.Entry<String, KanjiCommand>> filteredCommands = new ArrayList<>();

    private int scrollOffset = 0;
    private static final int ENTRIES_PER_PAGE = 20; // 10 left + 10 right

    private EditBox searchBox;
    private final List<Button> entryButtons = new ArrayList<>();

    public GrimoireScreen() {
        super(Component.literal("Grimoire"));
    }

    @Override
    protected void init() {
        super.init();
        KanjiCommandLoader.cache = null;
        commands = KanjiCommandLoader.loadCommands();

        filteredCommands = new ArrayList<>(commands.entrySet());

        int bgWidth = 600;
        int bgHeight = 500;
        int x = (this.width - bgWidth) / 2;
        int y = (this.height - bgHeight) / 2;

        // Search bar
        searchBox = new EditBox(this.font, x + 40, y + 20, 220, 20, Component.literal("Search..."));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // Prev
        this.addRenderableWidget(Button.builder(Component.literal("Prev"), b -> {
        if (scrollOffset > 0) {
            scrollOffset -= ENTRIES_PER_PAGE;
            rebuildEntryButtons();
        }
        }).bounds(x + 20, y + bgHeight - 30, 60, 20).build());

        // Next on far right
        this.addRenderableWidget(Button.builder(Component.literal("Next"), b -> {
            if (scrollOffset + ENTRIES_PER_PAGE < filteredCommands.size()) {
                scrollOffset += ENTRIES_PER_PAGE;
                rebuildEntryButtons();
            }
        }).bounds(x + bgWidth - 80, y + bgHeight - 30, 60, 20).build());

        // Close stays centered
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
            .bounds(x + bgWidth / 2 - 30, y + bgHeight - 30, 60, 20).build());

        rebuildEntryButtons();
    }

    private void onSearchChanged(String query) {
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        if (lowerQuery.isEmpty()) {
            filteredCommands = new ArrayList<>(commands.entrySet());
        } else {
            filteredCommands = commands.entrySet().stream()
                .filter(e -> e.getKey().contains(lowerQuery)
                        || e.getValue().command.toLowerCase(Locale.ROOT).contains(lowerQuery))
                .collect(Collectors.toList());
        }
        scrollOffset = 0;
        rebuildEntryButtons();
    }

    private void rebuildEntryButtons() {
        for (Button b : entryButtons) {
            this.removeWidget(b);
        }
        entryButtons.clear();

        int bgWidth = 600;
        int bgHeight = 500;
        int x = (this.width - bgWidth) / 2;
        int y = (this.height - bgHeight) / 2;

        int leftX = x + 40;
        int rightX = x + bgWidth / 2 + 20;
        int topY = y + 60;
        int bottomY = y + bgHeight - 80; // leave some room for bottom buttons

        int columnHeight = bottomY - topY;
        int rowsPerColumn = ENTRIES_PER_PAGE / 2;

        int buttonWidth = 220;
        int buttonHeight = columnHeight / rowsPerColumn - 4; // evenly space them

        int end = Math.min(scrollOffset + ENTRIES_PER_PAGE, filteredCommands.size());
        List<Map.Entry<String, KanjiCommand>> page = filteredCommands.subList(scrollOffset, end);

        for (int i = 0; i < page.size(); i++) {
            Map.Entry<String, KanjiCommand> entry = page.get(i);

            String kanji = entry.getKey();
            KanjiCommand kc = entry.getValue();

            // extract cost and last word
            String cost = String.valueOf(kc.cost);
            String lastWord = extractLastWord(kc.command);

            String buttonText = kanji + " : " + cost + " : " + lastWord;

            boolean leftColumn = (i < ENTRIES_PER_PAGE / 2);
            int columnIndex = leftColumn ? i : i - ENTRIES_PER_PAGE / 2;

            int btnX = leftColumn ? leftX : rightX;
            int btnY = topY + columnIndex * (buttonHeight + 4);

            Button btn = Button.builder(Component.literal(buttonText), b -> {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(null); // close GUI
                Grimoire3DRenderer.showGrimoire();
                mc.setScreen(new GrimoireDrawingScreen(kanji));
            }).bounds(btnX, btnY, buttonWidth, buttonHeight).build();

            entryButtons.add(btn);
            this.addRenderableWidget(btn);
        }
    }

    private String extractLastWord(String rawCommand) {
        if (rawCommand == null || rawCommand.isEmpty()) return "";

        // Split by whitespace or colon
        String[] parts = rawCommand.trim().split("[\\s:]+");

        // Walk backwards until we find something with letters/kanji
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].replaceAll("[^\\p{L}\\p{IsIdeographic}]+", "");
            if (!candidate.isEmpty()) {
                return capitalize(candidate);
            }
        }

        return "";
    }



    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Background
        int bgWidth = 600;
        int bgHeight = 500;
        int x = (this.width - bgWidth) / 2;
        int y = (this.height - bgHeight) / 2;
        guiGraphics.blit(BACKGROUND, x, y, 0, 0, bgWidth, bgHeight, bgWidth, bgHeight);

        // Title
        guiGraphics.drawCenteredString(this.font, "Grimoire", this.width / 2, y + 10, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        searchBox.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
