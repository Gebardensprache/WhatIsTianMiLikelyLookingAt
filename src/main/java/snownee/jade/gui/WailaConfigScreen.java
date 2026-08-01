package snownee.jade.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import snownee.jade.util.FloatUnaryOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.Theme;
import snownee.jade.gui.config.JadeWidget;
import snownee.jade.gui.config.OptionButton;
import snownee.jade.gui.config.OptionsList;
import snownee.jade.gui.config.value.CycleOptionValue;
import snownee.jade.gui.config.value.OptionValue;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.theme.ThemeHelper;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.ModIdentification;

public class WailaConfigScreen extends PreviewOptionsScreen {

	private @Nullable CycleOptionValue<ResourceLocation> styleEntry;
	private @Nullable OptionValue<Float> opacityEntry;

	public WailaConfigScreen(@Nullable GuiScreen parent) {
		super(parent, new TextComponentTranslation("gui.jade.jade_settings"));
		saver = () -> {
			IWailaConfig.get().save();
			JadeClient.refreshKeyState();
			KeyBinding.resetKeyBindingArrayAndHash();
			mc.gameSettings.saveOptions();
		};
		Predicate<KeyBinding> predicate = $ -> JadeKeys.openConfig().getKeyCategory().equals($.getKeyCategory());
		Runnable runnable = JadeClient.recoverKeysAction(predicate);
		canceller = () -> {
			IWailaConfig.get().invalidate();
			runnable.run();
		};
	}

	@SuppressWarnings("UnusedReturnValue")
	public static OptionsList.Entry editIgnoreList(OptionsList.Entry entry, String fileName, Runnable defaultFactory) {
		Objects.requireNonNull(entry.mainWidget()).setWidth(79);
		GuiButton editButton = new GuiButton(0, 0, 0, 20, 20, "☰") {
			@Override
			public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
				if (super.mousePressed(mc, mouseX, mouseY)) {
					new Thread(() -> {
						try {
							Thread.sleep(500);
						} catch (InterruptedException ignored) {
						}
						JadeClient.pleaseWait();
					}).start();
					File file = new File(CommonProxy.getConfigDirectory(), String.format("jade/%s.json", fileName));
					if (!file.exists()) {
						defaultFactory.run();
					}
					try {
						java.awt.Desktop.getDesktop().open(file);
					} catch (IOException e) {
						Jade.LOGGER.error("Failed to open config file %s".formatted(file), e);
					}
					return true;
				}
				return false;
			}
		};
		// 1.12.2: the "edit ignore list" hint is exposed through the entry description (hover) since there is
		// no widget-owned tooltip system; the button itself carries the ☰ label.
		entry.addWidget(new JadeWidget.Button(editButton), 80);
		entry.addMessage("☰");
		return entry;
	}

	@Override
	public OptionsList createOptions(OptionsList options) {
		IWailaConfig.General general = IWailaConfig.get().general();
		options.title("general");
		if (CommonProxy.isDevEnv()) {
			options.choices("debug_mode", general::isDebug, general::setDebug);
		}
		options.choices("display_tooltip", general::shouldDisplayTooltip, general::setDisplayTooltip);
		OptionsList.Entry entry = options.choices("display_entities", general::getDisplayEntities, general::setDisplayEntities);
		editIgnoreList(entry, "hide-entities", () -> WailaClientRegistration.instance().reloadIgnoreLists());
		options.choices("display_bosses", general::getDisplayBosses, general::setDisplayBosses).parent(entry);
		entry = options.choices("display_blocks", general::getDisplayBlocks, general::setDisplayBlocks);
		editIgnoreList(entry, "hide-blocks", () -> WailaClientRegistration.instance().reloadIgnoreLists());
		options.choices("display_fluids", general::getDisplayFluids, general::setDisplayFluids);
		options.choices("display_mode", general::getDisplayMode, general::setDisplayMode);
		OptionValue<?> value = options.choices("item_mod_name", general::showItemModNameTooltip, general::setItemModNameTooltip);
		List<String> modNames = ClientProxy.metadata.disableItemModNameTooltip().stream()
				.map($ -> ModIdentification.getModFullName($).orElse($))
				.toList();
		if (!modNames.isEmpty()) {
			value.setDisabled(true);
			value.appendDescription(new TextComponentTranslation("gui.jade.disabled_by_mods"));
			modNames.stream().map(TextComponentString::new).forEach(value::appendDescription);
			// 1.12.2: the multi-line description is rendered by the owning screen's hover machinery (the entry
			// description list), so there is no widget-owned Tooltip to attach.
		}

		options.choices("hide_from_guis", general::shouldHideFromGUIs, general::setHideFromGUIs);
		options.choices("boss_bar_overlap", general::getBossBarOverlapMode, general::setBossBarOverlapMode);
		options.slider("reach_distance", general::getExtendedReach, general::setExtendedReach, 0, 20, f -> MathHelper.floor(f * 2) / 2F);
		options.choices("perspective_mode", general::getPerspectiveMode, general::setPerspectiveMode);

		IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
		options.title("overlay");
		ITextComponent adjust = new TextComponentTranslation(OptionsList.Entry.makeKey("overlay_pos.adjust"));
		options.add(new OptionButton(
				new TextComponentTranslation(OptionsList.Entry.makeKey("overlay_pos")),
				new GuiButton(0, 0, 0, 100, 20, adjust.getFormattedText()) {
					@Override
					public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
						if (super.mousePressed(mc, mouseX, mouseY)) {
							startAdjustingPosition();
							return true;
						}
						return false;
					}
				}));
		// theme list supplier: all themes sharing the current main id, re-read on every
		// update so applyTheme can change the available style list (1.12.2: no
		// CycleButton.ValueListSupplier, so the modern supplier is emulated here)
		OptionValue<ResourceLocation> themeEntry = options.add(new CycleOptionValue<>(
				"overlay_theme",
				new CycleOptionValue.Builder<>(
						id -> new TextComponentTranslation("jade.theme." + id.getNamespace() + "." + id.getPath()),
						IThemeHelper.get().getThemes().stream()
								.filter($ -> $.styleId().isEmpty())
								.map(Theme::fullId)
								.toList(),
						overlay.getTheme().mainId()),
				() -> overlay.getTheme().mainId(),
				id -> {
					if (Objects.equals(id, overlay.getTheme().mainId())) {
						return;
					}
					if (!ThemeHelper.INSTANCE.hasTheme(id)) {
						return;
					}
					overlay.applyTheme(id);
					Theme theme = overlay.getTheme();
					if (theme.changeOpacity != 0) {
						Objects.requireNonNull(opacityEntry).setValue(theme.changeOpacity);
					}
					Objects.requireNonNull(styleEntry).updateValue();
				}));
		CycleOptionValue.Builder<ResourceLocation> styleBuilder = new CycleOptionValue.Builder<>(
				id -> new TextComponentTranslation(ThemeHelper.INSTANCE.getTheme(id).styleName),
				null,
				overlay.getTheme().fullId());
		styleEntry = options.add(new CycleOptionValue<>(
				"theme_style",
				styleBuilder,
				() -> Objects.requireNonNull(overlay.getTheme().id),
				overlay::applyTheme) {
			@Override
			public void updateValue() {
				styleBuilder.values = IThemeHelper.get().getThemes().stream()
						.filter($ -> $.mainId().equals(overlay.getTheme().mainId()))
						.map(Theme::fullId)
						.toList();
				super.updateValue();
				if (styleBuilder.values.size() > 1) {
					button.setActive(true);
				} else {
					button.setActive(false);
					button.setMessage(new TextComponentTranslation("jade.unavailable"));
				}
			}
		});
		styleEntry.parent(themeEntry);
		styleEntry.updateValue();
		opacityEntry = options.slider("overlay_alpha", overlay::getAlpha, overlay::setAlpha);
		options.forcePreview.add(options.slider(
				"overlay_scale",
				overlay::getOverlayScale,
				overlay::setOverlayScale,
				0.2f,
				2,
				FloatUnaryOperator.identity()));
		options.choices("display_item", overlay::getIconMode, overlay::setIconMode);
		options.choices("animation", overlay::getAnimation, overlay::setAnimation);

		options.title("key_binds");
		options.keybind(JadeKeys.openConfig());
		options.keybind(JadeKeys.showOverlay());
		options.keybind(JadeKeys.toggleLiquid());
		if (JadeKeys.hasRecipeViewerKeys()) {
			options.keybind(JadeKeys.showRecipes());
			options.keybind(JadeKeys.showUses());
		}
		options.keybind(JadeKeys.narrate());
		options.keybind(JadeKeys.showDetails());

		IWailaConfig.Accessibility accessibility = IWailaConfig.get().accessibility();
		options.title("accessibility");
		options.choices("accessibility_plugin", accessibility::getEnableAccessibilityPlugin, accessibility::setEnableAccessibilityPlugin);
		options.choices("tts_mode", accessibility::getTTSMode, accessibility::setTTSMode);
		options.choices("narrate_keys", accessibility::getNarrateKeys, accessibility::setNarrateKeys);
		options.slider("text_background_opacity", accessibility::getTextBackgroundOpacity, accessibility::setTextBackgroundOpacity);
		options.choices("flip_main_hand", accessibility::getFlipMainHand, accessibility::setFlipMainHand);

		OptionsList.Title dangerZone = options.title("danger_zone");
		dangerZone.setTitle(dangerZone.title().createCopy().setStyle(new Style().setColor(TextFormatting.RED)));
		options.add(new OptionButton(
				"reload_plugins", new GuiButton(0, 0, 0, 100, 20, OptionsList.Entry.makeTitle("reload_plugins.button").getFormattedText()) {
					@Override
					public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
						if (super.mousePressed(mc, mouseX, mouseY)) {
							enabled = false;
							Jade.loadPlugins();
							enabled = true;
							mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
									SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
							return true;
						}
						return false;
					}
				}));
		ITextComponent reset = new TextComponentTranslation("controls.reset").setStyle(new Style().setColor(TextFormatting.RED));
		ITextComponent title = new TextComponentTranslation(OptionsList.Entry.makeKey("reset_settings")).setStyle(new Style().setColor(TextFormatting.RED));
		GuiYesNoCallback callback = new GuiYesNoCallback() {
			@Override
			public void confirmClicked(boolean bl, int id) {
				if (bl) {
					for (KeyBinding keyMapping : mc.gameSettings.keyBindings) {
						if (JadeKeys.openConfig().getKeyCategory().equals(keyMapping.getKeyCategory())) {
							keyMapping.setToDefault();
						}
					}
					mc.gameSettings.saveOptions();
					try {
						Jade.resetConfig();
						initGui();
					} catch (Throwable e) {
						Jade.LOGGER.error("", e);
					}
				}
				mc.displayGuiScreen(WailaConfigScreen.this);
				options().setScrollAmount(options().getMaxScroll());
			}
		};
		options.add(new OptionButton(
				title, new GuiButton(0, 0, 0, 100, 20, reset.getFormattedText()) {
					@Override
					public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
						if (super.mousePressed(mc, mouseX, mouseY)) {
							mc.displayGuiScreen(new GuiYesNo(
									callback,
									title.getFormattedText(),
									new TextComponentTranslation(OptionsList.Entry.makeKey("reset_settings.confirm")).getFormattedText(),
									reset.getFormattedText(),
									I18n.format("gui.cancel"),
									0));
							return true;
						}
						return false;
					}
				}));

		return options;
	}
}
