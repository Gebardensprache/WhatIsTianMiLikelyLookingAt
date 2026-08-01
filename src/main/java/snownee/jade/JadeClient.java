package snownee.jade;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;
import com.ibm.icu.text.MessageFormat;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.Timer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.input.Keyboard;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.config.IWailaConfig.DisplayMode;
import snownee.jade.api.config.IWailaConfig.Overlay;
import snownee.jade.api.config.IWailaConfig.TTSMode;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.ColorPalette;
import snownee.jade.api.ui.ScreenDirection;
import snownee.jade.api.ui.TooltipAnimation;
import snownee.jade.compat.RecipeLookupPlugin;
import snownee.jade.compat.RecipeLookupResult;
import snownee.jade.gui.HomeConfigScreen;
import snownee.jade.key_extension.KeyMappingEx;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.overlay.WailaTickHandler;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.ModIdentification;
import snownee.jade.util.WailaExceptionHandler;

public final class JadeClient {

	/** 1.12.2: profile slots; inactive slots are unbound (keyCode 0). */
	public static final KeyBinding[] profiles = new KeyBinding[4];
	public static @Nullable KeyBinding openConfig;
	public static @Nullable KeyBinding showOverlay;
	public static @Nullable KeyBinding toggleLiquid;
	public static @Nullable KeyBinding showDetails;
	public static @Nullable KeyBinding narrate;
	public static @Nullable KeyBinding showRecipes;
	public static @Nullable KeyBinding showUses;
	private static final WailaTickHandler tickHandler = new WailaTickHandler();
	private static final List<RecipeLookupPlugin> recipeLookupPlugins = Lists.newArrayList();
	private static float savedProgress;
	private static float progressAlpha;
	private static boolean canHarvest;

	public static void init() {
		openConfig = ClientProxy.registerKeyBinding("config", Keyboard.KEY_NUMPAD0);
		showOverlay = ClientProxy.registerKeyBinding("show_overlay", Keyboard.KEY_NUMPAD1);
		toggleLiquid = ClientProxy.registerKeyBinding("toggle_liquid", Keyboard.KEY_NUMPAD2);
		if (JadeKeys.hasRecipeViewerKeys()) {
			showRecipes = ClientProxy.registerKeyBinding("show_recipes", Keyboard.KEY_NUMPAD3);
			showUses = ClientProxy.registerKeyBinding("show_uses", Keyboard.KEY_NUMPAD4);
		}
		narrate = ClientProxy.registerKeyBinding("narrate", Keyboard.KEY_NUMPAD5);
		showDetails = ClientProxy.registerKeyBinding("show_details", Keyboard.KEY_LSHIFT);
		for (int i = 0; i < 4; i++) {
			// 1.12.2: profiles are unbound until the user assigns keys in the Controls menu.
			profiles[i] = ClientProxy.registerKeyBinding("profile." + i, 0);
		}
	}

	public static WailaTickHandler tickHandler() {
		return tickHandler;
	}

	/**
	 * Legacy per-tick polling hook, invoked from {@code ClientProxy.onKeyPressed}
	 * ({@code InputEvent.KeyInputEvent}). Activations are consumed with
	 * {@link KeyBinding#isPressed()}, matching the 1.12.2 input model.
	 */
	public static void onKeyPressed() {
		Minecraft mc = Minecraft.getMinecraft();
		while (JadeKeys.openConfig().isPressed()) {
			Jade.invalidateConfig();
			ItemStorageProvider.targetCache.invalidateAll();
			ItemStorageProvider.containerCache.invalidateAll();
			// 1.12.2: open the in-game config GUI (the modern gui tree), reachable the
			// same way as the mod list's Config button (JadeGuiFactory).
			mc.displayGuiScreen(new HomeConfigScreen(mc.currentScreen));
		}

		while (JadeKeys.showOverlay().isPressed()) {
			IWailaConfig.General general = IWailaConfig.get().general();
			DisplayMode mode = general.getDisplayMode();
			if (mode == DisplayMode.TOGGLE) {
				general.setDisplayTooltip(!general.shouldDisplayTooltip());
				if (!general.shouldDisplayTooltip() && Jade.history().hintOverlayToggle) {
					mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
							formatString("toast.jade.toggle_hint.1")));
					mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
							formatString("toast.jade.toggle_hint.2", JadeKeys.showOverlay().getDisplayName())));
					Jade.history().hintOverlayToggle = false;
				}
				narrateKey("show_overlay", general.shouldDisplayTooltip());
				IWailaConfig.get().save();
			}
		}

		while (JadeKeys.toggleLiquid().isPressed()) {
			IWailaConfig.General general = IWailaConfig.get().general();
			general.setDisplayFluids(!general.shouldDisplayFluids());
			narrateKey("toggle_liquid", general.shouldDisplayFluids());
			IWailaConfig.get().save();
		}

		while (JadeKeys.narrate().isPressed()) {
			IWailaConfig.Accessibility accessibility = IWailaConfig.get().accessibility();
			if (accessibility.getTTSMode() == TTSMode.TOGGLE) {
				accessibility.toggleTTS();
				if (accessibility.shouldEnableTextToSpeech() && Jade.history().hintNarratorToggle) {
					mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
							formatString("toast.jade.tts_hint.1")));
					mc.ingameGUI.getChatGUI().printChatMessage(new TextComponentString(
							formatString("toast.jade.tts_hint.2", JadeKeys.narrate().getDisplayName())));
					Jade.history().hintNarratorToggle = false;
				}
				IWailaConfig.get().save();
			} else if (tickHandler.rootElement != null) {
				tickHandler.narrate(tickHandler.rootElement, false);
			}
		}

		if (Jade.rootConfig().isEnableProfiles()) {
			for (int i = 0; i < 4; i++) {
				while (profiles[i].isPressed()) {
					Jade.useProfile(i);
					if (IWailaConfig.get().accessibility().getNarrateKeys()) {
						tickHandler.narrate(formatString("narration.jade.key.profile", profiles[i].getKeyDescription()), false);
					}
				}
			}
		}

		if (JadeKeys.hasRecipeViewerKeys()) {
			while (JadeKeys.showUses().isPressed()) {
				lookupRecipes(true);
			}
			while (JadeKeys.showRecipes().isPressed()) {
				lookupRecipes(false);
			}
		}
	}

	/**
	 * Looks up recipes (or uses) for the currently targeted item through the first
	 * recipe-viewer plugin that accepts it.
	 * <p>
	 * 1.12.2 port of the modern {@code lookupRecipes}; the flow is unchanged except
	 * that {@code ModIdentification.getSpecialId} is a no-op here (no modern
	 * data-component special IDs).
	 */
	public static void lookupRecipes(boolean uses) {
		if (recipeLookupPlugins.isEmpty() || tickHandler.state == null) {
			return;
		}		ItemStack itemStack = tickHandler.state.accessor().getPickedResult();
		if (itemStack.isEmpty()) {
			return;
		}
		RecipeLookupResult selected = null;
		List<RecipeLookupResult> results = Lists.newArrayList();
		for (RecipeLookupPlugin plugin : recipeLookupPlugins) {
			try {
				RecipeLookupResult result = plugin.lookup(itemStack, null, uses);
				if (result.isFail()) {
					continue;
				}
				results.add(result);
				if (selected == null || result.score() > selected.score()) {
					selected = result;
				}
			} catch (Throwable e) {
				WailaExceptionHandler.handleErr(e, null, null);
			}
		}
		if (selected == null) {
			return;
		}
		try {
			selected.action().accept(Minecraft.getMinecraft().currentScreen, results);
		} catch (Throwable e) {
			WailaExceptionHandler.handleErr(e, null, null);
		}
	}

	/**
	 * Renders the harvest/block-breaking progress line under the probe box.
	 * <p>
	 * 1.12.2 port of modern {@code drawBreakingProgress}. Registered as an
	 * after-render callback ({@code VanillaPlugin}) so it runs inside the overlay
	 * pass's translated matrix space; the line is drawn in root-box coordinates
	 * with {@link DisplayHelper#fill}.
	 * <p>
	 * Modern reads {@code MultiPlayerGameMode.destroyProgress /
	 * destroyBlockPos / isDestroying()}; 1.12.2's {@code PlayerControllerMP}
	 * keeps the equivalent state in the private fields {@code curBlockDamageMP}
	 * (the running accumulated damage) and {@code currentBlock}, made public by
	 * access transformers ({@code jade_at.cfg}). {@code getIsHittingBlock()} is
	 * public.
	 */
	public static void drawBreakingProgress(
			BoxElement root,
			TooltipAnimation animation,
			Accessor<?> accessor) {
		if (!IWailaConfig.get().plugin().get(JadeIds.MC_BREAKING_PROGRESS)) {
			progressAlpha = 0;
			return;
		}
		if (!Float.isNaN(root.getBoxProgress())) {
			progressAlpha = 0;
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		PlayerControllerMP playerController = mc.playerController;
		if (playerController == null || mc.world == null || mc.player == null) {
			return;
		}
		BlockPos pos = playerController.currentBlock;
		IBlockState state = mc.world.getBlockState(pos);
		if (playerController.getIsHittingBlock()) {
			canHarvest = CommonProxy.isCorrectToolForDrops(state, mc.player, mc.world, pos);
		} else if (progressAlpha == 0) {
			return;
		}
		Theme theme = IThemeHelper.get().theme();
		ColorPalette colors = theme.tooltipStyle.boxProgressColors;
		int color = canHarvest ? colors.title() : colors.failure();
		float top = root.getY() + root.getHeight();
		float width = root.getWidth();
		// 1.12.2: two distinct time bases, mirroring modern's DeltaTracker. Modern fades the
		// bar by getGameTimeDeltaTicks() (the frame's real duration in ticks) -- that is
		// Timer.elapsedPartialTicks here. But modern interpolates the breaking damage by
		// getGameTimeDeltaPartialTick(false), the 0..1 fraction within the current game tick.
		// PlayerControllerMP.curBlockDamageMP accumulates once per tick in onPlayerDamageBlock,
		// so the renderer must interpolate it with Timer.renderPartialTicks (also 0..1);
		// elapsedPartialTicks there would jump the bar by a whole tick's worth per frame.
		Timer timer = mc.timer;
		progressAlpha += timer.elapsedPartialTicks * (playerController.getIsHittingBlock() ? 0.1F : -0.1F);
		if (playerController.getIsHittingBlock()) {
			progressAlpha = Math.min(progressAlpha, 0.6F);
			float progress = state.getPlayerRelativeBlockHardness(mc.player, mc.world, pos);
			float curBlockDamageMP = playerController.curBlockDamageMP;
			if (curBlockDamageMP + progress >= 1) {
				progressAlpha = savedProgress = 1;
			} else {
				progress = curBlockDamageMP + timer.renderPartialTicks * progress;
				savedProgress = MathHelper.clamp(progress, 0, 1);
			}
		} else {
			progressAlpha = Math.max(progressAlpha, 0);
		}
		if (progressAlpha == 0) {
			return;
		}
		color = Overlay.applyAlpha(color, progressAlpha);
		float offset0 = theme.tooltipStyle.boxProgressOffset(ScreenDirection.UP);
		float offset1 = theme.tooltipStyle.boxProgressOffset(ScreenDirection.RIGHT);
		float offset2 = theme.tooltipStyle.boxProgressOffset(ScreenDirection.DOWN);
		float offset3 = theme.tooltipStyle.boxProgressOffset(ScreenDirection.LEFT);
		width += offset1 - offset3;
		DisplayHelper.fill(offset3, top - 1 + offset0, offset3 + width * savedProgress, top + offset2, color);
	}

	/**
	 * Instantiates and registers a {@link RecipeLookupPlugin} by class name.
	 */
	public static void addRecipeLookupPlugin(String clazz) {
		try {
			recipeLookupPlugins.add((RecipeLookupPlugin) Class.forName(clazz).getDeclaredConstructor().newInstance());
		} catch (Throwable e) {
			if (CommonProxy.isDevEnv()) {
				Jade.LOGGER.warn("Failed to load recipe lookup plugin: {}", clazz, e);
			}
		}
	}

	public static void narrateKey(String key, boolean bl) {
		if (IWailaConfig.get().accessibility().getNarrateKeys()) {
			key = "narration.jade.key.%s.%s".formatted(key, bl ? "on" : "off");
			tickHandler.narrate(I18n.format(key), false);
		}
	}

	/**
	 * Appends the owning mod's name to an item tooltip line.
	 * <p>
	 * 1.12.2: {@code ItemTooltipEvent} carries a legacy {@code ItemStack} and a
	 * {@code List<String>} tooltip; returns a legacy {@link ITextComponent} so the
	 * caller can append {@code getFormattedText()}. The modern creative-tab dedup
	 * context does not exist here and is intentionally not emulated.
	 */
	@Nullable
	public static ITextComponent appendModName(ItemStack itemStack) {
		if (!IWailaConfig.get().general().showItemModNameTooltip()) {
			return null;
		}
		if (itemStack.isEmpty()) {
			return null;
		}
		String name;
		try {
			name = ModIdentification.getModName(itemStack);
		} catch (Throwable e) {
			WailaExceptionHandler.handleErr(e, null, null);
			return null;
		}
		Style style = IWailaConfig.get().formatting().getItemModNameStyle();
		return new TextComponentString(name).setStyle(style == null ? new Style() : style);
	}

	/**
	 * Formats a translation key with the ICU message syntax used by Jade's lang files
	 * ({@code {0}} placeholders), returning a legacy text component.
	 */
	public static ITextComponent format(String s, Object... objects) {
		return new TextComponentString(formatString(s, objects));
	}

	/**
	 * Formats a translation key with the ICU message syntax used by Jade's lang files.
	 * <p>
	 * 1.12.2: {@code I18n.format} itself handles {@code %s} placeholders only, so the
	 * ICU-format translation strings are post-processed through
	 * {@link MessageFormat} like upstream does.
	 */
	public static String formatString(String s, Object... objects) {
		try {
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof ITextComponent component) {
					objects[i] = component.getFormattedText();
				}
			}
			return MessageFormat.format(I18n.format(s), objects);
		} catch (Exception e) {
			return I18n.format(s, objects);
		}
	}

	/**
	 * Retained as a harmless no-op for the parked-GUI ABI ({@code WailaConfigScreen}
	 * calls it from a background thread). The modern {@code SystemToast} machinery has
	 * no 1.12.2 counterpart and no toast is wanted here anyway.
	 */
	public static void pleaseWait() {
	}

	public static void refreshKeyState() {
		boolean active = Jade.rootConfig().isEnableProfiles();
		for (KeyBinding keyBinding : profiles) {
			KeyMappingEx.setActive(keyBinding, active);
		}
	}

	/**
	 * Returns a runnable that restores every {@link KeyBinding} matching {@code predicate}
	 * to the key code it had when this method was called.
	 * <p>
	 * 1.12.2: mirrors the modern {@code KeyMapping.setKey} rollback the config screen uses
	 * for its canceller. Key codes come from the {@link KeyMappingEx} duck
	 * ({@code keyEx$key()}); restoring clears the modifier (the modifier is part of the
	 * modern {@code Key} but the legacy binding stores it separately).
	 */
	public static Runnable recoverKeysAction(Predicate<KeyBinding> predicate) {
		Map<KeyBinding, Integer> keyMap = new java.util.HashMap<>();
		for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
			if (predicate.test(keyBinding)) {
				keyMap.put(keyBinding, ((KeyMappingEx) keyBinding).keyEx$key());
			}
		}
		return () -> keyMap.forEach((keyBinding, keyCode) ->
				keyBinding.setKeyModifierAndCode(KeyModifier.NONE, keyCode));
	}
}
