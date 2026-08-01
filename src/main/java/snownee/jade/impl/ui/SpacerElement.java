package snownee.jade.impl.ui;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import snownee.jade.JadeInternals;
import snownee.jade.api.ui.CopyBehavior;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.LayoutElement;
import snownee.jade.api.ui.Renderable;
import snownee.jade.api.ui.ResizeableElement;

public class SpacerElement extends ResizeableElement {
	private @Nullable LayoutElement wrapped;
	private int wrappedOffsetX;
	private int wrappedOffsetY;
	private @Nullable Predicate<? extends LayoutElement> onClick;

	public SpacerElement(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public SpacerElement wrapped(LayoutElement wrapped) {
		this.wrapped = wrapped;
		if (wrapped instanceof Element element) {
			ITextComponent narration = element.cachedNarration();
			if (narration != null) {
				narration(narration);
			} else {
				narration("");
			}
			if (tag == null) {
				tag(element.getTag());
			}
		}
		return this;
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		return null;
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (wrapped instanceof Renderable renderable) {
			renderable.extractRenderState(mouseX, mouseY, partialTicks);
		}
	}

	@Override
	public void renderDebug(int mouseX, int mouseY, float partialTicks, RenderDebugContext context) {
		super.renderDebug(mouseX, mouseY, partialTicks, context);
		if (wrapped instanceof Element element) {
			element.renderDebug(mouseX, mouseY, partialTicks, context);
		}
		if (wrapped != null) {
			JadeInternals.getDisplayHelper().drawBorder(getRectangle(), 1, 0x880000FF, true);
		}
	}

	@Override
	public void setFreeSpace(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public SpacerElement offset(int x, int y) {
		wrappedOffsetX = x;
		wrappedOffsetY = y;
		if (wrapped != null && (x != 0 || y != 0)) {
			wrapped.setX(x + wrappedOffsetX);
			wrapped.setY(y + wrappedOffsetY);
		}
		return this;
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		if (wrapped != null) {
			wrapped.setX(x + wrappedOffsetX);
		}
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		if (wrapped != null) {
			wrapped.setY(y + wrappedOffsetY);
		}
	}

	@Override
	public ResizeableElement onClick(Predicate<Element> onClick) {
		this.onClick = onClick;
		return this;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return wrapped != null && wrapped.getRectangle().containsPoint((int) mouseX, (int) mouseY);
	}

	// 1.12.2: upstream mouseClicked(MouseButtonEvent, boolean) override dropped. Nothing in-scope
	// dispatches mouse clicks to individual Elements (the only caller of root.mouseClicked is the
	// parked gui.PinScreen); onClick is retained for API parity but currently unused.

	@Override
	public boolean copyToClipboard() {
		if (wrapped instanceof CopyBehavior behavior && behavior.copyToClipboard()) {
			return true;
		}
		return super.copyToClipboard();
	}
}
