package snownee.jade.gui.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.util.JadeFont;

/**
 * 1.12.2 shim replacing modern {@code AbstractStringWidget} used as the title of an
 * {@link OptionsList.Entry}. 1.12.2 has no widget hierarchy -- the title is just text
 * with a width measured through {@link JadeFont}. It extends {@link JadeWidget} so
 * entries can store titles in their widget lists like the modern tree does.
 */
public class TitleWidget extends JadeWidget {

	public ITextComponent message;
	private final JadeFont font;

	public TitleWidget(ITextComponent message, FontRenderer font) {
		this.message = message;
		this.font = new JadeFont(font);
	}

	public TitleWidget(ITextComponent message) {
		this(message, Minecraft.getMinecraft().fontRenderer);
	}

	@Override
	public void setMessage(ITextComponent message) {
		this.message = message;
	}

	@Override
	public ITextComponent getMessage() {
		return message;
	}

	@Override
	public int getWidth() {
		return font.width(message);
	}

	@Override
	public int getHeight() {
		return font.lineHeight();
	}

	public JadeFont font() {
		return font;
	}

	public String getString() {
		return message.getFormattedText();
	}

	@Override
	public void setX(int x) {
		this.x = x;
	}

	private int x;
	private int y;

	@Override
	public int getX() {
		return x;
	}

	@Override
	public void setY(int y) {
		this.y = y;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public void setWidth(int width) {
	}

	@Override
	public boolean isMouseOver(int mouseX, int mouseY) {
		return false;
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		font.raw().drawString(getString(), x, y, 0xFFFFFFFF);
	}

	/** Modern {@code Component.empty()} title. */
	public static TitleWidget empty() {
		return new TitleWidget(new TextComponentString(""));
	}
}
