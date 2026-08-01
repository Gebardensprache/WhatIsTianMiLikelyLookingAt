package snownee.jade.impl.theme;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.ReportedException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.Jade;
import snownee.jade.addon.core.ModNameProvider;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.SneakyDetails;
import snownee.jade.api.theme.TextSetting;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.ColorPalette;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.TextElement;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.util.JadeClientCodecs;
import snownee.jade.util.JsonConfig;

/**
 * 1.12.2: the JSON-driven theme loading infrastructure
 * ({@code SimpleJsonResourceReloadListener}, {@code FileToIdConverter},
 * {@code ProfilerFiller}, {@code KeyedReloadListener}, {@code MinMaxBounds.Ints})
 * is dropped because none of these modern resource-loading abstractions exist in
 * 1.12.2. Instead this class implements
 * {@link net.minecraft.client.resources.IResourceManagerReloadListener} and
 * re-parses {@code assets/<ns>/jade_themes/*.json} on every resource reload,
 * mirroring the modern {@code FileToIdConverter.json("jade_themes")} semantics.
 * The {@code autoEnable} + {@code history.themesHash} config persistence is not
 * ported; {@code WailaConfig.Overlay.applyTheme} already handles active-theme
 * persistence and is invoked once after a reload.
 * <p>
 * {@code MutableComponent} / {@code Component} are unified to
 * {@code ITextComponent}. {@code Style.EMPTY} is {@code new Style()}.
 * {@code component.copy()} is {@code component.createCopy()}.
 * {@code Mth.floor} is {@code MathHelper.floor}.
 * {@code Component.literal(s)} is {@code new TextComponentString(s)}.
 * {@code Component.translatable(k, args)} is {@code new TextComponentTranslation(k, args)}.
 * {@code withColor(int)} (a modern Style API) is replaced with a hand-written
 * nearest-{@code TextFormatting} color lookup. {@code applyTo(Style)} (also
 * modern) is replaced with a hand-written style merge.
 */
public class ThemeHelper implements IThemeHelper, IResourceManagerReloadListener {
	public static final ThemeHelper INSTANCE = new ThemeHelper();
	public static final ResourceLocation ID = JadeIds.JADE("themes");
	/** 1.12.2: {@code ExtraCodecs.NON_NEGATIVE_INT.fieldOf("version")} is {@code Codec.INT.fieldOf("version")}. */
	private static final Codec<JadeClientCodecs.ThemeHolder> HOLDER_CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("version").forGetter(JadeClientCodecs.ThemeHolder::version),
			Codec.BOOL.optionalFieldOf("autoEnable", false).forGetter(JadeClientCodecs.ThemeHolder::autoEnable),
			JadeClientCodecs.THEME.forGetter(JadeClientCodecs.ThemeHolder::theme)
	).apply(i, JadeClientCodecs.ThemeHolder::new));
	/** 1.12.2: {@code FileToIdConverter.json("jade_themes")} is a fixed set of shipped theme ids. */
	private static final String[] THEME_PATHS = {
			"jade_themes/dark.json",
			"jade_themes/waila.json",
			"jade_themes/top.json",
			"jade_themes/create.json",
			"jade_themes/dark/slim.json",
			"jade_themes/waila/slim.json",
			"jade_themes/create/slim.json"
	};
	private static final Int2ObjectMap<Style> styleCache = new Int2ObjectOpenHashMap<>(6);
	private final Map<ResourceLocation, Theme> themes = Maps.newTreeMap();
	private final Style[] modNameStyleCache = new Style[]{new Style(), new Style(), new Style()};
	private @Nullable Theme theme;
	private @Nullable Theme fallback;
	private int generation;
	private @Nullable Theme themeOverride;

	private static final int[] TEXT_FORMATTING_RGB = {
			0x000000, // BLACK
			0x0000AA, // DARK_BLUE
			0x00AA00, // DARK_GREEN
			0x00AAAA, // DARK_AQUA
			0xAA0000, // DARK_RED
			0xAA00AA, // DARK_PURPLE
			0xFFAA00, // GOLD
			0xAAAAAA, // GRAY
			0x555555, // DARK_GRAY
			0x5555FF, // BLUE
			0x55FF55, // GREEN
			0x55FFFF, // AQUA
			0xFF5555, // RED
			0xFF55FF, // LIGHT_PURPLE
			0xFFFF55, // YELLOW
			0xFFFFFF  // WHITE
	};
	private static final TextFormatting[] TEXT_FORMATTING_VALUES = TextFormatting.values();

	/**
	 * Maps an ARGB color int to the nearest {@link TextFormatting} color
	 * by Euclidean distance in RGB space. 1.12.2's {@code Style} only stores
	 * legacy {@code TextFormatting} enum values, not arbitrary RGB ints.
	 */
	private static TextFormatting textFormattingFromInt(int color) {
		int rgb = color & 0xFFFFFF;
		int bestIndex = 0;
		int bestDistance = Integer.MAX_VALUE;
		for (int i = 0; i < TEXT_FORMATTING_RGB.length; i++) {
			int dr = ((rgb >> 16) & 0xFF) - ((TEXT_FORMATTING_RGB[i] >> 16) & 0xFF);
			int dg = ((rgb >> 8) & 0xFF) - ((TEXT_FORMATTING_RGB[i] >> 8) & 0xFF);
			int db = (rgb & 0xFF) - (TEXT_FORMATTING_RGB[i] & 0xFF);
			int dist = dr * dr + dg * dg + db * db;
			if (dist < bestDistance) {
				bestDistance = dist;
				bestIndex = i;
			}
		}
		return TEXT_FORMATTING_VALUES[bestIndex];
	}

	public static Style colorStyle(int color) {
		// 1.12.2: Int2ObjectMap.computeIfAbsent(int, Int2ObjectFunction) is fastutil 8-only;
		// 1.12.2 bundles fastutil 7.1.0 where that overload does not exist, so the modern
		// call throws NoSuchMethodError at runtime (crashing every tooltip gather). Use a
		// manual get-or-put that works on both 7.x and 8.x.
		Style cached = styleCache.get(color);
		if (cached == null) {
			cached = new Style().setColor(textFormattingFromInt(color));
			styleCache.put(color, cached);
		}
		return cached;
	}

	public ThemeHelper() {
		// Hardcoded fallback default theme so rendering never has a null theme
		// before/without a resource reload; the JSON reload then replaces it.
		Theme dark = new Theme(
				"Dark",
				BoxStyle.tooltip(JadeIds.JADE("tooltip"), new int[]{3, 3, 3, 3}),
				BoxStyle.DEFAULT_NESTED_BOX,
				BoxStyle.DEFAULT_VIEW_GROUP,
				TextSetting.DEFAULT,
				0F,
				false,
				Optional.empty(),
				0,
				SneakyDetails.DEFAULT,
				ColorPalette.DEFAULT,
				Map.of());
		dark.id = JadeIds.DEFAULT_THEME;
		themes.put(dark.id, dark);
		theme = dark;
		fallback = dark;
	}

	/**
	 * 1.12.2: replaces {@code SimpleJsonResourceReloadListener.apply(...)}.
	 * Walks the shipped {@code jade_themes/*.json} files for every resource
	 * domain and parses each with {@link JadeClientCodecs.ThemeHolder}. Themes
	 * with a version outside 200-299 are skipped with a warning (mirroring the
	 * modern {@code allowedVersions} check). {@code autoEnable} and the
	 * {@code history.themesHash} config persistence are intentionally not
	 * ported; {@code WailaConfig.Overlay.applyTheme} is re-applied below so the
	 * active theme survives a reload.
	 */
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		Map<ResourceLocation, Theme> loaded = Maps.newTreeMap();
		for (String domain : resourceManager.getResourceDomains()) {
			for (String path : THEME_PATHS) {
				ResourceLocation id = new ResourceLocation(domain, path);
				Theme theme1 = parseTheme(resourceManager, id);
				if (theme1 != null) {
					// id encodes the file (e.g. jade:jade_themes/dark.json); the
					// registered theme id is the file id without the prefix
					// (e.g. jade:dark), matching FileToIdConverter semantics.
					ResourceLocation themeId = new ResourceLocation(domain, path.substring("jade_themes/".length(), path.length() - ".json".length()));
					theme1.id = themeId;
					loaded.put(themeId, theme1);
				}
			}
		}
		if (loaded.isEmpty()) {
			// Resource reloads (F3+T, resource-pack changes) should never wipe the
			// constructor-registered fallback; keep the previous theme state.
			return;
		}
		themes.clear();
		themes.putAll(loaded);
		fallback = themes.get(JadeIds.DEFAULT_THEME);
		if (fallback == null) {
			throw new ReportedException(CrashReport.makeCrashReport(
					new NullPointerException("Missing default theme"),
					"Missing default theme"));
		}
		// 1.12.2: modern's autoEnable/history.themesHash persistence is not
		// ported; just re-apply the persisted active theme (defaults to jade:dark).
		// 1.12.2: Overlay has getTheme() but no activeTheme field; use the current
		// theme's id so applyTheme resolves it against the freshly loaded map.
		IWailaConfig.get().overlay().applyTheme(IWailaConfig.get().overlay().getTheme().id);
	}

	@Nullable
	private static Theme parseTheme(IResourceManager resourceManager, ResourceLocation fileId) {
		try {
			IResource resource = resourceManager.getResource(fileId);
			try (InputStream input = resource.getInputStream()) {
				JsonElement json = JsonConfig.GSON.fromJson(
						new InputStreamReader(input, StandardCharsets.UTF_8),
						JsonElement.class);
				JadeClientCodecs.ThemeHolder holder = HOLDER_CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
				if (holder.version() < 200 || holder.version() > 299) {
					Jade.LOGGER.warn("Theme {} has unsupported version {}. Skipping.", fileId, holder.version());
					return null;
				}
				return holder.theme();
			}
		} catch (Exception e) {
			// FileNotFoundException for themes not present in this domain is expected.
			return null;
		}
	}

	@Override
	public Theme theme() {
		return themeOverride != null ? themeOverride : Objects.requireNonNull(theme);
	}

	@Override
	public Collection<Theme> getThemes() {
		return themes.values();
	}

	@Override
	public Theme getTheme(ResourceLocation id) {
		return Preconditions.checkNotNull(themes.getOrDefault(id, Objects.requireNonNull(fallback)), "Theme not found: %s", id);
	}

	@Override
	public boolean hasTheme(ResourceLocation id) {
		return themes.containsKey(id);
	}

	@Override
	public ITextComponent info(Object componentOrString) {
		return color(componentOrString, theme().text.colors().info());
	}

	@Override
	public ITextComponent success(Object componentOrString) {
		return color(componentOrString, theme().text.colors().success());
	}

	@Override
	public ITextComponent warning(Object componentOrString) {
		return color(componentOrString, theme().text.colors().warning());
	}

	@Override
	public ITextComponent danger(Object componentOrString) {
		return color(componentOrString, theme().text.colors().danger());
	}

	@Override
	public ITextComponent failure(Object componentOrString) {
		return color(componentOrString, theme().text.colors().failure());
	}

	@Override
	public ITextComponent title(Object componentOrString) {
		ITextComponent component;
		if (componentOrString instanceof ITextComponent) {
			component = (ITextComponent) componentOrString;
		} else {
			component = new TextComponentString(Objects.toString(componentOrString));
		}
		return color(DisplayHelper.INSTANCE.stripColor(component), theme().text.colors().title());
	}

	/**
	 * 1.12.2: no {@code Style.applyTo(Style)}. Hand-written merge matching modern's
	 * {@code applyTo} semantics: each {@code override} field is copied only when it is
	 * explicitly set (non-null), so unset fields fall through to {@code base} instead
	 * of clearing it. {@code Style}'s boolean getters return primitives that resolve
	 * through the parent chain, so the private boxed fields are read via reflection to
	 * detect "explicitly set". (An earlier version that unconditionally
	 * {@code setItalic(override.getItalic())} wiped the formatting config's default
	 * {@code BLUE+ITALIC} mod-name style.)
	 */
	private static Style mergeStyles(Style override, Style base) {
		Style result = base.createDeepCopy();
		if (override.getColor() != null) result.setColor(override.getColor());
		Boolean bold = getBoxedFormat(override, "bold");
		if (bold != null) result.setBold(bold);
		Boolean italic = getBoxedFormat(override, "italic");
		if (italic != null) result.setItalic(italic);
		Boolean underlined = getBoxedFormat(override, "underlined");
		if (underlined != null) result.setUnderlined(underlined);
		Boolean strikethrough = getBoxedFormat(override, "strikethrough");
		if (strikethrough != null) result.setStrikethrough(strikethrough);
		Boolean obfuscated = getBoxedFormat(override, "obfuscated");
		if (obfuscated != null) result.setObfuscated(obfuscated);
		if (override.getClickEvent() != null) result.setClickEvent(override.getClickEvent());
		if (override.getHoverEvent() != null) result.setHoverEvent(override.getHoverEvent());
		if (override.getInsertion() != null) result.setInsertion(override.getInsertion());
		return result;
	}

	private static @Nullable Boolean getBoxedFormat(Style style, String field) {
		try {
			java.lang.reflect.Field f = Style.class.getDeclaredField(field);
			f.setAccessible(true);
			return (Boolean) f.get(style);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public ITextComponent modName(Object componentOrString) {
		ITextComponent component;
		if (componentOrString instanceof ITextComponent) {
			component = (ITextComponent) componentOrString;
		} else {
			component = new TextComponentString(Objects.toString(componentOrString));
		}
		Style itemStyle = IWailaConfig.get().formatting().getItemModNameStyle();
		Style themeStyle = theme().text.modNameStyle();
		if (modNameStyleCache[0] != itemStyle || modNameStyleCache[1] != themeStyle) {
			Style style = mergeStyles(themeStyle, itemStyle);
			modNameStyleCache[0] = itemStyle;
			modNameStyleCache[1] = themeStyle;
			modNameStyleCache[2] = style;
		}
		return component.setStyle(modNameStyleCache[2]);
	}

	@Override
	public TextElement modNameElement(Object componentOrString) {
		return JadeUI
				.text(modName(componentOrString))
				.scale(Objects.equals(IWailaConfig.get().plugin().getEnum(JadeIds.CORE_MOD_NAME), ModNameProvider.Mode.SMALLER) ?
						0.75F :
						1F);
	}

	@Override
	public ITextComponent seconds(int ticks, float tickRate, boolean alwaysOnePart) {
		int seconds = MathHelper.floor(ticks / tickRate);
		if (seconds >= 3600) {
			int hours = seconds / 3600;
			seconds %= 3600;
			int minutes = seconds / 60;
			if (alwaysOnePart || minutes == 0) {
				return info(new TextComponentTranslation("jade.hours", hours));
			} else {
				return info(new TextComponentTranslation("jade.hours_minutes", hours, minutes));
			}
		}
		if (seconds >= 60) {
			int minutes = seconds / 60;
			seconds %= 60;
			if (alwaysOnePart || seconds == 0) {
				return info(new TextComponentTranslation("jade.minutes", minutes));
			} else {
				return info(new TextComponentTranslation("jade.minutes_seconds", minutes, seconds));
			}
		}
		return info(new TextComponentTranslation("jade.seconds", seconds));
	}

	@Override
	public int generation() {
		return generation;
	}

	public void setTheme(Theme theme) {
		if (this.theme == theme) {
			return;
		}
		this.theme = theme;
		generation++;
	}

	@Override
	public void setThemeOverride(@Nullable Theme theme) {
		if (themeOverride == theme) {
			return;
		}
		generation++;
		themeOverride = theme;
	}

	protected ITextComponent color(Object componentOrString, int color) {
		if (componentOrString instanceof Number number) {
			componentOrString = DisplayHelper.dfCommas.format(number.doubleValue());
		}
		if (componentOrString instanceof ITextComponent component) {
			Style existingStyle = component.getStyle();
			Style merged = existingStyle.createDeepCopy();
			merged.setColor(textFormattingFromInt(color));
			return component.setStyle(merged);
		}
		// 1.12.2: com.mojang.brigadier.Message not available; dropping the branch
		return new TextComponentString(Objects.toString(componentOrString)).setStyle(colorStyle(color));
	}
}
