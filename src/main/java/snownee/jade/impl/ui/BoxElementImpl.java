package snownee.jade.impl.ui;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.JadeInternals;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.Layout;
import snownee.jade.api.ui.LayoutElement;
import snownee.jade.api.ui.MessageType;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.api.ui.Renderable;
import snownee.jade.api.ui.ResizeableElement;
import snownee.jade.api.ui.ScreenDirection;
import snownee.jade.api.ui.TooltipAnimation;
import snownee.jade.gui.JadeLinearLayout;
import snownee.jade.gui.LayoutSettings;
import snownee.jade.gui.LayoutWithPadding;
import snownee.jade.gui.PreviewOptionsScreen;
import snownee.jade.gui.ResizeableLayout;
import snownee.jade.impl.Tooltip;
import snownee.jade.track.ProgressTrackInfo;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.ToFloatFunction;
import snownee.jade.util.WailaExceptionHandler;

/**
 * 1.12.2: the following modern features are dropped from this class:
 * <ul>
 *   <li>{@code implements ContainerEventHandler} and its 5 no-op methods ({@code children},
 *       {@code isDragging}, {@code setDragging}, {@code getFocused}, {@code setFocused}) --
 *       confirmed to have zero external callers in the translated codebase.</li>
 *   <li>{@code visitWidgets(Consumer<AbstractWidget>)} -- no 1.12.2 layout-target exists</li>
 *   <li>{@code GuiGraphicsExtractor} parameter dropped from {@code extractRenderState} and
 *       {@code renderDebug} per the backport convention</li>
 *   <li>Scissor clipping ({@code graphics.enableScissor/disableScissor}) dropped -- 1.12.2's GL
 *       scissor does not auto-transform from the layout's local coordinate space.</li>
 *   <li>Widget-alpha propagation ({@code setWidgetAlpha}) is a no-op; global alpha is already
 *       applied by every draw helper via {@link IDisplayHelper#opacity()}.</li>
 *   <li>{@code sneakyDetails} rendering (line ~192) references the parked
 *       {@code api/theme/SneakyDetails} class -- the {@code GuiGraphicsExtractor} argument is
 *       dropped but the remaining reference will not compile until {@code api/theme/} is
 *       translated (B9; tracked as out-of-scope breakage per Rule 0).</li>
 * </ul>
 */
public class BoxElementImpl extends BoxElement {
	public LayoutWithPadding layout;
	private final Tooltip tooltip;
	private final BoxStyle style;
	private final List<Renderable> renderables;
	private @Nullable Element icon;
	private float boxProgress;
	private @Nullable MessageType boxProgressType;
	private @Nullable ProgressTrackInfo track;

	public BoxElementImpl(Tooltip tooltip, BoxStyle style) {
		this.tooltip = Objects.requireNonNull(tooltip);
		this.style = Objects.requireNonNull(style);
		this.icon = tooltip.getIcon();
		renderables = Lists.newArrayListWithExpectedSize(tooltip.size() + 1);
		updateSize();
	}

	@Override
	public void updateSize() {
		JadeLinearLayout linearLayout = JadeLinearLayout.vertical().alignItems(JadeLinearLayout.Align.STRETCH);
		for (Tooltip.Line line : tooltip.lines) {
			JadeLinearLayout lineLayout = JadeLinearLayout.horizontal();
			for (LayoutElement element : line.elements()) {
				if (element instanceof ResizeableElement resizeableElement) {
					resizeableElement.updateSize();
				}
				lineLayout.addChild(
						element, lineLayout.newChildLayoutSettings(element), container -> {
							if (container.child instanceof Element element0 && element0.getAlignSelf() != null) {
								container.alignSelf = element0.getAlignSelf();
							}
							if (container.child instanceof ResizeableLayout resizeableLayout) {
								container.flexGrow = resizeableLayout.getFlexGrow();
							}
						});
			}
			LayoutSettings lineSettings = linearLayout.newChildLayoutSettings(lineLayout);
			if (line.settings != null) {
				lineSettings = (LayoutSettings) line.settings.apply(lineSettings);
			}
			linearLayout.addChild(
					lineLayout, lineSettings, container -> {
						container.headMargin = line.marginTop;
						container.tailMargin = line.marginBottom;
					});
		}
		tooltip.isDirty = false;

		if (icon != null) {
			JadeLinearLayout iconLayout = JadeLinearLayout.horizontal().alignItems(JadeLinearLayout.Align.START).spacing(3);
			IWailaConfig.IconMode iconMode = IWailaConfig.get().overlay().getIconMode();
			if (iconMode == IWailaConfig.IconMode.CENTERED) {
				iconLayout.alignItems(JadeLinearLayout.Align.CENTER);
			} else if (iconMode == IWailaConfig.IconMode.TOP && icon.getHeight() > linearLayout.getHeight()) {
				iconLayout.alignItems(JadeLinearLayout.Align.CENTER);
			}
			iconLayout.addChild(icon);
			iconLayout.addChild(linearLayout);
			linearLayout = iconLayout;
		}

		layout = new LayoutWithPadding(
				linearLayout,
				style.padding(ScreenDirection.LEFT),
				style.padding(ScreenDirection.UP),
				style.padding(ScreenDirection.RIGHT),
				style.padding(ScreenDirection.DOWN));
		layout.arrangeElements();
		width = layout.getWidth();
		height = layout.getHeight();

		renderables.clear();
		JadeUI.visitChildrenRecursive(
				layout, element -> {
					if (element instanceof Renderable renderable) {
						renderables.add(renderable);
					}
				});
	}

	@Override
	public void setX(int x) {
		super.setX(x);
		layout.setX(x);
	}

	@Override
	public void setY(int y) {
		super.setY(y);
		layout.setY(y);
	}

	private static void chase(TooltipAnimation animation, ToFloatFunction<Rect2f> getter, FloatConsumer setter, float progress) {
		if (IWailaConfig.get().overlay().getAnimation()) {
			float source = getter.applyAsFloat(animation.rect);
			float target = getter.applyAsFloat(animation.expectedRect);
			float diff = target - source;
			if (diff == 0) {
				return;
			}
			if (progress >= 1) {
				animation.startTime = -1;
				setter.accept(target);
				return;
			}
			float startValue = getter.applyAsFloat(animation.startRect);
			float deltaValue = target - startValue;
			setter.accept(startValue + progress * deltaValue);
		} else {
			setter.accept(getter.applyAsFloat(animation.expectedRect));
		}
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (tooltip.isEmpty()) {
			return;
		}

		// render background
		float alpha = IDisplayHelper.get().backgroundOpacity();
		boolean root = JadeIds.ROOT.equals(getTag());
		if (root) {
			alpha *= IWailaConfig.get().overlay().getAlpha();
		}
		if (alpha > 0) {
			style.render(this, getX(), getY(), getWidth(), getHeight(), alpha);
		}

		// 1.12.2: scissor clipping (graphics.enableScissor/disableScissor) dropped --
		// GuiGraphicsExtractor's local-to-screen transform is not available here
		for (Renderable renderable : renderables) {
			try {
				renderable.extractRenderState(mouseX, mouseY, partialTicks);
			} catch (Exception e) {
				WailaExceptionHandler.handleErr(e, null, null);
				IDisplayHelper.get().drawBorder(((LayoutElement) renderable).getRectangle(), 1, 0x88FF0000, true);
			}
		}

		// 1.12.2: no scissor disable needed

		if (root && tooltip.sneakyDetails) {
			// 1.12.2: api/theme/SneakyDetails is parked (Rule 0); the GuiGraphicsExtractor parameter
			// is dropped from this call site but the reference will not compile until B9 translates
			// that package. Left as an intentionally unresolved out-of-scope error.
			IThemeHelper.get().theme().sneakyDetails.render(partialTicks, this);
		}
	}

	@Override
	public void renderDebug(int mouseX, int mouseY, float partialTicks, RenderDebugContext context) {
		super.renderDebug(mouseX, mouseY, partialTicks, context);
		if (!context.renderChildren) {
			return;
		}
		JadeUI.visitChildrenRecursive(
				layout, layoutElement -> {
					if (layoutElement instanceof Element element) {
						element.renderDebug(mouseX, mouseY, partialTicks, context);
					} else if (layoutElement instanceof Layout) {
						JadeInternals.getDisplayHelper().drawBorder(layoutElement.getRectangle(), 1, 0x8800FF00, true);
					}
				});
	}

	//	@Override
	//	public void render(GuiGraphicsExtractor guiGraphics, final float x, final float y, final float maxX, final float maxY) {
	//		if (tooltip.isEmpty()) {
	//			return;
	//		}
	//		guiGraphics.pose().pushMatrix();
	//		guiGraphics.pose().translate(x, y);
	//
	//		// render background
	//		float alpha = IDisplayHelper.get().opacity();
	//		if (JadeIds.ROOT.equals(getTag())) {
	//			alpha *= IWailaConfig.get().overlay().getAlpha();
	//		}
	//		if (alpha > 0) {
	//			style.render(guiGraphics, this, 0, 0, maxX - x, maxY - y, alpha);
	//		}
	//
	//		int borderWidth = style.borderWidth();
	//		// render box progress
	//		if (boxProgressType != null) {
	//			float left = style.boxProgressOffset(ScreenDirection.LEFT) + borderWidth;
	//			float width = maxX - x - left;
	//			float top = maxY - y - 1 + style.boxProgressOffset(ScreenDirection.UP) + borderWidth;
	//			float height = 1 + style.boxProgressOffset(ScreenDirection.DOWN);
	//			float progress = boxProgress;
	//			if (track == null && tag != null) {
	//				track = WailaTickHandler.instance().progressTracker.getOrCreate(
	//						tag, ProgressTrackInfo.class, () -> {
	//							return new ProgressTrackInfo(false, boxProgress, 0);
	//						});
	//			}
	//			if (track != null) {
	//				track.setProgress(progress);
	//				track.update(Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks());
	//				progress = track.getSmoothProgress();
	//			}
	//			((DisplayHelper) IDisplayHelper.get()).drawGradientProgress(
	//					guiGraphics,
	//					left,
	//					top,
	//					width,
	//					height,
	//					progress,
	//					style.boxProgressColors.get(boxProgressType));
	//		}
	//
	//		float contentLeft = padding(ScreenDirection.LEFT) + borderWidth;
	//		float contentTop = padding(ScreenDirection.UP) + borderWidth;
	//
	//		// render icon
	//		if (icon != null) {
	//			Vec2 iconSize = icon.getCachedSize();
	//			Vec2 offset = icon.getTranslation();
	//			float offsetY = offset.y;
	//			float min = contentTop + padding(ScreenDirection.DOWN) + iconSize.y;
	//			IWailaConfig.IconMode iconMode = IWailaConfig.get().overlay().getIconMode();
	//			if (iconMode == IWailaConfig.IconMode.TOP && min < getCachedSize().y) {
	//				offsetY += contentTop;
	//			} else {
	//				offsetY += (size.y - iconSize.y) / 2;
	//			}
	//			float offsetX = contentLeft + offset.x;
	//			icon.render(guiGraphics, offsetX, offsetY, offsetX + iconSize.x, offsetY + iconSize.y);
	//			contentLeft += iconSize.x + 3;
	//		}
	//
	//		// render elements
	//		{
	//			boolean fancy = Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;
	//			if (fancy) {
	//				guiGraphics.enableScissor(0, 0, (int) (maxX - x), (int) (maxY - y));
	//			}
	//			float lineTop = contentTop;
	//			int lineCount = tooltip.lines.size();
	//			Tooltip.Line line = tooltip.lines.getFirst();
	//			for (int i = 0; i < lineCount; i++) {
	//				Vec2 lineSize = line.size();
	//				line.render(guiGraphics, contentLeft, lineTop, maxX - x - padding(ScreenDirection.RIGHT), lineTop + lineSize.y);
	//				if (i < lineCount - 1) {
	//					int marginBottom = line.marginBottom;
	//					line = tooltip.lines.get(i + 1);
	//					lineTop += lineSize.y + calculateMargin(marginBottom, line.marginTop);
	//				}
	//			}
	//			if (fancy) {
	//				guiGraphics.disableScissor();
	//			}
	//		}
	//
	//		// render down arrow
	//		if (tooltip.sneakyDetails) {
	//			float arrowTop = (OverlayRenderer.ticks / 5) % 8 - 2;
	//			if (arrowTop <= 4) {
	//				alpha = 1 - Math.abs(arrowTop) / 2;
	//				if (alpha > 0.016) {
	//					guiGraphics.pose().pushMatrix();
	//					arrowTop += size.y - 6;
	//					float arrowLeft = contentLeft + (contentSize.x - DisplayHelper.font().width("▾") + 1) / 2f;
	//					guiGraphics.pose().translate(arrowLeft, arrowTop);
	//					int color = Overlay.applyAlpha(IThemeHelper.get().theme().text.colors().info(), alpha);
	//					DisplayHelper.INSTANCE.drawText(guiGraphics, "▾", 0, 0, color);
	//					guiGraphics.pose().popMatrix();
	//				}
	//			}
	//		}
	//
	//		guiGraphics.pose().popMatrix();
	//	}

	@Override
	public Tooltip getTooltip() {
		return tooltip;
	}

	@Override
	public void setBoxProgress(MessageType type, float progress) {
		boxProgress = progress;
		boxProgressType = type;
	}

	@Override
	public float getBoxProgress() {
		return boxProgressType == null ? Float.NaN : boxProgress;
	}

	@Override
	public void clearBoxProgress() {
		boxProgress = 0;
		boxProgressType = null;
	}

	@Override
	public @Nullable Element getIcon() {
		return icon;
	}

	@Override
	public void setIcon(@Nullable Element icon) {
		this.icon = icon;
	}

	public void updateExpectedRect(TooltipAnimation animation) {
		ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft());
		IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
		IWailaConfig.Accessibility accessibility = IWailaConfig.get().accessibility();
		float x = resolution.getScaledWidth() * accessibility.tryFlip(overlay.getOverlayPosX());
		float y = resolution.getScaledHeight() * (1.0F - overlay.getOverlayPosY());
		float width = layout.getWidth();
		float height = layout.getHeight();

		animation.scale = overlay.getOverlayScale();
		float thresholdHeight = resolution.getScaledHeight() * overlay.getAutoScaleThreshold();
		if (!JadeUI.isPinned() && layout.getHeight() * animation.scale > thresholdHeight) {
			animation.scale = Math.max(animation.scale * 0.5f, thresholdHeight / layout.getHeight());
		}

		Rect2f expectedRect = animation.expectedRect;
		expectedRect.setWidth((int) (width * animation.scale));
		expectedRect.setHeight((int) (height * animation.scale));
		expectedRect.setX((int) (x - expectedRect.getWidth() * accessibility.tryFlip(overlay.getAnchorX())));
		expectedRect.setY((int) (y - expectedRect.getHeight() * overlay.getAnchorY()));

		if (PreviewOptionsScreen.isAdjustingPosition()) {
			return;
		}

		IWailaConfig.BossBarOverlapMode mode = IWailaConfig.get().general().getBossBarOverlapMode();
		if (mode == IWailaConfig.BossBarOverlapMode.PUSH_DOWN) {
			Rect2f bossBarRect = ClientProxy.getBossBarRect();
			// check if tooltip intersects with boss bar
			if (bossBarRect != null && bossBarRect.intersects(expectedRect)) {
				expectedRect.setY(bossBarRect.getY() + bossBarRect.getHeight());
			}
		}

		// 1.12.2: keep the tooltip frame on screen. BoxStyle.render expands the
		// background/frame by 9px on each side, so a box whose top sits at y=0 (the
		// default anchorY=0 / overlayPosY=1.0 position) has its rounded top edge
		// clipped off the top of the screen. Nudge it down to the frame inset. This is
		// a documented divergence from the modern source tree (which does not clamp);
		// position-adjustment mode above is unaffected.
		if (expectedRect.getY() < 9) {
			expectedRect.setY(9);
		}
	}

	public void updateRect(TooltipAnimation animation) {
		Rect2f src = animation.rect;
		Rect2f target = animation.expectedRect;
		if (src.getWidth() == 0) {
			src.setX(target.getX());
			src.setY(target.getY());
			src.setWidth(target.getWidth());
			src.setHeight(target.getHeight());
			animation.alpha = animation.showHideAlpha;
		} else {
			Duration duration = Duration.ofMillis(75);
			long deltaTime = System.currentTimeMillis() - animation.startTime;
			long durationMillis = duration.toMillis();
			float progress = (float) deltaTime / durationMillis;
			animation.alpha = Math.min(animation.showHideAlpha, Math.max(progress, 0.55F));
			chase(animation, Rect2f::getX, src::setX, progress);
			chase(animation, Rect2f::getY, src::setY, progress);
			chase(
					animation, Rect2f::getWidth, it -> {
						src.setWidth(it);
						width = (int) (it / animation.scale);
					}, progress);
			chase(
					animation, Rect2f::getHeight, it -> {
						src.setHeight(it);
						height = (int) (it / animation.scale);
					}, progress);
		}
	}

	@Override
	public BoxStyle getStyle() {
		return style;
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		if (tooltip.isEmpty()) {
			return null;
		}
		String narration = tooltip.getNarration();
		if (narration.isEmpty()) {
			return null;
		}
		return new TextComponentString(narration);
	}

	@Override
	public void setFreeSpace(int width, int height) {
		layout.setFreeSpace(width, height);
		this.width = layout.getWidth();
		this.height = layout.getHeight();
	}

	/**
	 * 1.12.2: no {@code AbstractWidget} hierarchy exists to visit (Jade's own {@link LayoutElement}
	 * does not have {@code visitWidgets}). This method is dropped.
	 */

	/**
	 * 1.12.2: widget alpha is handled globally via {@link IDisplayHelper#opacity()}, which already
	 * returns {@code OverlayRenderer.animation.alpha}. No local widget-alpha propagation is needed.
	 * Kept as a no-op stub for the {@code OverlayRenderer.renderOverlay} call site.
	 */
	public void setWidgetAlpha(float alpha) {
	}

	/**
	 * Finds the innermost renderable {@link LayoutElement} at the given local coordinates.
	 * <p>
	 * 1.12.2: replaces the {@code ContainerEventHandler.getChildAt(double, double)} contract
	 * from the modern code. Walks the {@link #renderables} list in reverse (topmost first) and
	 * returns the first hit. Called from {@link snownee.jade.overlay.OverlayRenderer} and
	 * {@link snownee.jade.gui.PinScreen} for debug-overlay and copy-to-clipboard interactions.
	 *
	 * @param mouseX mapped mouse X (local tooltip coordinates)
	 * @param mouseY mapped mouse Y (local tooltip coordinates)
	 * @return an {@link Optional} containing the topmost {@link LayoutElement} at the given
	 *         coordinates, or empty if no child contains the point
	 */
	public Optional<LayoutElement> getChildAt(double mouseX, double mouseY) {
		for (int i = renderables.size() - 1; i >= 0; i--) {
			Renderable renderable = renderables.get(i);
			if (renderable instanceof LayoutElement layoutElement) {
				if (mouseX >= layoutElement.getX() && mouseX < layoutElement.getX() + layoutElement.getWidth()
						&& mouseY >= layoutElement.getY() && mouseY < layoutElement.getY() + layoutElement.getHeight()) {
					return Optional.of(layoutElement);
				}
			}
		}
		return Optional.empty();
	}
}
