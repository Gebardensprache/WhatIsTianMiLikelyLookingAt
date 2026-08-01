package snownee.jade.api.ui;

/**
 * Something that can draw itself.
 * <p>
 * 1.12.2: stands in for {@code net.minecraft.client.gui.components.Renderable}. The modern
 * {@code GuiGraphicsExtractor} parameter and {@code DeltaTracker} are dropped; partial-tick
 * is passed directly. Where a font renderer is needed, obtain it from
 * {@code Minecraft.getMinecraft().fontRenderer} at the point of use.
 */
public interface Renderable {
	void extractRenderState(int mouseX, int mouseY, float partialTicks);
}
