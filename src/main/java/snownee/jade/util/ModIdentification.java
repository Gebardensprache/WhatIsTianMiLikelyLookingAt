package snownee.jade.util;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TraceableException;
import snownee.jade.api.callback.JadeItemModNameCallback;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.overlay.DisplayHelper;

public class ModIdentification {

	public static final ResourceLocation ID = JadeIds.JADE("mod_id");
	public static final ModIdentification INSTANCE = new ModIdentification();
	public static int NAME_MAX_WIDTH = 160;
	private static final Map<String, Optional<String>> NAMES = Maps.newConcurrentMap();
	private static final Map<String, Optional<String>> CUT_NAMES = Maps.newConcurrentMap();
	@Nullable
	private static WordCutter wordCutter;
	private static boolean translated;
	public static final String JADE_STACK = "$jade:stack";
	public static final MapCodec<ResourceLocation> JADE_STACK_ID_CODEC = JadeCodecs.RESOURCE_LOCATION.fieldOf("id").fieldOf(JADE_STACK);
	public static final String POLYMER_STACK = "$polymer:stack";
	public static final MapCodec<ResourceLocation> POLYMER_STACK_ID_CODEC = JadeCodecs.RESOURCE_LOCATION.fieldOf("id").fieldOf(POLYMER_STACK);

	public static WordCutter wordCutter() {
		WordCutter cutter = wordCutter;
		if (cutter == null) {
			JadeLanguages languages = JadeLanguages.INSTANCE;
			BreakIterator iterator = BreakIterator.getWordInstance(languages.getLocale());
			wordCutter = cutter = new WordCutter(iterator, languages);
		}
		return cutter;
	}

	public static void invalidateCache() {
		NAMES.clear();
		CUT_NAMES.clear();
		wordCutter = null;
	}

	public static String cutName(String fullName, int maxWidth) {
		fullName = fullName.trim();
		if (maxWidth <= 0) {
			return fullName;
		}
		if (DisplayHelper.font().width(fullName) <= maxWidth) {
			return fullName;
		}

		WordCutter cutter = wordCutter();
		cutter.setText(fullName, maxWidth);

		int tokens;
		do {
			tokens = cutter.tokenCount();
			cutter.removeBracketed();
			cutter.trim();
		} while (tokens != cutter.tokenCount() && cutter.tooLong());

		afterColon:
		if (cutter.hasColon() && cutter.tooLong()) {
			int start = cutter.findFirst(token -> token.type() == WordCutter.TokenType.COLON);
			if (start == -1) {
				break afterColon;
			}
			String s = cutter.concat(start + 1, cutter.tokenCount()).trim().toLowerCase(Locale.ENGLISH);
			boolean remove = s.endsWith("edition") || s.endsWith("version");
			if (!remove && !s.contains(" ")) {
				remove = s.equals("legacy") || s.startsWith("re");
			}
			if (remove) {
				cutter.removeRange(start, cutter.tokenCount());
				cutter.trim();
			}
		}

		if (cutter.tooLong()) {
			WordCutter.Token last = cutter.tokens().getLast();
			if (last.type() == WordCutter.TokenType.WORD && last.str().toLowerCase(Locale.ENGLISH).equals("mod")) {
				cutter.removeRange(cutter.tokenCount() - 1, cutter.tokenCount());
				cutter.trim();
			}
		}

		cutter.cutToMaxWidth(true);
		return cutter.toString();
	}

	public static Optional<String> getModName(String namespace) {
		return getModName(namespace, NAME_MAX_WIDTH);
	}

	public static Optional<String> getModName(String namespace, int maxWidth) {
		if (maxWidth != NAME_MAX_WIDTH) {
			return getModNameInternal(namespace, maxWidth);
		}
		return CUT_NAMES.computeIfAbsent(namespace, $ -> getModNameInternal($, NAME_MAX_WIDTH));
	}

	public static Optional<String> getModNameInternal(String namespace, int maxWidth) {
		String fullName = getModFullName(namespace).orElse(null);
		if (fullName == null) {
			return Optional.empty();
		}
		return Optional.of(cutName(fullName, maxWidth));
	}

	public static Optional<String> getModFullName(String namespace) {
		return NAMES.computeIfAbsent(
				namespace, $ -> {
					Optional<String> fromTranslation = Optional.empty();
					String key = "jade.modName." + $;
					if (JadeUI.hasTranslation(key)) {
						fromTranslation = Optional.of(I18n.format(key));
					} else {
						key = "itemGroup." + $;
						if (JadeUI.hasTranslation(key)) {
							fromTranslation = Optional.of(I18n.format(key));
						}
					}
					Optional<String> fromLoader = ClientProxy.getModName($, translated)
							.map(s -> TextFormatting.getTextWithoutFormattingCodes(s));
					if (!translated && fromLoader.isPresent()) {
						return fromLoader;
					}
					return fromTranslation.isPresent() ? fromTranslation : fromLoader;
				});
	}

	public static String getModName(ResourceLocation id) {
		return getModName(id.getNamespace()).orElse(id.getNamespace());
	}

	public static String getModName(Block block) {
		ResourceLocation id;
		try {
			id = CommonProxy.getId(block);
		} catch (Throwable e) {
			throw TraceableException.create(e, block.getRegistryName().getNamespace());
		}
		return getModName(id);
	}

	/**
	 * Returns the mod name for a block state, delegating to the block overload.
	 */
	public static String getModName(IBlockState state) {
		return getModName(state.getBlock());
	}

	public static Optional<ResourceLocation> getSpecialId(ItemStack stack) {
		// 1.12.2: components don't exist; use NBT
		if (stack.hasTagCompound() && stack.getTagCompound() != null) {
			NBTTagCompound tag = stack.getTagCompound();
			if (tag.hasKey(JADE_STACK)) {
				String idStr = tag.getString(JADE_STACK);
				if (!idStr.isEmpty()) {
					return Optional.of(new ResourceLocation(idStr));
				}
			} else if (tag.hasKey(POLYMER_STACK)) {
				String idStr = tag.getString(POLYMER_STACK);
				if (!idStr.isEmpty()) {
					return Optional.of(new ResourceLocation(idStr));
				}
			}
		}
		return Optional.empty();
	}

	public static String getModId(ItemStack stack) {
		Optional<ResourceLocation> specialId = getSpecialId(stack);
		if (specialId.isPresent()) {
			return specialId.orElseThrow().getNamespace();
		}
		return CommonProxy.getModIdFromItem(stack);
	}

	public static String getModName(ItemStack stack) {
		String id;
		try {
			for (JadeItemModNameCallback callback : WailaClientRegistration.instance().itemModNameCallback.callbacks()) {
				String s = callback.gatherItemModName(stack);
				if (!Strings.isNullOrEmpty(s)) {
					return s;
				}
			}
			id = getModId(stack);
		} catch (Throwable e) {
			throw TraceableException.create(e, Item.REGISTRY.getNameForObject(stack.getItem()).getNamespace());
		}
		return getModName(id).orElse(id);
	}

	public static String getModName(Entity entity) {
		if (entity instanceof EntityPainting painting) {
			// 1.12.2: Painting doesn't have variants; use registry name of the painting
			return getModName(EntityList.getKey(painting.getClass()));
		} else if (entity instanceof EntityItem itemEntity) {
			return getModName(itemEntity.getItem());
		} else if (entity instanceof EntityFallingBlock fallingBlock) {
			return getModName(fallingBlock.getBlock());
		} else if (entity instanceof EntityVillager villager) {
			// 1.12.2: Villager profession is int-based, not registry
			return getModName(EntityList.getKey(villager.getClass()));
		}
		ResourceLocation id;
		try {
			id = CommonProxy.getId(entity.getClass());
		} catch (Throwable e) {
			ResourceLocation key = EntityList.getKey(entity.getClass());
			if (key == null) {
				key = new ResourceLocation("minecraft");
			}
			throw TraceableException.create(e, key.getNamespace());
		}
		// 1.12.2: EntityList.getKey returns null for classes without a registry entry
		// (notably EntityPlayer/EntityPlayerSP, which are never registered as mobs).
		// Fall back to minecraft so tooltip gathering on the player never NPEs.
		if (id == null) {
			id = new ResourceLocation("minecraft");
		}
		return getModName(id);
	}

	public static void setTranslated(boolean translated) {
		if (ModIdentification.translated == translated) {
			return;
		}
		ModIdentification.translated = translated;
		invalidateCache();
	}
}
