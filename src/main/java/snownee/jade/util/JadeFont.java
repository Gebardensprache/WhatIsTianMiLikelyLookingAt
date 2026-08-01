package snownee.jade.util;

import java.util.List;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;

/**
 * Thin wrapper over vanilla's {@link FontRenderer} exposing the measuring API Jade's layout code expects.
 * <p>
 * 1.12.2: upstream wraps {@code net.minecraft.client.gui.Font} and installs a filter that skips oversized glyphs so a
 * resource pack cannot blow up tooltip line height. That is not reproducible here -- 1.12.2's FontRenderer hardcodes
 * {@code FONT_HEIGHT = 9} and exposes no per-glyph metrics to inspect -- so the filter is dropped and line height is
 * simply read off the wrapped renderer.
 */
public class JadeFont {
	private final FontRenderer font;

	public JadeFont(FontRenderer font) {
		this.font = font;
	}

	/**
	 * Underlying vanilla renderer, for callers that need to actually draw.
	 */
	public FontRenderer raw() {
		return font;
	}

	public int width(String text) {
		return font.getStringWidth(text);
	}

	public int width(ITextComponent text) {
		return font.getStringWidth(text.getFormattedText());
	}

	/**
	 * Line height in pixels. Upstream exposes this as a field; kept as a method because 1.12.2's value lives on the
	 * wrapped renderer and can change when the unicode flag flips.
	 */
	public int lineHeight() {
		return font.FONT_HEIGHT;
	}

	public List<String> split(String text, int maxWidth) {
		return font.listFormattedStringToWidth(text, maxWidth);
	}

	public List<String> split(ITextComponent text, int maxWidth) {
		return font.listFormattedStringToWidth(text.getFormattedText(), maxWidth);
	}
}
