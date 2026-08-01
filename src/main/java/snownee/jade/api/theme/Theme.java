package snownee.jade.api.theme;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.ColorPalette;
import snownee.jade.api.ui.Element;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.ui.BoxElementImpl;

/**
 * Complete visual theme description used by Jade's renderer.
 * <p>
 * 1.12.2: {@code Identifier} replaced with {@code ResourceLocation};
 * {@code withPath} replaced with explicit {@code new ResourceLocation} calls;
 * {@code getPath()} replaced with {@code getPath()};
 * {@code getNamespace()} replaced with {@code getNamespace()}.
 * The {@code modifyIcon(@Nullable Element)} method drops the
 * {@code GuiMetadataSection}/{@code GuiSpriteScaling}/{@code NineSlice}
 * metadata lookup and the
 * {@code Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.GUI)}
 * call (none of which exist in 1.12.2's sprite system), using
 * {@code iconSlotInflation} directly as the box padding amount.
 */
public class Theme {

	public @Nullable ResourceLocation id;
	public String styleName;
	public BoxStyle tooltipStyle;
	public BoxStyle nestedBoxStyle;
	public BoxStyle viewGroupStyle;
	public TextSetting text;
	public float changeOpacity;
	public boolean lightColorScheme;
	public @Nullable ResourceLocation iconSlotSprite;
	public int iconSlotInflation;
	public @Nullable BoxElement iconSlotSpriteCache;
	public SneakyDetails sneakyDetails;
	public ColorPalette progressColors;
	public Map<ResourceLocation, ResourceLocation> spriteMapping;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public Theme(
			String styleName,
			BoxStyle tooltipStyle,
			BoxStyle nestedBoxStyle,
			BoxStyle viewGroupStyle,
			TextSetting text,
			float changeOpacity,
			boolean lightColorScheme,
			Optional<ResourceLocation> iconSlotSprite,
			int iconSlotInflation,
			SneakyDetails sneakyDetails,
			ColorPalette progressColors,
			Map<ResourceLocation, ResourceLocation> spriteMapping) {
		this.styleName = styleName;
		this.tooltipStyle = tooltipStyle;
		this.nestedBoxStyle = nestedBoxStyle;
		this.viewGroupStyle = viewGroupStyle;
		this.text = text;
		this.changeOpacity = changeOpacity;
		this.lightColorScheme = lightColorScheme;
		this.iconSlotSprite = iconSlotSprite.orElse(null);
		this.iconSlotInflation = iconSlotInflation;
		this.sneakyDetails = sneakyDetails;
		this.progressColors = progressColors;
		this.spriteMapping = spriteMapping;
	}

	public ResourceLocation fullId() {
		return Objects.requireNonNull(id);
	}

	public ResourceLocation mainId() {
		if (Objects.requireNonNull(id).getPath().contains("/")) {
			return new ResourceLocation(id.getNamespace(), id.getPath().substring(0, id.getPath().indexOf('/')));
		} else {
			return id;
		}
	}

	public String styleId() {
		if (Objects.requireNonNull(id).getPath().contains("/")) {
			return id.getPath().substring(id.getPath().indexOf('/') + 1);
		} else {
			return "";
		}
	}

	public ResourceLocation mapSprite(ResourceLocation sprite) {
		return spriteMapping.getOrDefault(sprite, sprite);
	}

	public @Nullable Element modifyIcon(@Nullable Element icon) {
		if (icon == null) {
			return null;
		}

		IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
		if (!overlay.shouldShowIcon() || overlay.getIconMode() == IWailaConfig.IconMode.INLINE) {
			return null;
		}

		if (iconSlotSprite != null) {
			if (iconSlotSpriteCache == null) {
				// 1.12.2: no GuiMetadataSection/GuiSpriteScaling/NineSlice system exists.
				// Padding uses iconSlotInflation directly without sprite-metadata border extraction.
				int[] padding = new int[4];
				Arrays.fill(padding, iconSlotInflation);
				iconSlotSpriteCache = new BoxElementImpl(new Tooltip(), BoxStyle.simple(iconSlotSprite, padding));
			}
			ITooltip tooltip1 = iconSlotSpriteCache.getTooltip();
			tooltip1.clear();
			tooltip1.add(icon);
			iconSlotSpriteCache.updateSize();
			icon = iconSlotSpriteCache;
		}
		return icon.tag(JadeIds.CORE_ROOT_ICON);
	}
}
