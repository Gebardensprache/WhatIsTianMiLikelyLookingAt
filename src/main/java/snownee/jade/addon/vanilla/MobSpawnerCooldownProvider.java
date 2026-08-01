package snownee.jade.addon.vanilla;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class MobSpawnerCooldownProvider implements StreamServerDataProvider<BlockAccessor, Integer> {
	public static final MobSpawnerCooldownProvider INSTANCE = new MobSpawnerCooldownProvider();
	private static final @Nullable Field SPAWN_DELAY_FIELD;

	static {
		Field field;
		try {
			field = MobSpawnerBaseLogic.class.getDeclaredField("spawnDelay");
			field.setAccessible(true);
		} catch (Exception e) {
			// 1.12.2: spawnDelay access can be denied by a transformed runtime.
			field = null;
		}
		SPAWN_DELAY_FIELD = field;
	}

	@Override
	public @Nullable Integer streamData(BlockAccessor accessor) {
		TileEntityMobSpawner spawner = accessor.typedBlockEntity();
		int delay = getSpawnDelay(spawner.getSpawnerBaseLogic());
		return delay > 0 ? delay : null;
	}

	private static int getSpawnDelay(MobSpawnerBaseLogic spawner) {
		if (SPAWN_DELAY_FIELD == null) {
			return 0;
		}
		try {
			return SPAWN_DELAY_FIELD.getInt(spawner);
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
		return JadeIds.MC_MOB_SPAWNER_COOLDOWN;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!config.get(JadeIds.MC_MOB_SPAWNER)) {
				return;
			}
			int cooldown = MobSpawnerCooldownProvider.INSTANCE.decodeFromData(accessor).orElse(0);
			if (cooldown > 0) {
				// 1.12.2: regular-spawner delay replaces trial-spawner cooldown.
				tooltip.add(new TextComponentTranslation("jade.trial_spawner_cd", IThemeHelper.get().seconds(cooldown, accessor.tickRate())));
			}
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_MOB_SPAWNER_COOLDOWN;
		}
	}
}
