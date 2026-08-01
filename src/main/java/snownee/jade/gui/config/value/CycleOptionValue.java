package snownee.jade.gui.config.value;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.config.JadeWidget;
import snownee.jade.gui.config.OptionsList;

/**
 * 1.12.2: the modern {@code CycleButton}/{@code CycleButton.Builder} are replaced by a Jade-local cycling
 * {@link GuiButton} subclass ({@link CycleButton}) plus a small builder ({@link Builder}). Semantics preserved:
 * the button cycles through its values on press, the selected value is written through the entry's setter
 * (and saved immediately), and {@code setValue}/{@code updateValue} keep button and value in sync.
 */
public class CycleOptionValue<T> extends OptionValue<T> {

	public final CycleButton<T> button;

	public CycleOptionValue(String optionName, @Nullable Builder<T> builder, Supplier<T> getter, Consumer<T> setter) {
		super(optionName, getter, setter);
		if (builder == null) {
			builder = new Builder<>(v -> new TextComponentString(String.valueOf(v)), null, getter.get());
		}
		this.button = new CycleButton(this, builder);
		updateValue();
		addWidget(new JadeWidget.Button(button), 0);
	}

	@Override
	public void setValue(T value) {
		button.setValue(value);
		updateValue();
	}

	@Override
	public void updateValue() {
		button.setValue(value = getter.get());
		if (button.builder.tooltipProvider != null) {
			ITextComponent tooltip = button.builder.tooltipProvider.apply(value);
			description = tooltip == null ? List.of() : Lists.newArrayList(tooltip);
		}
	}
	public static class CycleButton<T> extends GuiButton {

		private final CycleOptionValue<T> parent;
		private final Builder<T> builder;
		private T value;

		public CycleButton(CycleOptionValue<T> parent, Builder<T> builder) {
			super(0, 0, 0, 100, 20, "");
			this.parent = parent;
			this.builder = builder;
		}

		@Override
		public void mouseReleased(int mouseX, int mouseY) {
			// no-op
		}

		@Override
		public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
			if (super.mousePressed(mc, mouseX, mouseY)) {
				cycle();
				return true;
			}
			return false;
		}

		public void cycle() {
			List<T> values = builder.values;
			if (values == null || values.isEmpty()) {
				return;
			}
			int index = values.indexOf(value);
			index = (index + 1) % values.size();
			T newValue = values.get(index);
			value = newValue;
			parent.value = newValue;
			parent.save();
			updateDisplayString();
		}

		public void setValue(T value) {
			this.value = value;
			updateDisplayString();
		}

		public T getValue() {
			return value;
		}

		public void setOnValueChange(BiConsumer<CycleButton<T>, T> onValueChange) {
			// modern API surface; value changes are wired through the entry's setter already
		}

		/** Modern {@code setMessage}; used to display the "unavailable" placeholder. */
		public void setMessage(ITextComponent message) {
			displayString = message.getFormattedText();
		}

		private void updateDisplayString() {
			ITextComponent message = builder.nameProvider.apply(value);
			if (message == null) {
				message = new TextComponentString("");
			}
			displayString = message.getFormattedText();
		}

		@Override
		public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
			if (!visible) {
				return;
			}
			hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
			int color = 0xFFA0A0A0;
			if (enabled) {
				color = hovered ? 0xFFFFFFFF : 0xFFE0E0E0;
			}
			Gui.drawRect(x, y, x + width, y + height, enabled ? 0x66101010 : 0x33000000);
			if (hovered && enabled) {
				Gui.drawRect(x, y, x + width, y + 1, 0xAAFFFFFF);
				Gui.drawRect(x, y + height - 1, x + width, y + height, 0xAAFFFFFF);
			}
			int textWidth = mc.fontRenderer.getStringWidth(displayString);
			mc.fontRenderer.drawString(displayString, x + (width - textWidth) / 2, y + (height - 8) / 2, color);
		}

		public void setActive(boolean active) {
			enabled = active;
		}
	}

	/** Minimal replacement for the modern {@code CycleButton.Builder}. */
	public static class Builder<T> {
		public final Function<T, ITextComponent> nameProvider;
		public @Nullable List<T> values;
		public @Nullable Function<T, ITextComponent> tooltipProvider;

		public Builder(Function<T, @Nullable ITextComponent> nameProvider, @Nullable List<T> values, @Nullable T initialValue) {
			this.nameProvider = nameProvider;
			this.values = values;
		}

		public Builder<T> withValues(List<T> values) {
			this.values = values;
			return this;
		}

		/** Modern {@code withTooltip}: per-value description, rendered as the entry description when set. */
		public Builder<T> withTooltip(Function<T, ITextComponent> tooltipProvider) {
			this.tooltipProvider = tooltipProvider;
			return this;
		}

		/** Boolean builder (modern {@code CycleButton.booleanBuilder}). */
		public static Builder<Boolean> booleanBuilder(ITextComponent on, ITextComponent off, boolean initial) {
			return new Builder<>(v -> v ? on : off, Arrays.asList(true, false), initial);
		}

		/** Enum builder (modern {@code CycleButton.builder} with enum values). */
		public static <T extends Enum<T>> Builder<T> enumBuilder(T initial) {
			List<T> values = Arrays.asList(initial.getDeclaringClass().getEnumConstants());
			return new Builder<>(v -> {
				String name = v.name().toLowerCase(Locale.ENGLISH);
				if ("on".equals(name)) {
					return OptionsList.OPTION_ON;
				}
				if ("off".equals(name)) {
					return OptionsList.OPTION_OFF;
				}
				return OptionsList.Entry.makeTitle(initial.name().toLowerCase(Locale.ENGLISH) + "_" + name);
			}, values, initial);
		}
	}
}
