package dev.amble.client.render.renderstates;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

public class LanternBlockEntityRenderState extends BlockEntityRenderState {
    public final BlockModelRenderState model = new BlockModelRenderState();
    public float yRot;
}
