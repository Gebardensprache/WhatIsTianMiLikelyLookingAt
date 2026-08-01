package snownee.jade.gui.config.value;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import snownee.jade.gui.config.JadeWidget;
import snownee.jade.gui.config.OptionsList;

public class SliderOptionValue extends OptionValue<Float> {

	private final Slider slider;
	private float min;
	private float max;
	private FloatUnaryOperator aligner;

	public SliderOptionValue(
			String optionName,
			Supplier<Float> getter,
			Consumer<Float> setter,
			float min,
			float max,
			FloatUnaryOperator aligner) {
		super(optionName, getter, setter);
		value = getter.get();
		this.min = min;
		this.max = max;
		this.aligner = aligner;
		slider = new Slider(this, 0, 0, 100, 20, title());
		updateValue();
		addWidget(new JadeWidget.Button(slider), 0);
	}

	@Override
	public void setValue(Float value) {
		slider.setValue(value, true);
	}

	@Override
	public void updateValue() {
		slider.setValue(value = getter.get(), false);
	}

	/**
	 * 1.12.2: replaces the modern {@code AbstractSliderButton}. The handle position is rendered manually and
	 * dragging is driven by {@link #mouseDragged(int, int)} which the entry routes to while the mouse is down
	 * over the entry.
	 */
	public static class Slider extends GuiButton {
		private static final DecimalFormat fmt = new DecimalFormat("##.##");
		private final SliderOptionValue parent;
		public double value;

		public Slider(SliderOptionValue parent, int x, int y, int width, int height, ITextComponent message) {
			super(0, x, y, width, height, "");
			this.parent = parent;
			this.value = fromScaled(parent.value, parent.min, parent.max);
			updateMessage();
		}

		public static double fromScaled(float f, float min, float max) {
			return MathHelper.clamp((f - min) / (max - min), 0, 1);
		}

		public float toScaled() {
			float f = parent.aligner.apply(parent.min + (parent.max - parent.min) * (float) value);
			String s = fmt.format(f);
			try {
				return fmt.parse(s).floatValue();
			} catch (ParseException e) {
				return f;
			}
		}

		protected void updateMessage() {
			displayString = fmt.format(toScaled());
		}

		protected void applyValue() {
			float scaled = toScaled();
			if (parent.value != scaled) {
				parent.value = scaled;
				parent.save();
			}
		}

		private void setValue(float value, boolean applyValue) {
			if (value != toScaled()) {
				this.value = fromScaled(value, parent.min, parent.max);
				if (applyValue) {
					applyValue();
				}
			}
			updateMessage();
		}

		@Override
		public void mouseReleased(int mouseX, int mouseY) {
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (super.mousePressed(mc, mouseX, mouseY)) {
				setValueFromMouse(mouseX);
				return true;
			}
			return false;
		}

		public void mouseDragged(int mouseX, int mouseY) {
			if (enabled && visible && mouseX >= x - 1 && mouseX <= x + width + 1) {
				setValueFromMouse(mouseX);
			}
		}

		private void setValueFromMouse(int mouseX) {
			double d = (mouseX - x - 4) / (double) (width - 8);
			value = MathHelper.clamp(d, 0, 1);
			applyValue();
			updateMessage();
		}

		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if (!visible) {
				return;
			}
			hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
			int color = enabled ? (hovered ? 0xFFFFFFFF : 0xFFA0A0A0) : 0xFF606060;
			Gui.drawRect(x, y, x + width, y + height, enabled ? 0x66101010 : 0x33000000);
			int trackY = y + height / 2 - 1;
			Gui.drawRect(x + 2, trackY, x + width - 2, trackY + 2, 0xFF404040);
			int handleX = x + 2 + (int) (value * (width - 4));
			Gui.drawRect(handleX - 2, y + 2, handleX + 2, y + height - 2, color);
			int textWidth = mc.fontRenderer.getStringWidth(displayString);
			mc.fontRenderer.drawString(displayString, x + (width - textWidth) / 2, y + (height - 8) / 2, color);
		}
	}
}
