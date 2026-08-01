package snownee.jade.overlay;

/**
 * 1.12.2: upstream's {@code FloatBlitRenderState} is a modern
 * {@code GuiElementRenderState} used to defer a textured quad into the vertex-consumer
 * render-state queue ({@code RenderPipeline}/{@code TextureSetup}/{@code Matrix3x2f}/
 * {@code ScreenRectangle}/{@code VertexConsumer} -- none of which exist in 1.12.2's
 * immediate-mode {@code GlStateManager} + {@code Tessellator} pipeline). Confirmed zero
 * callers anywhere in this codebase, so it is reduced to a plain float-quad data holder
 * (no rendering behavior of its own); any 1.12.2 blit is issued directly via
 * {@link DisplayHelper} at the call site instead of being queued through this type.
 */
public record FloatBlitRenderState(
		float x0,
		float y0,
		float x1,
		float y1,
		float u0,
		float u1,
		float v0,
		float v1,
		int color) {
}
