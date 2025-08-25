package com.example.makan.gamemode;

import java.util.HashMap;
import java.util.Map;

public class AbilityRegistry {
    private static final Map<CustomGameMode, AbilityHandler> handlers = new HashMap<>();

    static {
        handlers.put(CustomGameMode.KAJIN, new KajinAbilities());
        handlers.put(CustomGameMode.KAMIKAZE, new KamikazeAbilities());
        handlers.put(CustomGameMode.MOGURA, new MoguraAbilities());
        handlers.put(CustomGameMode.NENDOU, new NendouAbilities());
    }

    public static AbilityHandler getHandler(CustomGameMode mode) {
        return handlers.get(mode);
    }
}
