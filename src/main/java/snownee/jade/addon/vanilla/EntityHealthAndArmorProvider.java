package snownee.jade.addon.vanilla;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.impl.ui.ArmorElement;
import snownee.jade.impl.ui.HealthElement;

public class EntityHealthAndArmorProvider implements StreamServerDataProvider<EntityAccessor, Float> {
	public static final EntityHealthAndArmorProvider INSTANCE = new EntityHealthAndArmorProvider();

	@Override
	public @Nullable Float streamData(EntityAccessor accessor) {
		float absorption = ((EntityLivingBase) accessor.getEntity()).getAbsorptionAmount();
		return absorption > 0 ? absorption : 0;
	}

	@Override
	public DataCodec<Float> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Float decode(PacketBuffer buf) {
				return buf.readFloat();
			}

			@Override
			public void encode(PacketBuffer buf, Float value) {
				buf.writeFloat(value);
			}
		};
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		return isHealthVisible((EntityLivingBase) accessor.getEntity());
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ENTITY_HEALTH;
	}

	@Override
	public int getDefaultPriority() {
		return -8000;
	}

	private static boolean isHealthVisible(EntityLivingBase entity) {
		return !(entity instanceof EntityArmorStand)
				&& !(entity instanceof EntityDragon)
				&& !(entity instanceof EntityWither);
	}

	public static class Client extends EntityHealthAndArmorProvider implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			boolean healthText = false;
			boolean armorText = false;
			List<Element> elements = Lists.newArrayListWithExpectedSize(2);
			EntityLivingBase living = (EntityLivingBase) accessor.getEntity();
			if (config.get(JadeIds.MC_ENTITY_HEALTH) && isHealthVisible(living)) {
				float health = living.getHealth();
				float maxHealth = living.getMaxHealth();
				float absorption = decodeFromData(accessor).orElse(0F);
				// 1.12.2: frozen-heart sprites do not exist.
				HealthElement healthElement = new HealthElement(maxHealth, health, absorption);
				elements.add(healthElement.tag(JadeIds.MC_ENTITY_HEALTH));
				healthText = healthElement.showText();
			}
			if (config.get(JadeIds.MC_ENTITY_ARMOR) && living.getTotalArmorValue() > 0) {
				ArmorElement armorElement = new ArmorElement(living.getTotalArmorValue());
				elements.add(armorElement.tag(JadeIds.MC_ENTITY_ARMOR));
				armorText = armorElement.showText();
			}
			if (healthText && armorText) {
				tooltip.add(elements.get(0));
				tooltip.append(JadeUI.spacer(4, 0));
				tooltip.append(elements.get(1));
			} else {
				elements.forEach(tooltip::add);
			}
		}

		@Override
		public boolean isRequired() {
			return true;
		}
	}
}
