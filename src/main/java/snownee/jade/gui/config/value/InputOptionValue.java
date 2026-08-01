package snownee.jade.gui.config.value;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiTextField;
import snownee.jade.gui.config.JadeWidget;
import snownee.jade.gui.config.NotUglyEditBox;
import snownee.jade.gui.config.OptionsList;

public class InputOptionValue<T> extends OptionValue<T> {

	public static final Predicate<String> INTEGER = s -> s.matches("[-+]?[0-9]+");
	public static final Predicate<String> FLOAT = s -> s.matches("[-+]?([0-9]*[.,][0-9]+|[0-9]+)");

	private final GuiTextField textField;
	private final Predicate<String> validator;

	public InputOptionValue(Runnable responder, String optionName, Supplier<T> getter, Consumer<T> setter, Predicate<String> validator) {
		super(optionName, getter, setter);
		this.validator = validator;
		textField = new NotUglyEditBox(font.raw(), 0, 0, 98, 18, title());
		updateValue();
		((NotUglyEditBox) textField).setResponder(s -> {
			if (this.validator.test(s)) {
				setValue(s);
				textField.setTextColor(0xFFFFFFFF);
			} else {
				textField.setTextColor(0xFFFF5555);
			}
			responder.run();
		});
		addWidget(new OptionsList.EntryWidget(new JadeWidget.TextField(textField), 0, -textField.height / 2, true));
	}

	@Override
	public boolean isValidValue() {
		return validator.test(textField.getText());
	}

	@Override
	public void setValue(T value) {
		textField.setText(String.valueOf(value));
	}

	@SuppressWarnings("unchecked")
	private void setValue(String text) {
		if (value instanceof String) {
			value = (T) text;
		}
		try {
			if (value instanceof Integer) {
				value = (T) Integer.valueOf(text);
			} else if (value instanceof Short) {
				value = (T) Short.valueOf(text);
			} else if (value instanceof Byte) {
				value = (T) Byte.valueOf(text);
			} else if (value instanceof Long) {
				value = (T) Long.valueOf(text);
			} else if (value instanceof Double) {
				value = (T) Double.valueOf(text);
			} else if (value instanceof Float) {
				value = (T) Float.valueOf(text);
			}
		} catch (NumberFormatException ignored) {
		}
		save();
	}

	@Override
	public void updateValue() {
		T newValue = getter.get();
		if (!Objects.equals(value, newValue)) {
			value = newValue;
			textField.setText(String.valueOf(value));
		}
	}

}
