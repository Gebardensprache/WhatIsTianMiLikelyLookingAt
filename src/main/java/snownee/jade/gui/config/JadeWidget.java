package snownee.jade.gui.config;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.gui.config.OptionsList;

/**
 * 1.12.2 shim replacing the modern {@code AbstractWidget} hierarchy.
 * <p>
 * Modern Jade's {@code OptionsList.Entry} stores a list of {@code AbstractWidget}s and screens mutate them through
 * {@code setMessage}/{@code active}/{@code setTooltip}/{@code setWidth}... 1.12.2 has no common base class for
 * buttons and text fields, so this wrapper exposes the narrow surface the gui package actually uses. It is
 * deliberately not a GuiScreen {@code buttonList} member -- {@link OptionsList} renders its entries itself.
 */
public abstract class JadeWidget {

	public boolean active = true;
	public @Nullable ITextComponent tooltip;

	public abstract void setX(int x);

	public abstract int getX();

	public abstract void setY(int y);

	public abstract int getY();

	public abstract void setWidth(int width);

	public abstract int getWidth();

	public abstract int getHeight();

	/** Modern {@code setMessage}; rendering code needs the plain text. */
	public abstract void setMessage(ITextComponent message);

	/** Modern {@code getMessage}. */
	public abstract ITextComponent getMessage();

	public abstract boolean isMouseOver(int mouseX, int mouseY);

	public abstract void extractRenderState(int mouseX, int mouseY, float partialTicks);

	/** Modern {@code setTooltip}. */
	public void setTooltip(@Nullable ITextComponent tooltip) {
		this.tooltip = tooltip;
	}

	public static final class Button extends JadeWidget {

		public final GuiButton button;

		public Button(GuiButton button) {
			this.button = button;
		}

		@Override
		public void setX(int x) {
			button.x = x;
		}

		@Override
		public int getX() {
			return button.x;
		}

		@Override
		public void setY(int y) {
			button.y = y;
		}

		@Override
		public int getY() {
			return button.y;
		}

		@Override
		public void setWidth(int width) {
			button.setWidth(width);
		}

		@Override
		public int getWidth() {
			return button.width;
		}

		@Override
		public int getHeight() {
			return button.height;
		}

		@Override
		public void setMessage(ITextComponent message) {
			button.displayString = message.getFormattedText();
		}

		@Override
		public ITextComponent getMessage() {
			return new TextComponentString(button.displayString);
		}

		@Override
		public boolean isMouseOver(int mouseX, int mouseY) {
			return button.isMouseOver();
		}

		@Override
		public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
			button.enabled = active;
			button.drawButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
		}
	}

	public static final class TextField extends JadeWidget {

		public final GuiTextField textField;

		public TextField(GuiTextField textField) {
			this.textField = textField;
		}

		@Override
		public void setX(int x) {
			textField.x = x;
		}

		@Override
		public int getX() {
			return textField.x;
		}

		@Override
		public void setY(int y) {
			textField.y = y;
		}

		@Override
		public int getY() {
			return textField.y;
		}

		@Override
		public void setWidth(int width) {
			textField.width = width;
		}

		@Override
		public int getWidth() {
			return textField.width;
		}

		@Override
		public int getHeight() {
			return textField.height;
		}

		@Override
		public void setMessage(ITextComponent message) {
			textField.setText(message.getFormattedText());
		}

		@Override
		public ITextComponent getMessage() {
			return new TextComponentString(textField.getText());
		}

		@Override
		public boolean isMouseOver(int mouseX, int mouseY) {
			return false;
		}

		@Override
		public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
			textField.setEnabled(active);
			textField.drawTextBox();
		}
	}
}
