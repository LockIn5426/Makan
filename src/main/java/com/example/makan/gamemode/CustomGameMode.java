package com.example.makan.gamemode;

public enum CustomGameMode {
    SURVIVAL_LIKE(0),
    KAJIN(1),
    KAMIKAZE(2),
    MOGURA(3),
    NENDOU(4);

    private final int id;

    CustomGameMode(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static CustomGameMode fromId(int id) {
        for (CustomGameMode mode : values()) {
            if (mode.id == id) {
                return mode;
            }
        }
        // Default fallback if unknown ID received
        return SURVIVAL_LIKE;
    }
}
