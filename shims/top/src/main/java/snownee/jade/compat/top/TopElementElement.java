package snownee.jade.compat.top;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.ui.Element;

/**
 * Jade {@link Element} that delegates rendering to a reconstructed TOP {@code IElement}.
 * <p>
 * The TOP element draws itself with absolute coordinates relative to the overlay, so this
 * wrapper forwards Jade's layout-assigned position into the TOP element's {@code render(x, y)}.
 * Dimensions come from the TOP element's own {@code getWidth()}/{@code getHeight()}.
 */
public class TopElementElement extends Element {

	private final mcjty.theoneprobe.api.IElement delegate;

	public TopElementElement(mcjty.theoneprobe.api.IElement delegate) {
		this.delegate = delegate;
		width = delegate.getWidth();
		height = delegate.getHeight();
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		try {
			delegate.render(getX(), getY());
		} catch (Exception e) {
			// Keep the tooltip alive if a third-party element throws.
		}
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		return new TextComponentString("");
	}
}
