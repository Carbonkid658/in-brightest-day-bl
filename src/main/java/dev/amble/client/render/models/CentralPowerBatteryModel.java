package dev.amble.client.render.models;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class CentralPowerBatteryModel {
    public static final float CORE_Y = 53.1005F;

    private final ModelPart root;
    private final ModelPart body;

    public CentralPowerBatteryModel() {
        this.root = createBodyLayer().bakeRoot();
        this.body = this.root.getChild("bone16");
    }

    public ModelPart body() {
        return this.body;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition bone16 = partdefinition.addOrReplaceChild("bone16", CubeListBuilder.create().texOffs(0, 0).addBox(-28.0F, -27.8995F, 11.0F, 56.0F, 56.0F, 56.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -53.1005F, -39.0F));

        PartDefinition cube_r1 = bone16.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 112).addBox(-13.0F, -14.0F, 3.0F, 26.0F, 26.0F, 76.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.75F, 1.1005F, -2.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition north = bone16.addOrReplaceChild("north", CubeListBuilder.create().texOffs(90, 258).addBox(-18.0F, -19.3995F, -7.0F, 36.0F, 36.0F, 0.0F, new CubeDeformation(0.001F))
        .texOffs(0, 258).addBox(-22.5F, -22.6495F, 0.9F, 45.0F, 45.0F, 0.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        PartDefinition bone = north.addOrReplaceChild("bone", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, -21.8995F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition cube_r2 = bone.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(158, 296).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.4142F, -10.5858F, 0.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition cube_r3 = bone.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-10.4142F, -10.5858F, 0.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition cube_r4 = bone.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(158, 296).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-14.7279F, -0.1716F, 0.0F, 0.0F, 0.0F, -1.5708F));

        PartDefinition cube_r5 = bone.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(90, 294).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.4142F, 10.2426F, 0.0F, 0.0F, 0.0F, -2.3562F));

        PartDefinition cube_r6 = bone.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(288, 80).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 14.5564F, 0.0F, 0.0F, 0.0F, -3.1416F));

        PartDefinition cube_r7 = bone.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(90, 294).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.7279F, -0.1716F, 0.0F, 0.0F, 0.0F, 1.5708F));

        PartDefinition cube_r8 = bone.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(288, 80).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.4142F, 10.2426F, 0.0F, 0.0F, 0.0F, 2.3562F));

        PartDefinition bone5 = north.addOrReplaceChild("bone5", CubeListBuilder.create(), PartPose.offset(0.0F, -0.25F, 0.0F));

        PartDefinition bone4 = bone5.addOrReplaceChild("bone4", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition cube_r9 = bone4.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.4359F, 12.292F, -7.1783F, -0.7854F, 0.0F, 2.3562F));

        PartDefinition cube_r10 = bone4.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0719F, 17.4133F, -7.1783F, -0.7854F, 0.0F, 3.1416F));

        PartDefinition cube_r11 = bone4.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(17.5572F, -0.0719F, -7.1783F, -0.7854F, 0.0F, 1.5708F));

        PartDefinition cube_r12 = bone4.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.4359F, -12.4359F, -7.1783F, -0.7854F, 0.0F, 0.7854F));

        PartDefinition bone2 = bone5.addOrReplaceChild("bone2", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition cube_r13 = bone2.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0719F, 17.4133F, -7.1783F, -0.7854F, 0.0F, -3.1416F));

        PartDefinition cube_r14 = bone2.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.4359F, 12.292F, -7.1783F, -0.7854F, 0.0F, -2.3562F));

        PartDefinition cube_r15 = bone2.addOrReplaceChild("cube_r15", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.5572F, -0.0719F, -7.1783F, -0.7854F, 0.0F, -1.5708F));

        PartDefinition cube_r16 = bone2.addOrReplaceChild("cube_r16", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.4359F, -12.4359F, -7.1783F, -0.7854F, 0.0F, -0.7854F));

        PartDefinition south = bone16.addOrReplaceChild("south", CubeListBuilder.create().texOffs(90, 258).addBox(-18.0F, -19.3995F, -7.0F, 36.0F, 36.0F, 0.0F, new CubeDeformation(0.001F))
        .texOffs(0, 258).addBox(-22.5F, -22.6495F, 0.9F, 45.0F, 45.0F, 0.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(0.0F, 0.0F, 78.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition bone12 = south.addOrReplaceChild("bone12", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, -21.8995F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition cube_r17 = bone12.addOrReplaceChild("cube_r17", CubeListBuilder.create().texOffs(158, 296).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.4142F, -10.5858F, 0.0F, 0.0F, 0.0F, 0.7854F));

        PartDefinition cube_r18 = bone12.addOrReplaceChild("cube_r18", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-10.4142F, -10.5858F, 0.0F, 0.0F, 0.0F, -0.7854F));

        PartDefinition cube_r19 = bone12.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(158, 296).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-14.7279F, -0.1716F, 0.0F, 0.0F, 0.0F, -1.5708F));

        PartDefinition cube_r20 = bone12.addOrReplaceChild("cube_r20", CubeListBuilder.create().texOffs(90, 294).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-10.4142F, 10.2426F, 0.0F, 0.0F, 0.0F, -2.3562F));

        PartDefinition cube_r21 = bone12.addOrReplaceChild("cube_r21", CubeListBuilder.create().texOffs(288, 80).mirror().addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0F, 14.5564F, 0.0F, 0.0F, 0.0F, -3.1416F));

        PartDefinition cube_r22 = bone12.addOrReplaceChild("cube_r22", CubeListBuilder.create().texOffs(90, 294).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(14.7279F, -0.1716F, 0.0F, 0.0F, 0.0F, 1.5708F));

        PartDefinition cube_r23 = bone12.addOrReplaceChild("cube_r23", CubeListBuilder.create().texOffs(288, 80).addBox(-9.0F, -7.0F, -15.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(10.4142F, 10.2426F, 0.0F, 0.0F, 0.0F, 2.3562F));

        PartDefinition bone13 = south.addOrReplaceChild("bone13", CubeListBuilder.create(), PartPose.offset(0.0F, -0.25F, 0.0F));

        PartDefinition bone14 = bone13.addOrReplaceChild("bone14", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -0.3927F));

        PartDefinition cube_r24 = bone14.addOrReplaceChild("cube_r24", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.4359F, 12.292F, -7.1783F, -0.7854F, 0.0F, 2.3562F));

        PartDefinition cube_r25 = bone14.addOrReplaceChild("cube_r25", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0719F, 17.4133F, -7.1783F, -0.7854F, 0.0F, 3.1416F));

        PartDefinition cube_r26 = bone14.addOrReplaceChild("cube_r26", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(17.5572F, -0.0719F, -7.1783F, -0.7854F, 0.0F, 1.5708F));

        PartDefinition cube_r27 = bone14.addOrReplaceChild("cube_r27", CubeListBuilder.create().texOffs(0, 303).mirror().addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(12.4359F, -12.4359F, -7.1783F, -0.7854F, 0.0F, 0.7854F));

        PartDefinition bone15 = bone13.addOrReplaceChild("bone15", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.3927F));

        PartDefinition cube_r28 = bone15.addOrReplaceChild("cube_r28", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0719F, 17.4133F, -7.1783F, -0.7854F, 0.0F, -3.1416F));

        PartDefinition cube_r29 = bone15.addOrReplaceChild("cube_r29", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.4359F, 12.292F, -7.1783F, -0.7854F, 0.0F, -2.3562F));

        PartDefinition cube_r30 = bone15.addOrReplaceChild("cube_r30", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.5572F, -0.0719F, -7.1783F, -0.7854F, 0.0F, -1.5708F));

        PartDefinition cube_r31 = bone15.addOrReplaceChild("cube_r31", CubeListBuilder.create().texOffs(0, 303).addBox(-9.0F, 2.0F, -8.0F, 18.0F, 1.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-12.4359F, -12.4359F, -7.1783F, -0.7854F, 0.0F, -0.7854F));

        PartDefinition bone6 = bone16.addOrReplaceChild("bone6", CubeListBuilder.create().texOffs(224, 0).addBox(-48.0F, -1.5F, -2.25F, 98.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(-1.0F, 0.1005F, 39.5F));

        PartDefinition cube_r32 = bone6.addOrReplaceChild("cube_r32", CubeListBuilder.create().texOffs(0, 214).addBox(-34.0F, -11.0F, -11.5F, 70.0F, 22.0F, 22.0F, new CubeDeformation(0.0F))
        .texOffs(184, 214).addBox(-39.0F, -7.0F, -7.5F, 80.0F, 14.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.7854F, 0.0F, 0.0F));

        PartDefinition bone7 = bone16.addOrReplaceChild("bone7", CubeListBuilder.create(), PartPose.offset(0.0F, -32.8995F, 41.0F));

        PartDefinition cube_r33 = bone7.addOrReplaceChild("cube_r33", CubeListBuilder.create().texOffs(224, 8).addBox(-24.0F, -25.0F, 15.0F, 48.0F, 48.0F, 16.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 125.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition cube_r34 = bone7.addOrReplaceChild("cube_r34", CubeListBuilder.create().texOffs(292, 242).addBox(-18.0F, -19.0F, 24.0F, 36.0F, 36.0F, 0.0F, new CubeDeformation(0.004F)), PartPose.offsetAndRotation(0.5F, -0.15F, 0.5F, 1.5708F, 0.0F, 0.0F));

        PartDefinition cube_r35 = bone7.addOrReplaceChild("cube_r35", CubeListBuilder.create().texOffs(184, 242).addBox(-18.0F, -19.0F, 6.0F, 36.0F, 36.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition cube_r36 = bone7.addOrReplaceChild("cube_r36", CubeListBuilder.create().texOffs(184, 242).addBox(-18.0F, -19.0F, 6.0F, 36.0F, 36.0F, 18.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 102.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition cube_r37 = bone7.addOrReplaceChild("cube_r37", CubeListBuilder.create().texOffs(363, 77).addBox(-14.0F, -15.0F, 7.0F, 28.0F, 28.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 7.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition cube_r38 = bone7.addOrReplaceChild("cube_r38", CubeListBuilder.create().texOffs(363, 77).addBox(-14.0F, -15.0F, 7.0F, 28.0F, 28.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 86.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition cube_r39 = bone7.addOrReplaceChild("cube_r39", CubeListBuilder.create().texOffs(204, 112).addBox(-13.0F, -14.0F, -61.0F, 26.0F, 26.0F, 76.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 12.0F, 0.0F, 1.5708F, -0.7854F, 0.0F));

        PartDefinition bone8 = bone16.addOrReplaceChild("bone8", CubeListBuilder.create(), PartPose.offsetAndRotation(44.4645F, 34.993F, 57.338F, 0.3054F, 0.0F, 0.0F));

        PartDefinition cube_r40 = bone8.addOrReplaceChild("cube_r40", CubeListBuilder.create().texOffs(162, 258).addBox(-3.0F, -2.0F, -3.0F, 4.0F, 33.0F, 4.0F, new CubeDeformation(0.001F)), PartPose.offsetAndRotation(-89.777F, -3.4966F, 9.4845F, 0.3185F, 0.3035F, -0.7363F));

        PartDefinition cube_r41 = bone8.addOrReplaceChild("cube_r41", CubeListBuilder.create().texOffs(226, 296).addBox(-3.0F, -2.0F, -3.0F, 4.0F, 39.0F, 4.0F, new CubeDeformation(0.0F))
        .texOffs(226, 296).mirror().addBox(90.8F, -2.0F, -3.0F, 4.0F, 39.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-90.3645F, -36.3891F, -5.8535F, 0.4363F, 0.0F, 0.0F));

        PartDefinition cube_r42 = bone8.addOrReplaceChild("cube_r42", CubeListBuilder.create().texOffs(162, 258).mirror().addBox(-1.0F, -2.0F, -3.0F, 4.0F, 33.0F, 4.0F, new CubeDeformation(0.001F)).mirror(false), PartPose.offsetAndRotation(0.8481F, -3.4966F, 9.4845F, 0.3185F, -0.3035F, 0.7363F));

        PartDefinition bone9 = bone16.addOrReplaceChild("bone9", CubeListBuilder.create(), PartPose.offsetAndRotation(-44.4645F, 34.993F, 57.338F, 0.0873F, 0.0F, 0.0F));

        PartDefinition bone10 = bone16.addOrReplaceChild("bone10", CubeListBuilder.create(), PartPose.offsetAndRotation(-44.4645F, 34.993F, 57.338F, 0.3054F, 0.0F, 0.0F));

        PartDefinition cube_r43 = bone10.addOrReplaceChild("cube_r43", CubeListBuilder.create().texOffs(224, 72).addBox(-26.0F, -2.0F, -2.0F, 51.0F, 4.0F, 4.0F, new CubeDeformation(0.004F)), PartPose.offsetAndRotation(44.976F, 16.9024F, 17.8922F, 0.4363F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 512, 512);
    }
}
