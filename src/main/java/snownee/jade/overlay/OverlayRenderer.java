package snownee.jade.overlay;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.JadeInternals;
import snownee.jade.api.JadeIds;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.callback.JadeAfterRenderCallback;
import snownee.jade.api.callback.JadeBeforeRenderCallback;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.config.IWailaConfig.BossBarOverlapMode;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.LayoutElement;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.api.ui.TooltipAnimation;
import snownee.jade.gui.BaseOptionsScreen;
import snownee.jade.gui.PreviewOptionsScreen;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.config.WailaConfig.General;
import snownee.jade.impl.ui.BoxElementImpl;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.ModIdentification;

public class OverlayRenderer {

	public static final TooltipAnimation animation = new TooltipAnimation();
	public static float ticks;
	public static boolean shown;

	/**
	 * Style of the text the mouse is currently over, consumed by the overlay to decide whether to
	 * show a hover tooltip.
	 * <p>
	 * 1.12.2: relocated here from the deleted {@code GuiGraphicsExtractor} facade -- the modern
	 * graphics-context parameter that carried this state is gone, so it lives as shared static state
	 * on the renderer that both sets it (via {@link Element#setHoverEffect}) and consumes it.
	 */
	public static @Nullable Style hoveredTextStyle;

	/**
	 * Tooltip deferred to after the main overlay pass.
	 * <p>
	 * 1.12.2: relocated here from the deleted {@code GuiGraphicsExtractor} facade for the same reason
	 * as {@link #hoveredTextStyle}.
	 */
	public static @Nullable ITextComponent deferredTooltip;

	private static @Nullable BoxElementImpl lingerTooltip;
	private static float disappearTicks;

	/**
	 * 1.12.2: upstream collects deferred {@code GuiElementRenderState}s and replays them after the main
	 * pass. With immediate-mode rendering there is nothing buffered to replay, so this only surfaces the
	 * deferred tooltip.
	 */
	public static @Nullable ITextComponent extractDeferredElements() {
		ITextComponent tooltip = deferredTooltip;
		deferredTooltip = null;
		return tooltip;
	}

	public static boolean shouldShow() {
		if (JadeClient.tickHandler().rootElement == null) {
			return false;
		}

		IWailaConfig.General general = IWailaConfig.get().general();
		if (!general.shouldDisplayTooltip()) {
			return false;
		}

		if (general.getDisplayMode() == IWailaConfig.DisplayMode.HOLD_KEY && !JadeKeys.showOverlay().isKeyDown()) {
			return false;
		}

		BossBarOverlapMode mode = general.getBossBarOverlapMode();
		if (mode == BossBarOverlapMode.HIDE_TOOLTIP && !(Minecraft.getMinecraft().currentScreen instanceof BaseOptionsScreen) &&
				ClientProxy.getBossBarRect() != null) {
			return false;
		}

		return true;
	}

	public static boolean shouldShowImmediately(BoxElementImpl box) {
		if (box.getTooltip().isEmpty()) {
			return false;
		}

		Minecraft mc = Minecraft.getMinecraft();

		if (ClientProxy.shouldHideWithGui(mc, mc.currentScreen)) {
			return false;
		}

		box.updateExpectedRect(animation);
		Object currentScreen = mc.currentScreen; // 1.12.2: parked modern gui/ classes; test through Object
		if (currentScreen instanceof PreviewOptionsScreen) {
			PreviewOptionsScreen optionsScreen = (PreviewOptionsScreen) currentScreen;
			if (optionsScreen.forcePreviewOverlay()) {
				return true;
			}
			if (!Jade.history().previewOverlay) {
				return false;
			}
			ScaledResolution resolution = new ScaledResolution(mc);
			int mouseX = Mouse.getX() * resolution.getScaledWidth() / mc.displayWidth;
			int mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / mc.displayHeight - 1;
			if (animation.expectedRect.contains((float) mouseX, (float) mouseY)) {
				return false;
			}
		}

		General general = Jade.config().general();
		// 1.12.2: no mc.gui.overlay() overlay system / hud hidden API; skip those checks
		// 1.12.2: no tab list visibility check

		return true;
	}

	/**
	 * NOTE!!!
	 * <p>
	 * Please do NOT replace the whole codes with Mixin.
	 * It will make me unable to locate bugs.
	 * A regular plugin can also realize the same features.
	 * <p>
	 * Secondly, please notice the license that Jade is using.
	 * I don't think it is compatible with some open-source licenses.
	 */
	public static void renderOverlay478757(float delta) {
		ticks += delta;
		shown = false;
		BoxElementImpl root = JadeClient.tickHandler().rootElement;
		boolean show;
		if (root == null && PreviewOptionsScreen.isAdjustingPosition()) {
			Tooltip tooltip = new Tooltip();
			tooltip.add(IThemeHelper.get().title(new TextComponentString(Blocks.GRASS.getLocalizedName())));
			tooltip.add(IThemeHelper.get().modNameElement(ModIdentification.getModName(Blocks.GRASS)));
			Theme theme = IThemeHelper.get().theme();
//			tooltip.setIcon(theme.modifyIcon(JadeUI.item(new ItemStack(Blocks.GRASS))));
			root = new BoxElementImpl(tooltip, theme.tooltipStyle);
			root.tag(JadeIds.ROOT);
			root.updateExpectedRect(animation);
			show = true;
		} else {
			show = shouldShow();
		}
		IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
		IWailaConfig.General general = IWailaConfig.get().general();
		if (root != null) {
			lingerTooltip = root;
		}
		if (root == null && lingerTooltip != null) {
			disappearTicks += delta;
			if (disappearTicks < overlay.getDisappearingDelay()) {
				root = lingerTooltip;
				show = true;
			}
		} else {
			disappearTicks = 0;
		}
		if (overlay.getAnimation() && lingerTooltip != null) {
			root = lingerTooltip;
			float speed = general.isDebug() ? 0.1F : 0.6F;
			animation.showHideAlpha += (show ? speed : -speed) * delta;
			animation.showHideAlpha = MathHelper.clamp(animation.showHideAlpha, 0, 1);
		} else {
			animation.showHideAlpha = show ? 1 : 0;
		}

		if (root == null) {
			return;
		}

		if (animation.showHideAlpha < 0.1F || !shouldShowImmediately(root)) {
			if (!PreviewOptionsScreen.isAdjustingPosition()) {
				lingerTooltip = null;
				animation.rect.setWidth(0); // mark dirty
				return;
			}
		}

		int mouseX = -1;
		int mouseY = -1;
		Minecraft mc = Minecraft.getMinecraft();
		if (JadeUI.isPinned()) {
			ScaledResolution resolution = new ScaledResolution(mc);
			mouseX = Mouse.getX() * resolution.getScaledWidth() / mc.displayWidth;
			mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / mc.displayHeight - 1;
		}

		mc.profiler.startSection("Jade Overlay");
		renderOverlay(root, mouseX, mouseY, delta); //TODO pass correct mouseX, mouseY
		mc.profiler.endSection();
	}

	public static void renderOverlay(BoxElementImpl root, int mouseX, int mouseY, float partialTicks) {
		root.updateRect(animation);

		WailaTickHandler tickHandler = JadeClient.tickHandler();
		if (tickHandler.state != null) {
			for (JadeBeforeRenderCallback callback : WailaClientRegistration.instance().beforeRenderCallback.callbacks()) {
				if (callback.beforeRender(root, animation, tickHandler.state.accessor())) {
					return;
				}
			}
		}

		boolean renderDebug = IWailaConfig.get().general().isDebug() && JadeUI.hasControlDown();
		if (renderDebug) {
			Rect2f bossBarRect = ClientProxy.getBossBarRect();
			if (bossBarRect != null) {
				JadeInternals.getDisplayHelper().drawBorder(bossBarRect, 2, 0x88FF00FF, true);
			}
		}

		GlStateManager.pushMatrix();
		Rect2f rect = animation.rect;
		GlStateManager.translate(rect.getX(), rect.getY(), 0.0F);

		// 1.12.2: the overlay is a 2D canvas. Depth testing is off for the whole pass so
		// screen-space elements (background, text, the harvest ✓/✕) draw in layout order
		// on top of each other instead of being culled by the item icon's Z~100 quad
		// (which, with the test on, writes its silhouette into the depth buffer). Only
		// 3D item icons re-enable test+mask locally (DisplayHelper.drawItem) so their
		// faces still sort correctly.
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		GlStateManager.disableDepth();
		GlStateManager.depthMask(false);

		float scale = animation.scale;
		if (scale != 1f) {
			GlStateManager.scale(scale, scale, 1.0F);
		}

		int mappedMouseX = mouseX;
		int mappedMouseY = mouseY;
		if (mouseX != -1) {
			int[] mapped = animation.mapMousePosition(mouseX, mouseY, (x, y) -> new int[]{x.intValue(), y.intValue()});
			if (mapped != null) {
				mappedMouseX = mapped[0];
				mappedMouseY = mapped[1];
			}
		}

		root.setWidgetAlpha(animation.alpha);
		deferredTooltip = null;
		root.extractRenderState(mappedMouseX, mappedMouseY, partialTicks);
		if (renderDebug) {
			root.renderDebug(mappedMouseX, mappedMouseY, partialTicks, new Element.RenderDebugContext(root, rect, true));
		} else if (JadeUI.isPinned() && JadeUI.hasControlDown()) {
			Optional<? extends LayoutElement> child = root.getChildAt(mappedMouseX, mappedMouseY);
			if (child.isPresent() && child.get() instanceof Element) {
				Element element = (Element) child.get();
				element.renderDebug(mappedMouseX, mappedMouseY, partialTicks, new Element.RenderDebugContext(root, rect, false));
			}
		}

		if (tickHandler.state != null) {
			WailaClientRegistration.instance().afterRenderCallback.call(new java.util.function.Consumer<JadeAfterRenderCallback>() {
				@Override
				public void accept(JadeAfterRenderCallback callback) {
					callback.afterRender(root, animation, tickHandler.state.accessor());
				}
			});
		}

		GlStateManager.popMatrix();

		GlStateManager.depthMask(true);
		if (depthTest) {
			GlStateManager.enableDepth();
		}

		extractDeferredElements();

		if (IWailaConfig.get().accessibility().shouldEnableTextToSpeech()) {
			tickHandler.narrate(root, true);
		}

		shown = true;
	}

	public static void clearLingerTooltip() {
		lingerTooltip = null;
	}
}
