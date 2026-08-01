package snownee.jade.gui.config;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class KeybindOptionButton extends OptionButton {

	private final KeyBinding keybind;

	public KeybindOptionButton(OptionsList owner, KeyBinding keybind) {
		super(new TextComponentString(keybind.getDisplayName()), (GuiButton) null);
		this.keybind = keybind;
		GuiButton button = new GuiButton(0, 0, 0, 100, 20, keybind.getDisplayName()) {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					owner.selectedKey = KeybindOptionButton.this.keybind;
					owner.resetMappingAndUpdateButtons();
					return true;
				}
				return false;
			}
		};
		addWidget(new JadeWidget.Button(button), 0);
	}

	public void refresh(@Nullable KeyBinding selectedKey) {
		JadeWidget widget = Objects.requireNonNull(mainWidget());
		if (selectedKey == keybind) {
			widget.setMessage(new TextComponentString("> ")
					.appendSibling(widget.getMessage().createCopy().setStyle(new Style().setColor(TextFormatting.WHITE).setUnderlined(true)))
					.appendSibling(new TextComponentString(" <")).setStyle(new Style().setColor(TextFormatting.YELLOW)));
		} else {
			widget.setMessage(new TextComponentString(keybind.getDisplayName()));
		}
	}
}
