package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class MobBreedingProvider implements StreamServerDataProvider<EntityAccessor, Integer> {
	public static final MobBreedingProvider INSTANCE = new MobBreedingProvider();
	private static final int IN_LOVE = -1;

	@Override
	public @Nullable Integer streamData(EntityAccessor accessor) {
		Entity entity = accessor.getEntity();
		int time;
		if (entity instanceof EntityVillager) {
			time = ((EntityVillager) entity).getGrowingAge();
		} else if (entity instanceof EntityAnimal) {
			EntityAnimal animal = (EntityAnimal) entity;
			if (animal.isInLove()) {
				return IN_LOVE;
			}
			time = animal.getGrowingAge();
		} else {
			return null;
		}
		return time > 0 ? time : null;
	}

	@Override
	public DataCodec<Integer> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Integer decode(PacketBuffer buf) {
				return buf.readVarInt();
			}

			@Override
			public void encode(PacketBuffer buf, Integer value) {
				buf.writeVarInt(value);
			}
		};
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_MOB_BREEDING;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			int time = MobBreedingProvider.INSTANCE.decodeFromData(accessor).orElse(0);
			if (time == IN_LOVE) {
				tooltip.add(new TextComponentTranslation("jade.mobbreeding.fed"));
			} else if (time > 0) {
				// 1.12.2: Allay duplication cooldown has no equivalent.
				tooltip.add(new TextComponentTranslation(
						"jade.mobbreeding.time",
						IThemeHelper.get().seconds(time, accessor.tickRate())));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_MOB_BREEDING;
		}
	}
}
