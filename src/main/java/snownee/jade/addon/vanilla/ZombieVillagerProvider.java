package snownee.jade.addon.vanilla;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.monster.EntityZombieVillager;
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

public class ZombieVillagerProvider implements StreamServerDataProvider<EntityAccessor, Integer> {
	public static final ZombieVillagerProvider INSTANCE = new ZombieVillagerProvider();
	private static final @Nullable Field CONVERSION_TIME_FIELD;

	static {
		Field field;
		try {
			field = EntityZombieVillager.class.getDeclaredField("conversionTime");
			field.setAccessible(true);
		} catch (Exception e) {
			// 1.12.2: conversionTime access can be denied by a transformed runtime.
			field = null;
		}
		CONVERSION_TIME_FIELD = field;
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		return ((EntityZombieVillager) accessor.getEntity()).isConverting();
	}

	@Override
	public @Nullable Integer streamData(EntityAccessor accessor) {
		int time = getConversionTime((EntityZombieVillager) accessor.getEntity());
		return time > 0 ? time : null;
	}

	private static int getConversionTime(EntityZombieVillager entity) {
		if (CONVERSION_TIME_FIELD == null) {
			return 0;
		}
		try {
			return CONVERSION_TIME_FIELD.getInt(entity);
		} catch (IllegalAccessException e) {
			return 0;
		}
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
		return JadeIds.MC_ZOMBIE_VILLAGER;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			int time = ZombieVillagerProvider.INSTANCE.decodeFromData(accessor).orElse(0);
			if (time > 0) {
				// 1.12.2: fixed 20-tick seconds formatting.
				tooltip.add(new TextComponentTranslation("jade.zombieConversion.time", IThemeHelper.get().seconds(time, accessor.tickRate())));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_ZOMBIE_VILLAGER;
		}
	}
}
