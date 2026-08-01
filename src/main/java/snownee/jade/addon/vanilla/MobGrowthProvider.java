package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
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

public class MobGrowthProvider implements StreamServerDataProvider<EntityAccessor, Integer> {
	public static final MobGrowthProvider INSTANCE = new MobGrowthProvider();

	@Override
	public @Nullable Integer streamData(EntityAccessor accessor) {
		Entity entity = accessor.getEntity();
		if (!(entity instanceof EntityAgeable)) {
			return null;
		}
		int time = -((EntityAgeable) entity).getGrowingAge();
		return time > 0 ? time : null;
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		// 1.12.2: EntityAgeable has no age-lock state or tadpole equivalent.
		return accessor.getEntity() instanceof EntityAgeable && ((EntityAgeable) accessor.getEntity()).isChild();
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
		return JadeIds.MC_MOB_GROWTH;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			int time = MobGrowthProvider.INSTANCE.decodeFromData(accessor).orElse(0);
			if (time > 0) {
				// 1.12.2: fixed 20-tick seconds formatting.
				tooltip.add(new TextComponentTranslation("jade.mobgrowth.time", IThemeHelper.get().seconds(time, accessor.tickRate())));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_MOB_GROWTH;
		}
	}
}
