package snownee.jade.gui.config;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.util.text.ITextComponent;

public class OptionButton extends OptionsList.Entry {

	public OptionButton(String titleKey, @Nullable GuiButton button) {
		this(makeTitle(titleKey), button);
	}

	public OptionButton(ITextComponent title, @Nullable GuiButton button) {
		super(title);
		if (button != null) {
			if (button.displayString.isEmpty()) {
				button.displayString = title.getFormattedText();
			} else {
				addMessage(button.displayString);
			}
			addWidget(new JadeWidget.Button(button), 0);
		}
	}

}
