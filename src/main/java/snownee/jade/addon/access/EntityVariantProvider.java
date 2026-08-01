package snownee.jade.addon.access;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

public class EntityVariantProvider implements IEntityComponentProvider {
	/**
	 * 1.12.2: the modern Markings enum does not exist -- the horse marking index is encoded
	 * in EntityHorse#getHorseVariant() as ((variant & 65280) >> 8) % 5, mapped over the
	 * same names the modern code uses.
	 */
	private static final Map<Integer, String> MARKINGS = Maps.newHashMap();

	static {
		MARKINGS.put(0, "none");
		MARKINGS.put(1, "white");
		MARKINGS.put(2, "white_field");
		MARKINGS.put(3, "white_dots");
		MARKINGS.put(4, "black_dots");
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		Entity entity = accessor.getEntity();
		String variantName = EntityVariantHelper.getVariantName(entity, false);
		if (variantName == null) {
			return;
		}
		String s = variantName;
		ResourceLocation entityKey = EntityList.getKey(entity);
		// 1.12.2: BuiltInRegistries does not exist -- the entity type key comes from
		// EntityList, and toShortLanguageKey() is emulated as the key's path alone.
		String type = entityKey == null ? "unknown" : entityKey.getPath();
		String key = "jade.access.entity." + type + "." + s;
		if (JadeUI.hasTranslation(key) || (config.get(JadeIds.DEBUG_SPECIAL_REGISTRY_NAME) && !accessor.showDetails())) {
			s = I18n.format(key);
		} else {
			s = s.replace('.', ' ').replace('_', ' ');
		}
		tooltip.add(new TextComponentTranslation("jade.access.entity.variant", s));
		if (entity instanceof EntityHorse) {
			EntityHorse horse = (EntityHorse) entity;
			int variant = horse.getHorseVariant();
			int markings = ((variant & 65280) >> 8) % 5;
			String marking = MARKINGS.get(markings);
			if (marking == null) {
				marking = "none";
			}
			String key2 = "jade.access.entity.horse_markings." + marking;
			String s2;
			if (JadeUI.hasTranslation(key2) || (config.get(JadeIds.DEBUG_SPECIAL_REGISTRY_NAME) && !accessor.showDetails())) {
				s2 = I18n.format(key2);
			} else {
				s2 = marking.replace('_', ' ');
			}
			tooltip.add(new TextComponentTranslation("jade.access.entity.horse_markings", s2));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_ENTITY_VARIANT;
	}
}
