package com.pingsu.appliedember.client.render;

import appeng.api.client.AEKeyRenderHandler;
import appeng.client.gui.style.Blitter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.pingsu.appliedember.AppliedEmberResources;
import com.pingsu.appliedember.me.key.EmberKey;
import com.pingsu.appliedember.me.key.EmberKeyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

public class EmberRenderer implements AEKeyRenderHandler<EmberKey> {
    public static final EmberRenderer INSTANCE = new EmberRenderer();
    static final Material EMBER = new Material(InventoryMenu.BLOCK_ATLAS, AppliedEmberResources.id("block/aekey/ember"));

    /**
     * Z offset that lifts the quad off the block face it is drawn on, so it does not z-fight with the
     * face itself (plan 4.4: the literal used to be an unexplained {@code 0.1f}).
     */
    private static final float FACE_Z_OFFSET = 0.1f;

    /**
     * The block-face quad is inset slightly because item/fluid icons do not reach the full face size;
     * matches the constant AE2's own fluid key render handler uses.
     */
    private static final float FACE_INSET = 0.05f;

    private EmberRenderer() {}

    @Override
    public void drawInGui(Minecraft minecraft, GuiGraphics guiGraphics, int x, int y, EmberKey stack) {
        Blitter.sprite(EMBER.sprite())
                .dest(x, y, 16, 16)
                .blit(guiGraphics);
    }

    /**
     * Draws the ember sprite on the face of a block that displays an ember stack (e.g. an ME Ember
     * Cell on a storage monitor).
     *
     * <p>Plan #15 asked for this to be rewritten with {@link Blitter} plus a pose transform, but
     * {@code Blitter} only blits into a {@link GuiGraphics} (screen space) and cannot feed a
     * {@link MultiBufferSource} in world space — AE2's own fluid key render handler emits the same
     * four-vertex quad for exactly this reason. The quad is therefore kept, with the previously
     * unexplained offsets named (plan 4.4) and the vertex order matched to AE2's implementation.
     */
    @Override
    public void drawOnBlockFace(PoseStack poseStack, MultiBufferSource buffers, EmberKey what, float scale, int combinedLight, Level level) {
        var sprite = EMBER.sprite();
        poseStack.pushPose();
        poseStack.translate(0, 0, FACE_Z_OFFSET);

        var buffer = buffers.getBuffer(RenderType.solid());

        scale -= FACE_INSET;
        // y is flipped because the pose stack origin for a block face has +y pointing up.
        var x0 = -scale / 2;
        var y0 = scale / 2;
        var x1 = scale / 2;
        var y1 = -scale / 2;

        var transform = poseStack.last().pose();
        buffer.vertex(transform, x0, y1, 0)
                .color(-1)
                .uv(sprite.getU0(), sprite.getV1())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(combinedLight)
                .normal(0, 0, 1)
                .endVertex();
        buffer.vertex(transform, x1, y1, 0)
                .color(-1)
                .uv(sprite.getU1(), sprite.getV1())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(combinedLight)
                .normal(0, 0, 1)
                .endVertex();
        buffer.vertex(transform, x1, y0, 0)
                .color(-1)
                .uv(sprite.getU1(), sprite.getV0())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(combinedLight)
                .normal(0, 0, 1)
                .endVertex();
        buffer.vertex(transform, x0, y0, 0)
                .color(-1)
                .uv(sprite.getU0(), sprite.getV0())
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(combinedLight)
                .normal(0, 0, 1)
                .endVertex();

        poseStack.popPose();
    }

    @Override
    public Component getDisplayName(EmberKey stack) {
        return EmberKeyType.EMBER_NAME;
    }
}
