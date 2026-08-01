package snownee.jade.addon.debug;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

public class BlockLootTableProvider implements StreamServerDataProvider<BlockAccessor, Pair<ResourceLocation, Long>> {
	public static final BlockLootTableProvider INSTANCE = new BlockLootTableProvider();
	private static final @Nullable Field LOOT_TABLE_SEED_FIELD;

	static {
		Field field;
		try {
			// 1.12.2: lootTableSeed is a protected field; the modern
			// getLootTableSeed() accessor does not exist. MCP name works in the dev
			// runtime; in a production (SRG) runtime the lookup fails and the seed
			// degrades to 0, matching the MobSpawnerCooldownProvider fallback.
			field = TileEntityLockableLoot.class.getDeclaredField("lootTableSeed");
			field.setAccessible(true);
		} catch (Exception e) {
			field = null;
		}
		LOOT_TABLE_SEED_FIELD = field;
	}

	@Override
	public @Nullable Pair<ResourceLocation, Long> streamData(BlockAccessor accessor) {
		if (!shouldRequestData(accessor)) {
			return null;
		}
		// 1.12.2: TileEntityLockableLoot implements ILootContainer and carries the
		// loot-table seed. Other loot containers (minecart entities) are not
		// reachable through BlockAccessor.getBlockEntity().
		if (accessor.getBlockEntity() instanceof TileEntityLockableLoot blockEntity && blockEntity.getLootTable() != null) {
			long seed = 0;
			if (LOOT_TABLE_SEED_FIELD != null) {
				try {
					seed = LOOT_TABLE_SEED_FIELD.getLong(blockEntity);
				} catch (IllegalAccessException e) {
					seed = 0;
				}
			}
			return Pair.of(blockEntity.getLootTable(), seed);
		}
		return null;
	}

	@Override
	public DataCodec<Pair<ResourceLocation, Long>> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Pair<ResourceLocation, Long> decode(PacketBuffer buf) {
				NBTTagCompound tag = DataCodec.readTag(buf);
				return Pair.of(new ResourceLocation(tag.getString("LootTable")), tag.getLong("LootTableSeed"));
			}

			@Override
			public void encode(PacketBuffer buf, Pair<ResourceLocation, Long> value) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("LootTable", value.getFirst().toString());
				tag.setLong("LootTableSeed", value.getSecond());
				buf.writeCompoundTag(tag);
			}
		};
	}

	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		// 1.12.2: permission level 2 = gamemaster, matching modern COMMANDS_GAMEMASTER.
		return accessor.getPlayer().canUseCommand(2, "");
	}

	public static class Client extends BlockLootTableProvider implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Pair<ResourceLocation, Long> pair = decodeFromData(accessor).orElse(null);
			if (pair != null) {
				tooltip.add(new TextComponentTranslation("jade.lootTable", pair.getFirst().toString()));
				tooltip.add(new TextComponentTranslation("jade.lootTableSeed", pair.getSecond()));
			}
		}

		@Override
		public boolean enabledByDefault() {
			return false;
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.DEBUG_LOOT_TABLE;
	}
}
