package snownee.jade.api.ui;

import java.text.Format;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import snownee.jade.JadeInternals;

/**
 * Abstraction over platform-specific rendering helpers used by Jade UI elements.
 * <p>
 * 1.12.2: the {@code GuiGraphicsExtractor} parameter is dropped from all methods
 * per the backport convention. Callers obtain font / render helpers directly.
 */
public interface IDisplayHelper {

	/**
	 * Returns the active helper instance.
	 *
	 * @return display helper
	 */
	static IDisplayHelper get() {
		return JadeInternals.getDisplayHelper();
	}

	/**
	 * Draws an item stack.
	 *
	 * @param x     x position
	 * @param y     y position
	 * @param stack item stack
	 * @param scale render scale
	 * @param text  optional overlay text
	 */
	void drawItem(float x, float y, ItemStack stack, float scale, @Nullable String text);

	void drawBorder(Rect2f rectangle, int width, int color, boolean corner);

	String humanReadableNumber(double number, String unit, boolean milli);

	String humanReadableNumber(double number, String unit, boolean milli, @Nullable Format formatter);

	void drawText(String text, float x, float y, int color);

	void drawText(ITextComponent text, float x, float y, int color);

	ITextComponent stripColor(ITextComponent component);

	void blitSprite(ResourceLocation ResourceLocation, int i, int j, int k, int l);

	void blitSprite(
			ResourceLocation ResourceLocation,
			int i,
			int j,
			int k,
			int l,
			int m);

	void blitSprite(
			ResourceLocation ResourceLocation,
			int spriteWidth,
			int spriteHeight,
			int uStart,
			int vStart,
			int x,
			int y,
			int width,
			int height);

	void blitSprite(
			ResourceLocation ResourceLocation,
			int spriteWidth,
			int spriteHeight,
			int uStart,
			int vStart,
			int x,
			int y,
			int width,
			int height,
			int color);

	float opacity();

	float backgroundOpacity();
}
