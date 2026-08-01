package snownee.jade.gui;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.util.SmoothChasingValue;

/**
 * 1.12.2: plain {@link GuiButton} with a custom draw. The cursor/narration parts of the modern button are
 * dropped (no cursor system in 1.12.2).
 */
public class CreditButton extends GuiButton {

	private final ITextComponent hoveredTitle;
	private final Consumer<CreditButton> onPress;
	private final Consumer<CreditButton> onHover;
	private final SmoothChasingValue progress = new SmoothChasingValue();
	private boolean oldHovered;
	private boolean showTranslators;
	private List<String> translators = List.of();
	private int translatorIndex;
	private float translatorTime;

	public CreditButton(
			int x,
			int y,
			int width,
			int height,
			ITextComponent title,
			ITextComponent hoveredTitle,
			Consumer<CreditButton> onPress,
			Consumer<CreditButton> onHover) {
		super(0, x, y, width, height, title.getFormattedText());
		this.hoveredTitle = hoveredTitle;
		this.onHover = onHover;
		this.onPress = onPress;
	}

	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (super.mousePressed(mc, mouseX, mouseY)) {
			if (onPress != null) {
				onPress.accept(this);
			}
			return true;
		}
		return false;
	}

	@Override
	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) {
			return;
		}
		boolean hovered = isMouseOver();
		if (!oldHovered && hovered) {
			progress.target(1);
		} else if (!hovered) {
			progress.target(0);
		} else if (progress.value > 0.5F) {
			progress.target(0);
			onHover.accept(this);
		}
		progress.tick(partialTicks);
		progress.value = Math.min(0.6F, progress.value);
		float alpha = hovered ? 170 : 85;
		if (showTranslators && !translators.isEmpty()) {
			int cycleTime = 60;
			translatorTime += partialTicks;
			if (translatorTime > cycleTime) {
				nextTranslator();
			}
			if (!hovered && translators.size() > 1) {
				if (translatorTime < 5) {
					alpha *= translatorTime / 5;
				} else if (cycleTime - translatorTime < 5) {
					alpha *= (cycleTime - translatorTime) / 5;
				}
				alpha = Math.max(alpha, 17);
			}
		}
		FontRenderer font = mc.fontRenderer;
		ITextComponent credit = hovered ? hoveredTitle : getMessage();
		int textWidth = font.getStringWidth(credit.getFormattedText());
		float scale = 1 + progress.value * 0.2F;
		GlStateManager.pushMatrix();
		GlStateManager.translate(x + width * 0.5F, y, 0);
		GlStateManager.scale(scale, scale, 1);
		GlStateManager.translate(textWidth * -0.5F, 0, 0);
		font.drawString(credit.getFormattedText(), 0, 0, 0xFFFFFF | (int) alpha << 24);
		GlStateManager.popMatrix();
		oldHovered = hovered;
	}

	/** @deprecated modern Jade passes the narration callback; 1.12.2 has no narration. */
	@Deprecated
	public void showTranslators() {
		if (showTranslators) {
			return;
		}
		showTranslators = true;
		if (!JadeUI.hasTranslation("gui.jade.translators") || "placeholder ".equals(I18n.format("gui.jade.translated_by", ""))) {
			return;
		}
		String s = I18n.format("gui.jade.translators");
		if ("Bob, Alice, Charlie".equals(s)) {
			return;
		}
		translators = Stream.of(StringUtils.split(s, ',')).map(String::trim).filter(StringUtils::isNotEmpty).toList();
		if (translators.size() > 1) {
			translatorIndex = new java.util.Random().nextInt(translators.size());
		}
		nextTranslator();
	}

	private void nextTranslator() {
		setMessage(new TextComponentTranslation("gui.jade.translated_by", translators.get(translatorIndex)));
		if (translators.size() <= 1) {
			return;
		}
		translatorIndex++;
		if (translatorIndex >= translators.size()) {
			translatorIndex = 0;
		}
		translatorTime = 0;
	}

	private void setMessage(ITextComponent message) {
		displayString = message.getFormattedText();
	}

	private ITextComponent getMessage() {
		return new TextComponentString(displayString);
	}
}
