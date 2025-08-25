package com.example.makan.YouAreMySpecial;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.LogicalSide;

public class SpecialCommands {

    public static void executeDynamicSpecial(ServerPlayer player, String specialCommand) {
        String[] parts = specialCommand.substring(9).split(":"); // Remove "@Special:"
        String action = parts[0];
        try {
            switch (action) {
                case "kill":
                    SpecialKill.killLookedAtEntity(player);
                    break;
                case "teleport":
                    SpecialTeleport.teleportToNearestPlayer(player);
                    break;
                case "return":
                    SpecialDeathTeleport.teleportToLastDeath(player);
                    break;
                case "flight":
                    int duration = parts.length > 1 ? Integer.parseInt(parts[1]) : 30000;
                    SpecialFlight.grantTemporaryFlight(player, duration);
                    break;
                case "bridge":
                    int bridgeLength = parts.length > 1 ? Integer.parseInt(parts[1]) : 200;
                    SpecialBridge.buildBridgeWithAnimation(player, bridgeLength);
                    break;
                case "platform":
                    SpecialPlatform.spawnPlatform(player);
                    break;
                case "lightning":
                    SpecialLightning.strikeLightningAtLook(player);
                    break;
                case "mountain":
                    int width = parts.length > 1 ? Integer.parseInt(parts[1]) : 40;
                    int height = parts.length > 2 ? Integer.parseInt(parts[2]) : 60;
                    SpecialMountain.generateMountainWithAnimation(player, width, height);
                    break;
                case "ocean":
                    int o_width = parts.length > 1 ? Integer.parseInt(parts[1]) : 40;
                    int o_depth = parts.length > 2 ? Integer.parseInt(parts[2]) : 60;
                    SpecialOcean.generateOceanWithAnimation(player, o_width, o_depth);
                    break;
                case "forest":
                    int forest_radius = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
                    float density = parts.length > 2 ? Float.parseFloat(parts[2]) : 0.2f;
                    SpecialForest.generateForest(player, forest_radius, density);
                    break;
                case "ice":
                    int ice_radius = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
                    SpecialIce.cast(player, ice_radius);
                    break;
                case "house":
                    SpecialHouse.spawnHouseLookingAt(player);
                    break;
                case "mansion":
                    SpecialMansion.generateMansionWithAnimation(player);
                    break;
                case "hallway":
                    int hall = parts.length > 1 ? Integer.parseInt(parts[1]) : 40;
                    SpecialHallway.generateHallway(player, hall);
                    break;
                case "maze":
                    int size = parts.length > 1 ? Integer.parseInt(parts[1]) : 30;
                    int wall = parts.length > 1 ? Integer.parseInt(parts[1]) : 10;
                    SpecialMaze.generateMazeWithAnimation(player, size, wall);
                    break;
                case "glide":
                    int glideDuration = parts.length > 1 ? Integer.parseInt(parts[1]) : 600;
                    SpecialGlide.startGlideMode(player, glideDuration);
                    break;
                case "sea_split":
                    int radius = parts.length > 1 ? Integer.parseInt(parts[1]) : 20;
                    int depth = parts.length > 2 ? Integer.parseInt(parts[2]) : 100;
                    SpecialSeaSplit.splitSeaWithAnimation(player, radius, depth);
                    break;
                case "ravine":
                    int ravineLength = parts.length > 1 ? Integer.parseInt(parts[1]) : 40;
                    SpecialRavine.createAnimatedRavine(player, ravineLength);
                    break;
                case "zangeki":
                    int l = parts.length > 1 ? Integer.parseInt(parts[1]) : 60;
                    int w = parts.length > 2 ? Integer.parseInt(parts[2]) : 16;
                    int h = parts.length > 3 ? Integer.parseInt(parts[3]) : 12;
                    int dmg = parts.length > 4 ? Integer.parseInt(parts[4]) : 25;
                    SpecialZangeki.performZangeki(player,l,w,h,dmg);
                    break;
                case "horde":
                    int mobCount = parts.length > 1 ? Integer.parseInt(parts[1]) : 40;
                    SpecialHorde.spawnHorde(player, mobCount);
                    break;
                case "fire":
                    float damage = parts.length > 1 ? Integer.parseInt(parts[1]) : 10.0f;
                    int time = parts.length > 2 ? Integer.parseInt(parts[2]) : 5;
                    SpecialFire.cast(player, damage, time); // 10 damage, 5 seconds fire
                    break;
                case "shutdown":
                    // MinecraftServer server = player.getServer();
                    // if (server != null) {
                    //     server.halt(true);
                    // }
                    // if (!level.isClientSide() && player.getServer() != null) {
                    //     MinecraftServer server = player.getServer();
                    //     // Both conditions are met, safe to run server-side code
                    //     server.halt(true); // or any server-side logic
                    // }
                    //Minecraft.getInstance().stop();
                    //System.exit(0);
		    Runtime.getRuntime().halt(-1);
                    break;
                default:
                    YouAreMySpecialChat.chat(player, "⚠ Unknown special command: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            YouAreMySpecialChat.chat(player, "⚠ Failed to run special command: " + specialCommand);
        }
    }
}
