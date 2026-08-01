package snownee.jade.impl.ui;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.api.ui.TextElement;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.util.JadeLanguages;

public class TextElementImpl extends TextElement {

	protected final ITextComponent text;
	protected float scale = 1;
	protected float alpha = 1;
	private int textWidth;

	public TextElementImpl(ITextComponent text) {
		this.text = text;
		width = textWidth = Math.max(DisplayHelper.font().width(text), 0);
		height = DisplayHelper.font().lineHeight() - 1;
	}

	@Override
	public TextElement scale(float scale) {
		this.scale = scale;
		width = Math.max(Math.round(DisplayHelper.font().width(text) * scale), 0);
		height = Math.round(DisplayHelper.font().lineHeight() * scale) - 1;
		return this;
	}

	@Override
	public TextElement alpha(float alpha) {
		this.alpha = alpha;
		return this;
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		int x = textLeft();
		int normalColor = IWailaConfig.Overlay.applyAlpha(IThemeHelper.get().getNormalColor(), alpha);
		boolean scaled = scale != 1;
		if (scaled) {
			GlStateManager.pushMatrix();
			GlStateManager.translate(x, getY() + scale, 0.0F);
			GlStateManager.scale(scale, scale, 1.0F);
			DisplayHelper.INSTANCE.drawText(text, 0, 0, normalColor);
		} else {
			DisplayHelper.INSTANCE.drawText(text, x, getY(), normalColor);
		}
		if (mouseX != -1 && getRectangle().containsPoint(mouseX, mouseY)) {
			if (text.getStyle() != null && text.getStyle().getHoverEvent() != null) {
				Element.setHoverEffect(text.getStyle().getHoverEvent());
			}
		}
		if (scaled) {
			GlStateManager.popMatrix();
		}
	}

	@Override
	public ITextComponent getNarration() {
		return NarratableComponent.getNarration(text);
	}

	public String getString() {
		return text.getUnformattedText();
	}

	@Override
	public void setFreeSpace(int width, int height) {
		this.width = width;
		this.height = height;
	}

	// 1.12.2: upstream mouseClicked(MouseButtonEvent, boolean) override, and the ActiveTextCollector-based
	// style-click-event walking it depends on, are dropped entirely -- neither ActiveTextCollector nor
	// MouseButtonEvent exist here, and nothing in-scope dispatches mouse clicks to a TextElementImpl.

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= textLeft() && mouseX < textLeft() + textWidth && mouseY >= getY() && mouseY < getY() + height;
	}

	private int textLeft() {
		int x = getX();
		if (JadeLanguages.INSTANCE.isRTL()) {
			x += width - textWidth;
		}
		return x;
	}
}
