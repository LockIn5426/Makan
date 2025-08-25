package com.example.makan.entities;

import software.bernie.geckolib.model.GeoModel;
import net.minecraft.resources.ResourceLocation;

public class GodzillaModel extends GeoModel<GodzillaEntity> {
    @Override
    public ResourceLocation getModelResource(GodzillaEntity object) {
        return new ResourceLocation("makan", "geo/godzilla.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GodzillaEntity object) {
        return new ResourceLocation("makan", "textures/entity/godzilla.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GodzillaEntity animatable) {
        return new ResourceLocation("makan", "animations/godzilla.animation.json");
    }
}


