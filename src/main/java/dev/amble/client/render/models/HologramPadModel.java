package dev.amble.client.render.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class HologramPadModel {
    public static final float PIVOT_Y = 24.0F;

    private final ModelPart body;

    public HologramPadModel() {
        this.body = createBodyLayer().bakeRoot().getChild("bb_main");
    }

    public ModelPart body() {
        return this.body;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("bb_main", CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -2.0F, -7.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.0F))
        .texOffs(0, 17).addBox(-7.0F, -2.0F, -7.0F, 14.0F, 2.0F, 14.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 24.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 64, 64);
    }
}
