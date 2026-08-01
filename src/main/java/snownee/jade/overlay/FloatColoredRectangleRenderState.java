package snownee.jade.overlay;

/**
 * 1.12.2: see {@link FloatBlitRenderState} for the rationale -- upstream's modern
 * {@code GuiElementRenderState} queuing machinery does not exist here. Confirmed zero
 * callers anywhere in this codebase, so it is reduced to a plain float-quad data holder
 * (no rendering behavior of its own); any 1.12.2 colored-rectangle draw is issued
 * directly via {@link DisplayHelper} at the call site instead of being queued through
 * this type.
 */
public record FloatColoredRectangleRenderState(
		float x0,
		float y0,
		float x1,
		float y1,
		int col1,
		int col2) {
}
