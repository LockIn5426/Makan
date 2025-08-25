package com.example.makan.YouAreMySpecial;

import com.example.makan.YouAreMySpecial.YouAreMySpecialChat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

public class SpecialLightning {

    private static final double MAX_DISTANCE = 100.0;

    public static void strikeLightningAtLook(ServerPlayer player) {
        if (player == null || player.level() == null) {
            YouAreMySpecialChat.chat("❌ Player or level not available.");
            return;
        }

        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookVec = player.getLookAngle().normalize();
        Vec3 end = eyePos.add(lookVec.scale(MAX_DISTANCE));

        HitResult hit = player.level().clip(new ClipContext(
            eyePos,
            end,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE,
            player
        ));

        if (hit.getType() != HitResult.Type.BLOCK) {
            YouAreMySpecialChat.chat(player, "⚠️ No block targeted within 100 blocks.");
            return;
        }

        Vec3 hitPos = hit.getLocation();
        BlockPos target = new BlockPos(
            Mth.floor(hitPos.x),
            Mth.floor(hitPos.y),
            Mth.floor(hitPos.z)
        );

        ServerLevel serverLevel = player.serverLevel();
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning != null) {
            lightning.moveTo(target.getX(), target.getY(), target.getZ());
            lightning.setCause(player);
            serverLevel.addFreshEntity(lightning);

            YouAreMySpecialChat.chat(player, "⚡ Lightning summoned at target block!");
        } else {
            YouAreMySpecialChat.chat(player, "❌ Failed to create lightning.");
        }
    }
}
