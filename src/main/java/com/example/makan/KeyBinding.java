package com.example.makan;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public class KeyBinding {
        public static final String KEY_CATEGORY = "key.category.makan";

        public static final String KEY_CAST = "key.makan.cast";
        public static final String KEY_TURN_LEFT = "key.makan.turn_left";
        public static final String KEY_TURN_RIGHT = "key.makan.turn_right";
        public static final String KEY_CYCLE = "key.makan.cycle";
        public static final String KEY_NEXT_SLOT = "key.makan.next_spell_slot";

        public static final KeyMapping CASTING_KEY = new KeyMapping(KEY_CAST, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY);

        public static final KeyMapping TURN_LEFT_KEY = new KeyMapping(KEY_TURN_LEFT, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, KEY_CATEGORY);

        public static final KeyMapping TURN_RIGHT_KEY = new KeyMapping(KEY_TURN_RIGHT, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, KEY_CATEGORY);

        public static final KeyMapping CYCLING_KEY = new KeyMapping(KEY_CYCLE, KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY);

        public static final KeyMapping NEXT_SLOT_KEY = new KeyMapping(KEY_NEXT_SLOT, KeyConflictContext.IN_GAME, 
                InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_X, KEY_CATEGORY);

        public static final KeyMapping ABILITY1_KEY = new KeyMapping("key.makan.ability1", GLFW.GLFW_KEY_1, "key.categories.makan");
        public static final KeyMapping ABILITY2_KEY = new KeyMapping("key.makan.ability2", GLFW.GLFW_KEY_2, "key.categories.makan");
        public static final KeyMapping ABILITY3_KEY = new KeyMapping("key.makan.ability3", GLFW.GLFW_KEY_3, "key.categories.makan");

    
}
