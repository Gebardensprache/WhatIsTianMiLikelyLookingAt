package snownee.jade.gui.config;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.12.2 translation of the modern {@code NotUglyEditBox}: a {@link GuiTextField} with a responder callback,
 * optional fixed text position/inner width, and a custom borderless background mode. The modern background
 * {@code WidgetSprites} textures do not exist in 1.12.2 assets, so the look is emulated with plain rectangles.
 * The text/cursor/selection drawing is self-contained because vanilla {@link GuiTextField#drawTextBox} uses
 * private field offsets ({@code x+4}, {@code y+(height-8)/2}) that cannot be overridden.
 */
public class NotUglyEditBox extends GuiTextField {

	public @Nullable Integer fixedTextX, fixedTextY, fixedInnerWidth;
	public BackgroundMode backgroundMode = BackgroundMode.VISIBLE;
	public boolean alwaysRenderCross;
	/** Set by {@link OptionsList.Entry#extractContent} during the render pass (modern {@code isHovered}). */
	public boolean hovered;
	public boolean isMouseOverCross;
	private @Nullable Consumer<String> responder;
	private @Nullable ITextComponent hint;
	private boolean enabledState = true;
	private int textColor = 0xE0E0E0;
	private int disabledTextColor = 0x707070;

	public NotUglyEditBox(FontRenderer font, int i, int j, ITextComponent component) {
		this(font, i, j, 120, 18, component);
	}

	public NotUglyEditBox(FontRenderer font, int i, int j, int k, int l, ITextComponent component) {
		this(font, i, j, k, l, null, component);
	}

	public NotUglyEditBox(FontRenderer font, int i, int j, int k, int l, @Nullable NotUglyEditBox editBox, ITextComponent component) {
		super(0, font, i, j, k, l);
		setEnableBackgroundDrawing(false);
		if (editBox != null) {
			// preserve text and focus across init() re-creation (modern EditBox copy-ctor behavior)
			setText(editBox.getText());
			setFocused(editBox.isFocused());
		}
		this.hint = component;
	}

	public void setResponder(Consumer<String> responder) {
		this.responder = responder;
		setGuiResponder(new GuiPageButtonList.GuiResponder() {
			@Override
			public void setEntryValue(int id, boolean value) {
			}

			@Override
			public void setEntryValue(int id, float value) {
			}

			@Override
			public void setEntryValue(int id, String value) {
				if (NotUglyEditBox.this.responder != null) {
					NotUglyEditBox.this.responder.accept(value);
				}
			}
		});
	}

	public void setHint(ITextComponent hint) {
		this.hint = hint;
	}

	/** Modern {@code setValue}; does not notify the responder (matches vanilla {@code setText}). */
	public void setValue(String value) {
		setText(value);
	}

	/** Modern {@code getValue}. */
	public String getValue() {
		return getText();
	}

	/** Modern {@code setEditable}. */
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		enabledState = enabled;
	}

	/** Modern {@code isEditable}. */
	public boolean isEditable() {
		return enabledState;
	}

	/** Modern {@code setMaxLength}. */
	public void setMaxLength(int maxLength) {
		setMaxStringLength(maxLength);
	}

	@Override
	public void setTextColor(int color) {
		super.setTextColor(color);
		textColor = color;
	}

	/** Modern {@code EditBox.updateTextPosition}: no-op -- the fixed offsets are applied at draw time. */
	public void updateTextPosition() {
	}

	/** Modern {@code EditBox.getInnerWidth} (scrolling viewport width). */
	public int getInnerWidth() {
		return fixedInnerWidth != null ? fixedInnerWidth : getWidth() - 8;
	}

	private int textX() {
		return fixedTextX == null ? x + 4 : x + fixedTextX;
	}

	private int textY() {
		return fixedTextY == null ? y + (height - 8) / 2 : y + fixedTextY;
	}

	@Override
	public void drawTextBox() {
		if (!getVisible()) {
			return;
		}
		FontRenderer font = Minecraft.getMinecraft().fontRenderer;

		int bgAlpha;
		if (backgroundMode == BackgroundMode.HOVERING) {
			if (isFocused()) {
				bgAlpha = 255;
			} else if (isEditable() && hovered) {
				bgAlpha = 64;
			} else {
				bgAlpha = 0;
			}
		} else if (backgroundMode == BackgroundMode.INVISIBLE) {
			bgAlpha = 0;
		} else {
			bgAlpha = 255;
		}
		if (bgAlpha > 0) {
			Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, 0x80FFFFFF & 0xFFFFFF | bgAlpha << 24);
			Gui.drawRect(x, y, x + width, y + height, 0x26000000 | bgAlpha << 24);
			if (isEditable() && !getText().isEmpty()) {
				boolean renderCross = alwaysRenderCross || hovered;
				if (renderCross) {
					isMouseOverCross = hovered && isMouseOverCrossCheck();
					int c = isMouseOverCross ? 0xFFFFFFFF : 0xFFE0E0E0;
					font.drawString("×", x + width - 10, textY() + 1, c);
				}
			}
		}

		if (getText().isEmpty() && !isFocused() && hint != null) {
			font.drawString(hint.getFormattedText(), textX(), textY(), 0xFF888888);
		} else {
			int color = isEditable() ? textColor : disabledTextColor;
			String display = font.trimStringToWidth(getText(), getInnerWidth());
			int textX = textX();
			int textY = textY();
			if (isFocused()) {
				// draw with selection/cursor like vanilla, at the fixed offset
				drawTextWithCursor(font, display, textX, textY, color);
			} else {
				font.drawString(display, textX, textY, color);
			}
		}
	}

	private boolean isMouseOverCrossCheck() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc.currentScreen == null) {
			return false;
		}
		int mouseX = Mouse.getX() * mc.currentScreen.width / mc.displayWidth;
		return mouseX > x + width - 12;
	}

	private void drawTextWithCursor(FontRenderer font, String display, int textX, int textY, int color) {
		int cursor = Math.min(getCursorPosition(), display.length());
		String before = display.substring(0, cursor);
		int xPos = textX + font.getStringWidth(before);
		boolean showCursor = Minecraft.getSystemTime() / 500 % 2 == 0;
		if (showCursor && cursor < display.length()) {
			Gui.drawRect(xPos, textY - 1, xPos + 1, textY + 1 + font.FONT_HEIGHT, 0xFFE0E0E0);
			font.drawString(display, textX, textY, color);
		} else if (showCursor) {
			Gui.drawRect(xPos, textY - 1, xPos + 1, textY + 1 + font.FONT_HEIGHT, 0xFFE0E0E0);
			font.drawString(display, textX, textY, color);
		} else {
			font.drawString(display, textX, textY, color);
		}
	}

	@Override
	public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
		if (isMouseOverCross && mouseButton == 0) {
			setText("");
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/** Modern {@code EditBox.isVisible}. */
	public boolean isVisible() {
		return getVisible();
	}

	public enum BackgroundMode {
		VISIBLE, INVISIBLE, HOVERING
	}
}
