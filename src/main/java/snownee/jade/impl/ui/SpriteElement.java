package snownee.jade.impl.ui;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.Orientation;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.overlay.DisplayHelper;

public class SpriteElement extends ProgressOverlayElement {

	private final ResourceLocation sprite;
	public @Nullable Orientation tiledOrientation;
	private final int oWidth;
	private final int oHeight;
	private int color = -1;
	private int generation;
	private @Nullable ResourceLocation mappedSprite;

	public SpriteElement(ResourceLocation sprite, int width, int height) {
		this.sprite = sprite;
		oWidth = this.width = width;
		oHeight = this.height = height;
	}

	@Override
	public @Nullable ITextComponent getNarration() {
		return null;
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		if (tiledOrientation != null) {
			Rect2f rect;
			if (floatingRect == null) {
				rect = Rect2f.of(this);
			} else {
				rect = floatingRect.copy();
			}
			float axisLength = tiledOrientation.getAxisLength(rect);
			float axisPosition = tiledOrientation.getAxisPosition(rect);
			float crossAxisLength = tiledOrientation.getCrossAxisLength(rect);
			float crossAxisPosition = tiledOrientation.getCrossAxisPosition(rect);
			float tileAxisStart = 0F;
			float tileAxisStep = tiledOrientation == Orientation.HORIZONTAL ? oWidth : oHeight;
			while (tileAxisStart < axisLength) {
				float tileAxisEnd = Math.min(tileAxisStart + tileAxisStep, axisLength);
				float tileSize = tileAxisEnd - tileAxisStart;
				tiledOrientation.setPosition(rect, axisPosition + tileAxisStart, crossAxisPosition);
				tiledOrientation.setSize(rect, tileSize, crossAxisLength);
				DisplayHelper.INSTANCE.blitSprite(
						mappedSprite(),
						oWidth,
						oHeight,
						0,
						0,
						(int) rect.getX(),
						(int) rect.getY(),
						(int) rect.getWidth(),
						(int) rect.getHeight(),
						color);
				tileAxisStart += tileAxisStep;
			}
			return;
		}
		if (floatingRect == null) {
			IDisplayHelper.get().blitSprite(
					mappedSprite(),
					oWidth,
					oHeight,
					0,
					0,
					getX(),
					getY(),
					width,
					height,
					color);
		} else {
			DisplayHelper.INSTANCE.blitSprite(
					mappedSprite(),
					oWidth,
					oHeight,
					0,
					0,
					(int) floatingRect.getX(),
					(int) floatingRect.getY(),
					(int) floatingRect.getWidth(),
					(int) floatingRect.getHeight(),
					color);
		}
		if (IWailaConfig.get().general().isDebug() && floatingRect != null) {
			DisplayHelper.INSTANCE.drawBorder(
					new Rect2f(
							floatingRect.getX(),
							floatingRect.getY(),
							floatingRect.getWidth(),
							floatingRect.getHeight()),
					1, 0xFF00AAAA, true);
		}
	}

	private ResourceLocation mappedSprite() {
		if (mappedSprite == null || generation != IThemeHelper.get().generation()) {
			generation = IThemeHelper.get().generation();
			mappedSprite = IThemeHelper.get().theme().mapSprite(sprite);
		}
		return mappedSprite;
	}

	public void setColor(int color) {
		this.color = color;
	}
}
