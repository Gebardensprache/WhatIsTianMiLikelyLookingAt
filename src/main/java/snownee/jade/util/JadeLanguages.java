package snownee.jade.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtils;
import snownee.jade.Jade;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StringRepresentable;

public class JadeLanguages implements WordCutter.TokenClassifier,
		IResourceManagerReloadListener {
	public static final ResourceLocation ID = JadeIds.JADE("languages");
	public static final JadeLanguages INSTANCE = new JadeLanguages();
	private final EnumMap<WordCutter.TokenType, Pattern> tokens = new EnumMap<>(WordCutter.TokenType.class);
	private final Cache<String, WordCutter.TokenType> tokenCache = CacheBuilder.newBuilder().maximumSize(100).build();
	private Map<String, Pattern> nameClasses = Map.of();
	private final Cache<String, String> nameClassCache = CacheBuilder.newBuilder().maximumSize(100).build();
	private Locale locale = Locale.ENGLISH;
	private boolean rtl;
	private static final List<String> hackyPackFeatures = List.of(
			"container.crafter",
			"container.inventory",
			"container.crafting",
			"container.chest",
			"entity.minecraft.chest_boat");
	private Map<String, String> cleanTranslations = Map.of();
	private final Cache<String, Optional<String>> cleanTranslationCache = CacheBuilder.newBuilder().maximumSize(100).build();

	public String toCleanTranslation(String text) {
		if (text.startsWith("tile.") || text.startsWith("item.") || text.startsWith("entity.")) {
			try {
				Optional<String> clean = cleanTranslationCache.get(
						text,
						() -> Optional.ofNullable(getCleanTranslation(text)));
				if (clean.isPresent()) {
					return clean.get();
				}
			} catch (ExecutionException _) {
			}
		}
		return text;
	}

	/**
	 * Reloads Jade's language metadata after a resource reload.
	 *
	 * <p>1.12.2: replaces {@code LanguageManagerMixin}, which injected at the RETURN of
	 * {@code LanguageManager.onResourceManagerReload} and captured its {@code languageStack}
	 * local. Registered as an {@link net.minecraft.client.resources.IResourceManagerReloadListener}
	 * from {@code ClientProxy.init()} instead. The language stack is not exposed by
	 * {@code LanguageManager}, so it is rebuilt here exactly as vanilla does it:
	 * {@code ["en_us"]} plus the current language when it differs.
	 *
	 * @param resourceManager the reloaded resource manager
	 */
	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		Minecraft mc = Minecraft.getMinecraft();
		LanguageManager languageManager = mc.getLanguageManager();
		List<String> languageStack = Lists.newArrayList("en_us");
		if (languageManager != null) {
			String current = languageManager.getCurrentLanguage().getLanguageCode();
			if (!"en_us".equals(current)) {
				languageStack.add(current);
			}
		}
		onResourceManagerReload(mc, languageStack);
	}

	public void onResourceManagerReload(Minecraft mc, List<String> languageStack) {
		tokens.clear();
		tokenCache.invalidateAll();
		nameClasses = Map.of();
		nameClassCache.invalidateAll();
		cleanTranslationCache.invalidateAll();
		try {
			JsonObject jsonObject = JsonConfig.GSON.fromJson(I18n.format("jade.metadata"), JsonObject.class);
			Metadata metadata = Metadata.CODEC.parse(JsonOps.INSTANCE, jsonObject).getOrThrow();
			String langCode = mc.getLanguageManager().getCurrentLanguage().getLanguageCode();
			if (metadata.lang.contains(langCode)) {
				String[] langSplit = langCode.split("_", 2);
				locale = langSplit.length == 1 ? new Locale(langSplit[0]) : new Locale(langSplit[0], langSplit[1]);
				rtl = metadata.rtl;
				Preconditions.checkState(!metadata.tokens.containsKey(WordCutter.TokenType.WORD), "Word token type is not allowed");
				tokens.putAll(metadata.tokens);
				nameClasses = metadata.nameClasses;
			}
		} catch (Throwable e) {
			Jade.LOGGER.error("Failed to load Jade language metadata", e);
		}

		if (hasHackyPack()) {
			Set<String> hackyKeys = Sets.newHashSet();
			Map<String, String> translations = Maps.newHashMap();

			for (String languageCode : languageStack) {
				String path = String.format(Locale.ROOT, "lang/%s.json", languageCode);

				for (String namespace : mc.getResourceManager().getResourceDomains()) {
					try {
						ResourceLocation location = new ResourceLocation(namespace, path);
						List<IResource> resources = mc.getResourceManager()
								.getAllResources(location);
						appendFrom(languageCode, resources, hackyKeys, translations);
					} catch (Exception var10) {
						// ignore missing resources
					}
				}
			}

			translations.keySet().removeIf(key -> !hackyKeys.contains(key));
			cleanTranslations = Map.copyOf(translations);
		} else {
			cleanTranslations = Map.of();
		}
	}

	public @Nullable String getCleanTranslation(String key) {
		return cleanTranslations.get(key);
	}

	private static boolean hasHackyPack() {
		for (String key : hackyPackFeatures) {
			if (isPuaString(I18n.format(key))) {
				return true;
			}
		}
		return false;
	}

	private static boolean isPuaString(String s) {
		return s.codePoints().allMatch(codePoint -> {
			int type = Character.getType(codePoint);
			return type == Character.PRIVATE_USE || type == Character.SPACE_SEPARATOR;
		});
	}

	private static void appendFrom(
			String languageCode,
			List<IResource> resources,
			Set<String> hackyKeys,
			Map<String, String> globalTranslations) {
		for (IResource resource : resources) {
			try {
				InputStream inputStream = resource.getInputStream();

				try {
					JsonObject entries = JsonConfig.GSON.fromJson(
							new InputStreamReader(inputStream, StandardCharsets.UTF_8),
							JsonObject.class);
					Map<String, String> translations = Maps.newHashMap();

					for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
						String text = entry.getValue().getAsString();
						translations.put(entry.getKey(), text);
					}

					boolean hacky = false;
					for (String key : hackyPackFeatures) {
						if (translations.containsKey(key) && isPuaString(translations.get(key))) {
							hacky = true;
							break;
						}
					}
					if (hacky) {
						List<String> keysToRemove = Lists.newArrayList();
						translations.forEach((key, value) -> {
							if (isPuaString(value)) {
								keysToRemove.add(key);
							}
						});
						for (String key : keysToRemove) {
							translations.remove(key);
							hackyKeys.add(key);
						}
					}
					globalTranslations.putAll(translations);
				} catch (Throwable var9) {
					try {
						inputStream.close();
					} catch (Throwable var8) {
						var9.addSuppressed(var8);
					}

					throw var9;
				}

				inputStream.close();
			} catch (IOException var10) {
				Jade.LOGGER.warn("Failed to load translations for {} from pack {}", languageCode, resource.getResourcePackName(), var10);
			}
		}
	}

	public boolean isRTL() {
		return rtl;
	}

	public String getNameClass(String name) {
		if (nameClasses.isEmpty()) {
			return "other";
		}
		try {
			return nameClassCache.get(
					name, () -> {
						for (Map.Entry<String, Pattern> entry : nameClasses.entrySet()) {
							if (entry.getValue().matcher(name).matches()) {
								return entry.getKey();
							}
						}
						return "other";
					});
		} catch (ExecutionException e) {
			return "other";
		}
	}

	public Locale getLocale() {
		return locale;
	}

	@Override
	public WordCutter.TokenType classify(String s) {
		if (s.isEmpty() || s.isBlank()) {
			return WordCutter.TokenType.SEPARATOR;
		}
		if (tokens.isEmpty()) {
			return switch (s) {
				case "(", "[", "<" -> WordCutter.TokenType.LEFT_BRACKET;
				case ")", "]", ">" -> WordCutter.TokenType.RIGHT_BRACKET;
				case ":" -> WordCutter.TokenType.COLON;
				case "|", "-", ",", "/", "&" -> WordCutter.TokenType.SYMBOL;
				default -> WordCutter.TokenType.WORD;
			};
		} else {
			try {
				return tokenCache.get(
						s, () -> {
							for (Map.Entry<WordCutter.TokenType, Pattern> entry : tokens.entrySet()) {
								if (entry.getValue().matcher(s).matches()) {
									return entry.getKey();
								}
							}
							return WordCutter.TokenType.WORD;
						});
			} catch (ExecutionException e) {
				return WordCutter.TokenType.WORD;
			}
		}
	}

	private static final class Metadata {
		private final List<String> lang;
		private final boolean rtl;
		private final Map<WordCutter.TokenType, Pattern> tokens;
		private final Map<String, Pattern> nameClasses;

		Metadata(List<String> lang, boolean rtl, Map<WordCutter.TokenType, Pattern> tokens, Map<String, Pattern> nameClasses) {
			this.lang = lang;
			this.rtl = rtl;
			this.tokens = tokens;
			this.nameClasses = nameClasses;
		}

		List<String> lang() {
			return lang;
		}

		boolean rtl() {
			return rtl;
		}

		Map<WordCutter.TokenType, Pattern> tokens() {
			return tokens;
		}

		Map<String, Pattern> nameClasses() {
			return nameClasses;
		}

		static final Codec<Metadata> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.listOf().fieldOf("lang").forGetter(Metadata::lang),
				Codec.BOOL.optionalFieldOf("rtl", false).forGetter(Metadata::rtl),
				Codec.unboundedMap(StringRepresentable.fromEnum(WordCutter.TokenType.values()), Codec.STRING.xmap(Pattern::compile, Pattern::pattern))
						.optionalFieldOf("tokens", Map.of())
						.forGetter(Metadata::tokens),
				Codec.unboundedMap(Codec.STRING, Codec.STRING.xmap(Pattern::compile, Pattern::pattern))
						.optionalFieldOf("nameClasses", Map.of())
						.forGetter(Metadata::nameClasses)).apply(i, Metadata::new));
	}
}
