package snownee.jade.gui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.config.JadeWidget;
import snownee.jade.gui.config.NotUglyEditBox;
import snownee.jade.gui.config.OptionsList;
import snownee.jade.gui.config.value.OptionValue;
import snownee.jade.impl.config.WailaConfig;
import snownee.jade.util.JsonConfig;

public class ProfileConfigScreen extends BaseOptionsScreen {

	private @Nullable OptionValue<Boolean> enabledEntry;

	public ProfileConfigScreen(GuiScreen parent) {
		super(parent, new TextComponentTranslation("gui.jade.profile_settings"));
		saver = () -> {
			for (OptionsList.Entry entry : options().children()) {
				if (entry instanceof ProfileEntry profileEntry) {
					profileEntry.save();
				}
			}
			KeyBinding.resetKeyBindingArrayAndHash();
			mc.gameSettings.saveOptions();
		};
		boolean enabled = Jade.rootConfig().isEnableProfiles();
		int index = Jade.rootConfig().profileIndex;
		Runnable runnable = JadeClient.recoverKeysAction($ -> JadeKeys.openConfig().getKeyCategory().equals($.getKeyCategory()));
		canceller = () -> {
			if (enabled) {
				Jade.useProfile(index);
			} else {
				Jade.rootConfig().setEnableProfiles(false);
			}
			runnable.run();
		};
	}

	@Override
	public OptionsList createOptions(OptionsList options) {
		WailaConfig.Root root = Jade.rootConfig();
		options.title("profiles");
		enabledEntry = options.choices(
				"enable_profiles", root::isEnableProfiles, value -> {
					Jade.rootConfig().setEnableProfiles(value);
					refresh();
				});
		for (int i = 0; i < JadeClient.profiles.length; i++) {
			options.add(new ProfileEntry(i));
		}

		options.title("key_binds");
		for (KeyBinding keyBinding : JadeClient.profiles) {
			options.keybind(keyBinding);
		}

		return options;
	}

	@Override
	public void initGui() {
		super.initGui();
		refresh();
	}

	public void refresh() {
		boolean enabled = Jade.rootConfig().isEnableProfiles();
		for (OptionsList.Entry entry : options().children()) {
			if (entry != enabledEntry) {
				entry.setDisabled(!enabled);
				if (entry instanceof ProfileEntry profileEntry) {
					profileEntry.refresh();
				}
			}
		}
	}

	public static class ProfileEntry extends OptionsList.Entry {
		public static final ITextComponent USE = new TextComponentTranslation("gui.jade.profile.use");
		public static final ITextComponent SAVE = new TextComponentTranslation("selectWorld.edit.save");
		private final int index;
		private final NotUglyEditBox editBox;
		private final @Nullable String originalName;

		public ProfileEntry(int index) {
			super(new TextComponentTranslation("config.jade.profile." + index));
			this.index = index;

			editBox = new NotUglyEditBox(font.raw(), 0, 0, 150, 20, title());
			editBox.fixedTextX = 4;
			editBox.fixedTextY = 7;
			editBox.fixedInnerWidth = editBox.getWidth() - 4 - 12;

			editBox.backgroundMode = NotUglyEditBox.BackgroundMode.HOVERING;
			editBox.setMaxLength(WailaConfig.MAX_NAME_LENGTH);
			editBox.setHint(title());
			editBox.setResponder(_ -> refresh());
			String name = Jade.configs().get(index).get().getName();
			if (name.startsWith("@")) {
				editBox.setValue(I18n.format(name.substring(1)));
				originalName = editBox.getValue();
			} else {
				editBox.setValue(name);
				originalName = null;
			}
			addWidget(new OptionsList.EntryWidget(new JadeWidget.TextField(editBox), 6, -editBox.height / 2, false));

			addWidget(new JadeWidget.Button(new GuiButton(0, 0, 0, 48, 20, USE.getFormattedText()) {
				@Override
				public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
					if (super.mousePressed(mc, mouseX, mouseY)) {
						Jade.useProfile(index);
						if (mc.currentScreen instanceof ProfileConfigScreen screen) {
							screen.refresh();
						}
						return true;
					}
					return false;
				}
			}), 0);

			addWidget(new JadeWidget.Button(new GuiButton(0, 0, 0, 48, 20, SAVE.getFormattedText()) {
				@Override
				public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
					if (super.mousePressed(mc, mouseX, mouseY)) {
						if (JadeUI.hasControlDown()) {
							Jade.saveProfile(index);
							return true;
						}
						GuiScreen screen = mc.currentScreen;
						mc.displayGuiScreen(new GuiYesNo(
								new GuiYesNoCallback() {
									@Override
									public void confirmClicked(boolean bl, int id) {
										if (bl) {
											Jade.saveProfile(index);
										}
										mc.displayGuiScreen(screen);
									}
								},
								new TextComponentTranslation("gui.jade.save_profile.title").getFormattedText(),
								new TextComponentTranslation("gui.jade.save_profile.message", normalTitle()).getFormattedText(),
								I18n.format("gui.continue"),
								I18n.format("gui.cancel"),
								0));
						return true;
					}
					return false;
				}
			}), 100 - 48);
		}

		public void refresh() {
			WailaConfig.Root root = Jade.rootConfig();
			boolean enabled = root.isEnableProfiles();
			boolean current = index == root.profileIndex;
			if (enabled && current) {
				setTitle(normalTitle().createCopy().setStyle(new Style().setColor(TextFormatting.YELLOW))
						.appendSibling(new TextComponentTranslation("gui.jade.profile.active")));
			} else {
				setTitle(normalTitle());
			}
			for (JadeWidget widget : children()) {
				if (widget instanceof JadeWidget.TextField textField && textField.textField instanceof NotUglyEditBox editBox) {
					editBox.setTextColor(enabled && current ? 0xFFFFFF55 : 0xFFE0E0E0);
					editBox.setEnabled(enabled);
				} else if (enabled) {
					widget.active = !current;
				}
			}
		}

		private ITextComponent normalTitle() {
			return editBox.getValue().isEmpty() ?
					new TextComponentTranslation("config.jade.profile." + index) :
					new TextComponentString(editBox.getValue());
		}

		public void save() {
			JsonConfig<? extends WailaConfig> config = Jade.configs().get(index);
			if (originalName == null || !originalName.equals(editBox.getValue())) {
				config.get().setName(editBox.getValue());
			}
			config.save();
		}
	}

}
