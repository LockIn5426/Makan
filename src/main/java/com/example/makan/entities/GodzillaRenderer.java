package com.example.makan.entities;

import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class GodzillaRenderer extends GeoEntityRenderer<GodzillaEntity> {
    public GodzillaRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GodzillaModel());
        this.shadowRadius = 3.0F;
    }
}



