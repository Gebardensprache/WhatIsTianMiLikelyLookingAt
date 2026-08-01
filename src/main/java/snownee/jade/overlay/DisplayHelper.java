package snownee.jade.overlay;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.mutable.MutableFloat;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.item.ItemStack;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.config.IWailaConfig.Overlay;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.JadeFont;

/**
 * 1.12.2: {@code GuiGraphicsExtractor} and {@code RenderPipeline} parameters dropped.
 * All rendering uses direct GL state / built-in 1.12.2 draw helpers.
 */
public class DisplayHelper implements IDisplayHelper {

	public static final DisplayHelper INSTANCE = new DisplayHelper();
	// https://github.com/mezz/JustEnoughItems/blob/1.16/src/main/java/mezz/jei/plugins/vanilla/ingredients/fluid/FluidStackRenderer.java
	private static final int MIN_FLUID_HEIGHT = 1; // ensure tiny amounts of fluid are still visible
	private static final Pattern STRIP_COLOR = Pattern.compile("(?i)\\u00A7[0-9A-F]");
	public static final DecimalFormat dfCommas = new DecimalFormat("0.##");
	public static final DecimalFormat[] dfCommasArray = new DecimalFormat[]{dfCommas, new DecimalFormat("0.#"), new DecimalFormat("0")};
	private static final Supplier<JadeFont> FONT = Suppliers.memoize(() -> new JadeFont(Minecraft.getMinecraft().fontRenderer));

	// ---- sprite resource resolution ---- //
	/**
	 * 1.12.2: modern Jade stores sprites under {@code textures/gui/sprites/} and
	 * references them by bare id (e.g. {@code jade:nested_box}). 1.12.2's
	 * {@code TextureManager.bindTexture} loads {@code assets/<ns>/<path>} exactly,
	 * so the file path is reconstructed here, mirroring how modern's GUI atlas
	 * (AtlasIds.GUI / gui_sprites) maps {@code jade:nested_box} to
	 * {@code textures/gui/sprites/nested_box.png}. Texture resources whose path
	 * already carries a known image/atlas prefix (e.g. {@code minecraft:hud/*})
	 * are passed through untouched.
	 */
	private static final Map<String, int[]> SPRITE_SIZE_BY_PATH = new HashMap<>();

	private static ResourceLocation resolveSprite(ResourceLocation texture) {
		texture = IThemeHelper.get().theme().mapSprite(texture);
		if (texture.getPath().startsWith("textures/") || texture.getPath().startsWith("font/") || texture.getPath().startsWith("hud/")) {
			return texture;
		}
		// 1.12.2 texture paths carry the .png suffix (TextureManager -> SimpleTexture
		// resolves assets/<ns>/<path> verbatim, e.g. "textures/gui/icons.png").
		return new ResourceLocation(texture.getNamespace(), "textures/gui/sprites/" + texture.getPath() + ".png");
	}

	/**
	 * Whether a themed sprite exists. 1.12.2 has no {@code hasResource} on
	 * {@code IResourceManager}; {@code getAllResources} throws
	 * {@code FileNotFoundException} when nothing is present.
	 */
	public static boolean hasSprite(ResourceLocation texture) {
		texture = resolveSprite(texture);
		try {
			return !Minecraft.getMinecraft().getResourceManager().getAllResources(texture).isEmpty();
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 1.12.2: there is no runtime way to query a texture's native size
	 * ({@code ITextureObject} exposes only the GL id), so the 9-slice border
	 * source dimensions come from the shipped sprite sheet sizes (matching the
	 * {@code gui/scaling} {@code .mcmeta} border values). Paths end in
	 * {@code .png}; a path like {@code tooltip/dark_background.png} matches the
	 * {@code _background} suffix.
	 */
	private static int[] spriteSize(ResourceLocation texture) {
		int[] cached = SPRITE_SIZE_BY_PATH.get(texture.toString());
		if (cached != null) {
			return cached;
		}
		int[] size = new int[]{256, 256};
		String path = texture.getPath();
		if (path.endsWith("_background.png")) {
			size[0] = 100;
			size[1] = 100;
		} else if (path.contains("nested_box") || path.endsWith("_slim.png") || path.endsWith("top.png")) {
			size[0] = 82;
			size[1] = 82;
		} else if (path.endsWith("view_group.png")) {
			size[0] = 1;
			size[1] = 1;
		}
		SPRITE_SIZE_BY_PATH.put(texture.toString(), size);
		return size;
	}

	/**
	 * Draws a 9-slice (nine-patch) quad so the four corners keep their native
	 * size while the edges and centre stretch. {@code border} is the border
	 * width in texture pixels; the source sprite's native size is taken from
	 * {@link #spriteSize}. 1.12.2: replaces modern
	 * {@code GuiSpriteScaling.NineSlice} handling.
	 */
	public void blitNineSlice(ResourceLocation texture, int x, int y, int w, int h, int border, int color) {
		texture = resolveSprite(texture);
		color = Overlay.applyAlpha(color, opacity());
		if (w <= 0 || h <= 0) {
			return;
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		setGlColor(color);
		int[] size = spriteSize(texture);
		int texW = size[0];
		int texH = size[1];
		float invW = 1.0F / texW;
		float invH = 1.0F / texH;
		float left = border * invW;
		float right = 1.0F - border * invW;
		float top = border * invH;
		float bottom = 1.0F - border * invH;
		int b = Math.min(border, Math.min(w / 2, h / 2));
		int innerW = w - b - b;
		int innerH = h - b - b;
		int bx1 = x + b;
		int by1 = y + b;
		int bx2 = x + w - b;
		int by2 = y + h - b;
		// corners (native size)
		drawTexturedQuad(x, y, b, b, 0, 0, left, top);
		drawTexturedQuad(bx2, y, b, b, right, 0, 1, top);
		drawTexturedQuad(x, by2, b, b, 0, bottom, left, 1);
		drawTexturedQuad(bx2, by2, b, b, right, bottom, 1, 1);
		// edges (stretched)
		drawTexturedQuad(bx1, y, innerW, b, left, 0, right, top);
		drawTexturedQuad(bx1, by2, innerW, b, left, bottom, right, 1);
		drawTexturedQuad(x, by1, b, innerH, 0, top, left, bottom);
		drawTexturedQuad(bx2, by1, b, innerH, right, top, 1, bottom);
		// centre (stretched)
		drawTexturedQuad(bx1, by1, innerW, innerH, left, top, right, bottom);
		GlStateManager.disableBlend();
	}

	static {
		for (DecimalFormat format : dfCommasArray) {
			format.setRoundingMode(RoundingMode.DOWN);
		}
	}

	// ---- simple fill helper ---- //

	public static void fill(float minX, float minY, float maxX, float maxY, int color) {
		if (minX >= maxX || minY >= maxY) {
			return;
		}
		Gui.drawRect(Math.round(minX), Math.round(minY), Math.round(maxX), Math.round(maxY), color);
	}

	// ======================== IDisplayHelper ======================== //

	@Override
	public void drawItem(float x, float y, ItemStack stack, float scale, @Nullable String text) {
		if (opacity() < 0.5F) {
			return;
		}
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, 0.0F);
		GlStateManager.scale(scale, scale, 1.0F);
		// 1.12.2: modern's GuiGraphics.fakeItem performs the full item-render setup,
		// including standard GUI lighting. renderItemIntoGUI only enables lighting for
		// 3D (block) models — it never configures the lights themselves, which vanilla
		// GUI callers do via RenderHelper.enableGUIStandardItemLighting() (see
		// GuiContainer.drawScreen). Without this, block icons render with no lights and
		// come out darker than flat item sprites. The full-brightness lightmap coords
		// (240/240) mirror what vanilla sets for item slots.
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
		RenderHelper.enableGUIStandardItemLighting();
		// 1.12.2: the overlay pass runs with the depth test off (OverlayRenderer), but 3D
		// GUI item icons -- ender chests, beds, shulker boxes, banners, etc., rendered
		// through TileEntityItemStackRenderer into the same TESR models -- need test+mask
		// on so their faces sort correctly instead of flickering in submission order. This
		// mirrors what vanilla GuiContainer and TheOneProbe do when drawing item stacks.
		boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
		boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
		GlStateManager.enableDepth();
		GlStateManager.depthMask(true);
		mc.getRenderItem().renderItemIntoGUI(stack, 0, 0);
		mc.getRenderItem().renderItemOverlayIntoGUI(mc.fontRenderer, stack, 0, 0, text);
		GlStateManager.depthMask(depthMask);
		if (!depthTest) {
			GlStateManager.disableDepth();
		}
		GlStateManager.popMatrix();
		RenderHelper.disableStandardItemLighting();
	}

	@Override
	public void drawBorder(Rect2f rectangle, int width, int color, boolean corner) {
		float minX = rectangle.getX();
		float minY = rectangle.getY();
		float maxX = rectangle.getRight();
		float maxY = rectangle.getBottom();
		fill(minX + width, minY, maxX - width, minY + width, color);
		fill(minX + width, maxY - width, maxX - width, maxY, color);
		if (corner) {
			fill(minX, minY, minX + width, maxY, color);
			fill(maxX - width, minY, maxX, maxY, color);
		} else {
			fill(minX, minY + width, minX + width, maxY - width, color);
			fill(maxX - width, minY + width, maxX, maxY - width, color);
		}
	}

	@Override
	public String humanReadableNumber(double number, String unit, boolean milli) {
		return humanReadableNumber(number, unit, milli, dfCommas);
	}

	// https://programming.guide/worlds-most-copied-so-snippet.html
	@Override
	public String humanReadableNumber(double number, String unit, boolean milli, @Nullable Format formatter) {
		if (number == 0 || Math.abs(number) < 1e-9) {
			return "0" + unit;
		}
		StringBuilder sb = new StringBuilder();
		boolean n = number < 0;
		if (n) {
			number = -number;
			sb.append('-');
		}
		if (milli && number >= 1000) {
			number /= 1000;
			milli = false;
		}
		int exp = formatter == null && number < 10000 ? 0 : (int) Math.log10(number) / 3;
		if (exp > 7) {
			exp = 7;
		}
		if (exp > 0) {
			number /= Math.pow(1000, exp);
		}
		if (formatter == null) {
			if (number < 10) {
				formatter = dfCommasArray[0];
			} else if (number < 100) {
				formatter = dfCommasArray[1];
			} else {
				formatter = dfCommasArray[2];
			}
		}
		if (formatter instanceof java.text.NumberFormat) {
			sb.append(((java.text.NumberFormat) formatter).format(number));
		} else {
			sb.append(formatter.format(new Object[]{number}));
		}
		if (exp == 0) {
			if (milli) {
				sb.append('m');
			}
		} else {
			char pre = "kMGTPEZ".charAt(exp - 1);
			sb.append(pre);
		}
		sb.append(unit);
		return sb.toString();
	}

	@Override
	public void drawText(String text, float x, float y, int color) {
		Minecraft mc = Minecraft.getMinecraft();
		boolean shadow = IThemeHelper.get().theme().text.shadow();
		color = Overlay.applyAlpha(color, opacity());
		if (shadow) {
			mc.fontRenderer.drawStringWithShadow(text, x, y, color);
		} else {
			mc.fontRenderer.drawString(text, (int) x, (int) y, color);
		}
	}

	@Override
	public void drawText(ITextComponent text, float x, float y, int color) {
		drawText(text.getFormattedText(), x, y, color);
	}

	@Override
	public ITextComponent stripColor(ITextComponent component) {
		return new TextComponentString(component.getUnformattedText());
	}

	// ---- blitSprite family ---- //

	private static void drawTexturedQuad(int x, int y, int w, int h, float u0, float v0, float u1, float v1) {
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		buffer.pos(x, y + h, 0.0D).tex(u0, v1).endVertex();
		buffer.pos(x + w, y + h, 0.0D).tex(u1, v1).endVertex();
		buffer.pos(x + w, y, 0.0D).tex(u1, v0).endVertex();
		buffer.pos(x, y, 0.0D).tex(u0, v0).endVertex();
		tessellator.draw();
	}

	@Override
	public void blitSprite(ResourceLocation texture, int i, int j, int k, int l) {
		blitSprite(texture, i, j, k, l, 0xFFFFFFFF);
	}

	@Override
	public void blitSprite(ResourceLocation texture, int x, int y, int w, int h, int color) {
		texture = resolveSprite(texture);
		color = Overlay.applyAlpha(color, opacity());
		if (w == 0 || h == 0) {
			return;
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		setGlColor(color);
		drawTexturedQuad(x, y, w, h, 0, 0, 1, 1);
		GlStateManager.disableBlend();
	}

	@Override
	public void blitSprite(
			ResourceLocation texture,
			int spriteWidth,
			int spriteHeight,
			int uStart,
			int vStart,
			int x,
			int y,
			int width,
			int height) {
		blitSprite(texture, spriteWidth, spriteHeight, uStart, vStart, x, y, width, height, 0xFFFFFFFF);
	}

	@Override
	public void blitSprite(
			ResourceLocation texture,
			int spriteWidth,
			int spriteHeight,
			int uStart,
			int vStart,
			int x,
			int y,
			int width,
			int height,
			int color) {
		texture = resolveSprite(texture);
		color = Overlay.applyAlpha(color, opacity());
		if (width == 0 || height == 0) {
			return;
		}
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		setGlColor(color);
		float f = 1.0F / spriteWidth;
		float f1 = 1.0F / spriteHeight;
		drawTexturedQuad(x, y, width, height, uStart * f, vStart * f1, (uStart + width) * f, (vStart + height) * f1);
		GlStateManager.disableBlend();
	}

	@Override
	public float opacity() {
		return OverlayRenderer.animation.alpha;
	}

	@Override
	public float backgroundOpacity() {
		return OverlayRenderer.animation.showHideAlpha;
	}

	// ==================== extra public helpers ==================== //

	/**
	 * Sets the current GL colour from a packed ARGB int.
	 */
	public static void setGlColor(int color) {
		float a = ((color >> 24) & 0xFF) / 255.0F;
		float r = ((color >> 16) & 0xFF) / 255.0F;
		float g = ((color >> 8) & 0xFF) / 255.0F;
		float b = (color & 0xFF) / 255.0F;
		GlStateManager.color(r, g, b, a);
	}

	public void blitSprite(
			ResourceLocation texture,
			int spriteWidth,
			int spriteHeight,
			int u0,
			int v0,
			float x,
			float y,
			float width,
			float height,
			int color) {
		if (width == 0 || height == 0) {
			return;
		}
		color = Overlay.applyAlpha(color, opacity());
		blitSprite(texture, spriteWidth, spriteHeight, u0, v0, Math.round(x), Math.round(y), Math.round(width), Math.round(height), color);
	}

	public void drawFluid(float xPosition, float yPosition, JadeFluidObject fluid, float width, float height, long capacityMb) {
		if (fluid.isEmpty()) {
			return;
		}

		long amount = JadeFluidObject.bucketVolume();
		MutableFloat scaledAmount = new MutableFloat((amount * height) / capacityMb);
		if (amount > 0 && scaledAmount.floatValue() < MIN_FLUID_HEIGHT) {
			scaledAmount.setValue(MIN_FLUID_HEIGHT);
		}
		if (scaledAmount.floatValue() > height) {
			scaledAmount.setValue(height);
		}

		ClientProxy.getFluidSpriteAndColor(fluid, (sprite, color) -> {
			if (sprite == null) {
				float maxY = yPosition + height;
				if (color == -1) {
					color = 0xAAAAAAAA;
				}
				fill(xPosition, maxY - scaledAmount.floatValue(), xPosition + width, maxY, color);
			} else {
				color = Overlay.applyAlpha(color, opacity());
				blitTiledSprite(sprite, xPosition, yPosition, width, height, color);
			}
		});
	}

	public void blitTiledSprite(
			TextureAtlasSprite sprite,
			float x,
			float y,
			float width,
			float height,
			int color) {
		if (width <= 0 || height <= 0) {
			return;
		}
		int tileWidth = 16;
		int tileHeight = 16;
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		setGlColor(color);
		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
		for (int i = 0; i < width; i += tileWidth) {
			float u = Math.min(tileWidth, width - i);
			for (int j = 0; j < height; j += tileHeight) {
				float v = Math.min(tileHeight, height - j);
				float u0 = sprite.getInterpolatedU(0);
				float u1 = sprite.getInterpolatedU(u * 16 / tileWidth);
				float v0 = sprite.getInterpolatedV(0);
				float v1 = sprite.getInterpolatedV(v * 16 / tileHeight);
				int px = Math.round(x + i);
				int py = Math.round(y + j);
				int pw = Math.round(u);
				int ph = Math.round(v);
				buffer.pos(px, py + ph, 0.0D).tex(u0, v1).endVertex();
				buffer.pos(px + pw, py + ph, 0.0D).tex(u1, v1).endVertex();
				buffer.pos(px + pw, py, 0.0D).tex(u1, v0).endVertex();
				buffer.pos(px, py, 0.0D).tex(u0, v0).endVertex();
			}
		}
		tessellator.draw();
		GlStateManager.disableBlend();
	}

	/**
	 * Generic blit: draw a region of a texture.
	 */
	public void blit(
			ResourceLocation tex,
			int x,
			int y,
			float u,
			float v,
			int w,
			int h,
			int texW,
			int texH,
			int color) {
		color = Overlay.applyAlpha(color, opacity());
		Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		setGlColor(color);
		float f = 1.0F / texW;
		float f1 = 1.0F / texH;
		drawTexturedQuad(x, y, w, h, u * f, v * f1, (u + w) * f, (v + h) * f1);
		GlStateManager.disableBlend();
	}

	/**
	 * Returns the Jade font wrapper for layout measuring.
	 */
	public static JadeFont font() {
		return FONT.get();
	}
}
