package snownee.jade.addon.vanilla;

import java.lang.reflect.Field;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerCareer;
import net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

// @MerchantScreen / Villager.getTypeName
public class VillagerProfessionProvider implements IEntityComponentProvider {
	public static final VillagerProfessionProvider INSTANCE = new VillagerProfessionProvider();
	@Nullable
	private static final Field CAREER_ID = findCareerIdField();

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		Entity entity = accessor.getEntity();
		// 1.12.2: VillagerData does not exist. Professions come from the Forge registry, via a
		// differently-named getter on each of the two entity types.
		VillagerProfession profession;
		if (entity instanceof EntityVillager) {
			profession = ((EntityVillager) entity).getProfessionForge();
		} else if (entity instanceof EntityZombieVillager) {
			profession = ((EntityZombieVillager) entity).getForgeProfession();
		} else {
			return;
		}
		if (profession == null) {
			return;
		}
		ITextComponent name = getProfessionName(entity instanceof EntityVillager ? (EntityVillager) entity : null, profession);
		if (name == null) {
			return;
		}
		// 1.12.2: villager levels do not exist -- there is no workSound/level pair to append, so the
		// " - merchant.level.N" suffix and its separator are dropped.
		tooltip.add(name);
	}

	/**
	 * 1.12.2: vanilla's own display name uses {@code entity.Villager.<career>}, keyed on the career
	 * rather than the profession ({@code minecraft:priest} shows as "Cleric", {@code minecraft:smith}
	 * as "Armorer"). Zombie villagers do not expose an equivalent career field, so they fall back to
	 * their profession's primary career.
	 */
	@Nullable
	private static ITextComponent getProfessionName(@Nullable EntityVillager villager, VillagerProfession profession) {
		String careerName = getCareerName(villager, profession);
		if (careerName != null) {
			String key = "entity.Villager." + careerName;
			if (JadeUI.hasTranslation(key)) {
				return (ITextComponent) new TextComponentTranslation(key);
			}
		}
		ResourceLocation id = profession.getRegistryName();
		if (id == null) {
			return null;
		}
		String path = careerName != null ? careerName : id.getPath();
		return (ITextComponent) new TextComponentString(StringUtils.capitalize(path.replace('_', ' ')));
	}

	@Nullable
	private static String getCareerName(@Nullable EntityVillager villager, VillagerProfession profession) {
		int careerIndex = 0;
		if (villager != null && CAREER_ID != null) {
			try {
				int careerId = CAREER_ID.getInt(villager);
				if (careerId > 0) {
					careerIndex = careerId - 1;
				}
			} catch (IllegalAccessException ignored) {
				// Use the primary career if the runtime does not permit access.
			}
		}
		VillagerCareer career;
		try {
			career = profession.getCareer(careerIndex);
		} catch (Exception ignored) {
			career = null;
		}
		if (career == null && careerIndex != 0) {
			try {
				career = profession.getCareer(0);
			} catch (Exception ignored) {
				career = null;
			}
		}
		return career == null ? null : career.getName();
	}

	@Nullable
	private static Field findCareerIdField() {
		try {
			return ObfuscationReflectionHelper.findField(EntityVillager.class, "field_175563_bv");
		} catch (RuntimeException ignored) {
			return null;
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_VILLAGER_PROFESSION;
	}

}
