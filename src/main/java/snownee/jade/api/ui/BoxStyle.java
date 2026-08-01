package snownee.jade.api.ui;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.JadeIds;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.impl.ui.StyledElement;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.util.JadeCodecs;

/**
 * Rendering style for tooltip boxes and framed UI elements.
 * <p>
 * 1.12.2: the {@code GuiGraphicsExtractor} parameter is dropped from {@link #render(StyledElement, float, float, float, float, float)}.
 * Where the font renderer is needed callers obtain it from {@code Minecraft.getMinecraft().fontRenderer}.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BoxStyle {
	private static final int[] DEFAULT_PADDING = new int[]{3, 3, 3, 3};
	private static final Codec<ResourceLocation> RESOURCE_LOCATION_CODEC = Codec.STRING.xmap(ResourceLocation::new, ResourceLocation::toString);
	public static final Codec<BoxStyle> CODEC = RecordCodecBuilder.create(i -> i.group(
					JadeCodecs.floatArrayCodec(4, Codec.FLOAT)
							.optionalFieldOf("boxProgressOffset")
							.forGetter($ -> Optional.ofNullable($.boxProgressOffset)),
					ColorPalette.CODEC.optionalFieldOf("boxProgressColors", ColorPalette.DEFAULT).forGetter($ -> $.boxProgressColors),
					JadeCodecs.intArrayCodec(4, Codec.INT).optionalFieldOf("padding").forGetter($ -> Optional.ofNullable($.padding)),
					Codec.INT.optionalFieldOf("borderWidth", 1).forGetter($ -> $.borderWidth),
					RESOURCE_LOCATION_CODEC.optionalFieldOf("sprite").forGetter($ -> Optional.ofNullable($.sprite)),
					RESOURCE_LOCATION_CODEC.optionalFieldOf("withIconSprite").forGetter($ -> Optional.ofNullable($.withIconSprite)),
					Codec.BOOL.optionalFieldOf("tooltip", false).forGetter($ -> $.tooltip))
			.apply(i, BoxStyle::new));
	private static final BoxStyle TRANSPARENT = sprite(null, null, 0);
	public static final BoxStyle DEFAULT_NESTED_BOX = sprite(JadeIds.JADE("nested_box"), null);
	public static final BoxStyle DEFAULT_VIEW_GROUP = sprite(JadeIds.JADE("view_group"), new int[]{2, 2, 2, 2}, 0);
	public final float @Nullable [] boxProgressOffset;
	public final int @Nullable [] padding;
	public int borderWidth;
	public ColorPalette boxProgressColors;
	@Nullable
	public ResourceLocation sprite;
	@Nullable
	public ResourceLocation withIconSprite;
	public boolean tooltip;

	public BoxStyle(
			Optional<float[]> boxProgressOffset,
			ColorPalette boxProgressColors,
			Optional<int[]> padding,
			int borderWidth,
			Optional<ResourceLocation> sprite,
			Optional<ResourceLocation> withIconSprite,
			boolean tooltip) {
		this.boxProgressOffset = boxProgressOffset.orElse(null);
		this.boxProgressColors = boxProgressColors;
		this.padding = padding.orElseGet(DEFAULT_PADDING::clone);
		this.borderWidth = borderWidth;
		this.sprite = sprite.orElse(null);
		this.withIconSprite = withIconSprite.orElse(null);
		this.tooltip = tooltip;
	}

	public static BoxStyle nestedBox() {
		return IThemeHelper.get().theme().nestedBoxStyle;
	}

	public static BoxStyle viewGroup() {
		return IThemeHelper.get().theme().viewGroupStyle;
	}

	public static BoxStyle transparent() {
		return BoxStyle.TRANSPARENT;
	}

	public static BoxStyle simple(@Nullable ResourceLocation sprite, int @Nullable [] padding) {
		return new BoxStyle(
				Optional.empty(),
				ColorPalette.DEFAULT,
				Optional.ofNullable(padding),
				1,
				Optional.ofNullable(sprite),
				Optional.empty(),
				false);
	}

	public static BoxStyle tooltip(@Nullable ResourceLocation sprite, int @Nullable [] padding) {
		return tooltip(sprite, padding, 1);
	}

	public static BoxStyle tooltip(@Nullable ResourceLocation sprite, int @Nullable [] padding, int borderWidth) {
		return new BoxStyle(
				Optional.empty(),
				ColorPalette.DEFAULT,
				Optional.ofNullable(padding),
				borderWidth,
				Optional.ofNullable(sprite),
				Optional.empty(),
				true);
	}

	public static BoxStyle sprite(@Nullable ResourceLocation sprite, int @Nullable [] padding) {
		return sprite(sprite, padding, 1);
	}

	public static BoxStyle sprite(@Nullable ResourceLocation sprite, int @Nullable [] padding, int borderWidth) {
		return new BoxStyle(
				Optional.empty(),
				ColorPalette.DEFAULT,
				Optional.ofNullable(padding),
				borderWidth,
				Optional.ofNullable(sprite),
				Optional.empty(),
				false);
	}

	public float boxProgressOffset(ScreenDirection dir) {
		return boxProgressOffset == null ? 0 : boxProgressOffset[dir.ordinal()];
	}

	public int padding(ScreenDirection dir) {
		return MoreObjects.firstNonNull(padding, DEFAULT_PADDING)[dir.ordinal()];
	}

	/**
	 * 1.12.2: {@code GuiGraphicsExtractor} parameter dropped. Texture is bound and blitted
	 * via {@link snownee.jade.overlay.DisplayHelper} utilities.
	 * <p>
	 * 1.12.2: modern's {@code TooltipRenderUtil.getBackgroundSprite(texture)} /
	 * {@code getFrameSprite(texture)} do not exist; the equivalent is done
	 * inline: a tooltip style whose sprite is {@code jade:<name>} derives
	 * {@code jade:tooltip/<name>_background} and {@code jade:tooltip/<name>_frame}
	 * (e.g. {@code jade:dark} -> {@code tooltip/dark_background.png} +
	 * {@code tooltip/dark_frame.png}). {@code jade:top} has no
	 * {@code tooltip/top_background}, so it falls back to the plain sprite. The
	 * rounded rect is expanded by 9px each side (the modern tooltip border
	 * inset), which is what insets the content from the background's rounded
	 * frame. Background and frame are drawn with 9-slice scaling (border 10 for
	 * the 100x100 backgrounds) so corners are not distorted.
	 */
	public void render(StyledElement element, float x, float y, float w, float h, float alpha) {
		ResourceLocation texture = sprite;
		if (withIconSprite != null && element.getIcon() != null) {
			texture = withIconSprite;
		}
		if (texture == null) {
			return;
		}
		int roundedX = Math.round(x);
		int roundedY = Math.round(y);
		int roundedW = Math.round(w);
		int roundedH = Math.round(h);
		int col = alpha == 1 ? 0xFFFFFFFF : ((int) (alpha * 255) << 24) | 0xFFFFFF;
		if (tooltip) {
			roundedX = roundedX - 9;
			roundedY = roundedY - 9;
			roundedW = roundedW + 9 + 9;
			roundedH = roundedH + 9 + 9;
			// 1.12.2: no TooltipRenderUtil/background+frame sprite split; derived inline.
			ResourceLocation background = tooltipBackground(texture);
			ResourceLocation frame = tooltipFrame(texture);
			if (background != null && frame != null) {
				DisplayHelper.INSTANCE.blitNineSlice(background, roundedX, roundedY, roundedW, roundedH, 10, col);
				DisplayHelper.INSTANCE.blitNineSlice(frame, roundedX, roundedY, roundedW, roundedH, 10, col);
			} else {
				DisplayHelper.INSTANCE.blitSprite(texture, roundedX, roundedY, roundedW, roundedH, col);
			}
		} else if (borderWidth > 0) {
			// 1.12.2: the sprite's .mcmeta gui.scaling nine_slice border matches
			// BoxStyle.borderWidth for every shipped sprite (nested_box=1, *_slim=1,
			// top=2). Plain-stretching an 82x82 framed sprite to a small box (e.g. a
			// ~100x8 fluid capacity bar) squashes the 1px border to sub-pixel on the
			// short axis; nine-slice keeps all four borders at native thickness.
			DisplayHelper.INSTANCE.blitNineSlice(texture, roundedX, roundedY, roundedW, roundedH, borderWidth, col);
		} else {
			// view_group (1x1) and other borderless sprites stay plain-stretched.
			DisplayHelper.INSTANCE.blitSprite(texture, roundedX, roundedY, roundedW, roundedH, col);
		}
	}

	/**
	 * 1.12.2: replaces {@code TooltipRenderUtil.getBackgroundSprite(texture)}.
	 * For a tooltip sprite {@code jade:<name>}, resolves
	 * {@code jade:tooltip/<name>_background} when that sprite exists, else
	 * {@code null} to signal falling back to the plain sprite (e.g. {@code jade:top}).
	 */
	@Nullable
	private static ResourceLocation tooltipBackground(ResourceLocation texture) {
		ResourceLocation background = new ResourceLocation(texture.getNamespace(), "tooltip/" + texture.getPath() + "_background");
		return DisplayHelper.hasSprite(background) ? background : null;
	}

	/**
	 * 1.12.2: replaces {@code TooltipRenderUtil.getFrameSprite(texture)}.
	 * {@code jade:tooltip/<name>_frame}; {@code null} when the sprite is absent.
	 */
	@Nullable
	private static ResourceLocation tooltipFrame(ResourceLocation texture) {
		ResourceLocation frame = new ResourceLocation(texture.getNamespace(), "tooltip/" + texture.getPath() + "_frame");
		return DisplayHelper.hasSprite(frame) ? frame : null;
	}

	public int borderWidth() {
		return borderWidth;
	}

	public BoxStyle copy() {
		return new BoxStyle(
				JadeCodecs.nullableClone(boxProgressOffset),
				boxProgressColors,
				JadeCodecs.nullableClone(padding),
				borderWidth,
				Optional.ofNullable(sprite),
				Optional.ofNullable(withIconSprite),
				tooltip);
	}

//	public static class GradientBorder extends BoxStyle {
//		public static final GradientBorder TRANSPARENT = new GradientBorder(
//				Optional.empty(),
//				ColorPalette.DEFAULT,
//				Optional.empty(),
//				-1,
//				new int[]{-1, -1, -1, -1},
//				0,
//				Optional.of(false));
//		public static final GradientBorder DEFAULT_NESTED_BOX = new GradientBorder(
//				Optional.empty(),
//				ColorPalette.DEFAULT,
//				Optional.empty(),
//				-1,
//				new int[]{0xFF808080, 0xFF808080, 0xFF808080, 0xFF808080},
//				1,
//				Optional.empty());
//		public static final GradientBorder DEFAULT_VIEW_GROUP = new GradientBorder(
//				Optional.empty(),
//				ColorPalette.DEFAULT,
//				Optional.of(new int[]{2, 2, 2, 2}),
//				0x44444444,
//				new int[]{0x44444444, 0x44444444, 0x44444444, 0x44444444},
//				0.75F,
//				Optional.empty());
//		public int bgColor;
//		public int[] borderColor;
//		public float borderWidth;
//		@Nullable
//		public Boolean roundCorner;
//
//		private GradientBorder(
//				Optional<float[]> boxProgressOffset,
//				ColorPalette boxProgressColors,
//				Optional<int[]> padding,
//				int bgColor,
//				int[] borderColor,
//				float borderWidth,
//				Optional<Boolean> roundCorner) {
//			super(boxProgressOffset, boxProgressColors, padding);
//			this.bgColor = bgColor;
//			this.borderColor = borderColor;
//			this.borderWidth = borderWidth;
//			this.roundCorner = roundCorner.orElse(null);
//		}
//
//		@Override
//		public float borderWidth() {
//			return borderWidth;
//		}
//
//		@Override
//		public void render(GuiGraphicsExtractor guiGraphics, StyledElement element, float x, float y, float w, float h, float alpha) {
//			boolean roundCorner = hasRoundCorner();
//			if (bgColor != -1) {
//				int bg = IWailaConfig.Overlay.applyAlpha(bgColor, alpha);
//				DisplayHelper.INSTANCE.drawGradientRect(
//						guiGraphics,
//						x + borderWidth,
//						y + borderWidth,
//						w - borderWidth - borderWidth,
//						h - borderWidth - borderWidth,
//						bg,
//						bg);//center
//				if (roundCorner) {
//					DisplayHelper.INSTANCE.drawGradientRect(guiGraphics, x, y - 1, w, 1, bg, bg);
//					DisplayHelper.INSTANCE.drawGradientRect(guiGraphics, x, y + h, w, 1, bg, bg);
//					DisplayHelper.INSTANCE.drawGradientRect(guiGraphics, x - 1, y, 1, h, bg, bg);
//					DisplayHelper.INSTANCE.drawGradientRect(guiGraphics, x + w, y, 1, h, bg, bg);
//				}
//			}
//			if (borderWidth > 0) {
//				int[] borderColors = new int[4];
//				for (int i = 0; i < 4; i++) {
//					if (borderColor[i] != -1) {
//						borderColors[i] = IWailaConfig.Overlay.applyAlpha(borderColor[i], alpha);
//					}
//				}
//				DisplayHelper.INSTANCE.drawGradientRect(
//						guiGraphics,
//						x,
//						y + borderWidth,
//						borderWidth,
//						h - borderWidth - borderWidth,
//						borderColors[0],
//						borderColors[3]);
//				DisplayHelper.INSTANCE.drawGradientRect(
//						guiGraphics,
//						x + w - borderWidth,
//						y + borderWidth,
//						borderWidth,
//						h - borderWidth - borderWidth,
//						borderColors[1],
//						borderColors[2]);
//				DisplayHelper.INSTANCE.drawGradientRect(guiGraphics, x, y, w, borderWidth, borderColors[0], borderColors[1]);
//				DisplayHelper.INSTANCE.drawGradientRect(
//						guiGraphics,
//						x,
//						y + h - borderWidth,
//						w,
//						borderWidth,
//						borderColors[3],
//						borderColors[2]);
//			}
//		}
//	}
}
