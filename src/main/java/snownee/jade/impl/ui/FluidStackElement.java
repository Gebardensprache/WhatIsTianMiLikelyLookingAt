package snownee.jade.impl.ui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.overlay.DisplayHelper;

public class FluidStackElement extends ProgressOverlayElement {

	private final JadeFluidObject fluid;

	public FluidStackElement(JadeFluidObject fluid) {
		this.fluid = Objects.requireNonNull(fluid);
		width = height = 16;
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (floatingRect == null) {
			DisplayHelper.INSTANCE.drawFluid(getX(), getY(), fluid, width, height, JadeFluidObject.bucketVolume());
		} else {
			DisplayHelper.INSTANCE.drawFluid(
					floatingRect.getX(),
					floatingRect.getY(),
					fluid,
					floatingRect.getWidth(),
					floatingRect.getHeight(),
					JadeFluidObject.bucketVolume());
		}
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		return null;
	}

	/**
	 * 1.12.2: upstream serializes via {@code ComponentHolders.serialize(fluid.typeHolder(), ...)} against
	 * the connection's registry access, neither of which exist here. Falls back to copying the fluid's
	 * display name as a plain string.
	 */
	@Override
	public boolean copyToClipboard() {
		ITextComponent name = fluid.getDisplayName();
		if (name != null) {
			GuiScreen.setClipboardString(name.getUnformattedText());
			return true;
		}
		return false;
	}
}
