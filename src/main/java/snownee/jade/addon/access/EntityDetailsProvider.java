package snownee.jade.addon.access;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.IShearable;
import snownee.jade.JadeClient;
import snownee.jade.addon.core.ObjectNameProvider;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class EntityDetailsProvider implements IEntityComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		Entity entity = accessor.getEntity();
		String objectName = tooltip.getString(JadeIds.CORE_OBJECT_NAME);
		if (entity instanceof EntityCreeper) {
			if (((EntityCreeper) entity).getPowered()) {
				AccessibilityPlugin.replaceTitle(tooltip, objectName, "creeper.powered");
			}
		} else if (entity instanceof EntityWither) {
			// 1.12.2: the wither's "powered" state is its spawning invulnerability phase.
			if (((EntityWither) entity).getInvulTime() > 0) {
				AccessibilityPlugin.replaceTitle(tooltip, objectName, "wither.powered");
			}
		} else if (entity instanceof EntityZombieVillager) {
			if (((EntityZombieVillager) entity).isConverting()) {
				AccessibilityPlugin.replaceTitle(tooltip, objectName, "zombie_villager.curing");
			}
		} else if (entity instanceof EntitySlime) {
			// 1.12.2: the modern Bee and Goat branches are dropped -- bees have no nectar
			// state here and goats do not exist. The CopperGolem branch is dropped too
			// (no such entity); its weathering-state handling (exposed/weathered/oxidized)
			// has no 1.12.2 source of weathering states.
			String message = tooltip.getString(JadeIds.CORE_OBJECT_NAME);
			ITextComponent title = IThemeHelper.get().title(JadeClient.format("jade.access.slime.size", message, ((EntitySlime) entity).getSlimeSize()));
			tooltip.replace(JadeIds.CORE_OBJECT_NAME, title);
		}
		if (entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isChild()) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "entity.baby");
		}
		// 1.12.2: CommonProxy.isShearable does not exist (the modern TriState helper went
		// away with the neoforge Shearable interface). The same logic is re-expressed over
		// Forge's IShearable: baby sheep and mooshrooms report false (their drops would be
		// empty), snowmen with a pumpkin report true, sheared sheep false.
		boolean shearable = false;
		boolean shearablePresent = false;
		if (entity instanceof EntitySheep) {
			shearablePresent = true;
			shearable = !((EntitySheep) entity).getSheared() && !((EntitySheep) entity).isChild();
		} else if (entity instanceof EntityMooshroom) {
			shearablePresent = true;
			shearable = ((EntityAgeable) entity).getGrowingAge() >= 0;
		} else if (entity instanceof EntitySnowman) {
			shearablePresent = true;
			shearable = ((EntitySnowman) entity).isPumpkinEquipped();
		} else if (entity instanceof IShearable) {
			shearablePresent = true;
			shearable = ((IShearable) entity).isShearable(null, entity.world, new BlockPos(entity));
		}
		if (shearablePresent && !shearable) {
			// The modern CopperGolem branch (shearable -> "entity.shearable") is dropped:
			// the only 1.12.2 entity that reports "ready for shearing" is the snowman, and
			// the modern code only surfaces "shearable" for CopperGolem. Everything else
			// follows the modern general case: not shearable -> "entity.sheared".
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "entity.sheared");
		}
		// 1.12.2: the modern Mob#isSaddled branch is mapped to the actual saddle getters:
		// EntityPig#getSaddled and AbstractHorse#isHorseSaddled.
		if (entity instanceof EntityPig && ((EntityPig) entity).getSaddled()) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "entity.saddled");
		} else if (entity instanceof AbstractHorse && ((AbstractHorse) entity).isHorseSaddled()) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "entity.saddled");
		}

		String color = EntityVariantHelper.getVariantName(entity, true);
		if (color != null) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "entity." + color);
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_ENTITY_DETAILS;
	}

	@Override
	public int getDefaultPriority() {
		return ObjectNameProvider.ForEntity.INSTANCE.getDefaultPriority() + 10;
	}
}
