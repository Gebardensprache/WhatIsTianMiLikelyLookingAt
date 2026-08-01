package snownee.jade.addon.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.EntityLiving;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.storage.loot.ILootContainer;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

public class EntityLootTableProvider implements StreamServerDataProvider<EntityAccessor, ResourceLocation> {
	public static final EntityLootTableProvider INSTANCE = new EntityLootTableProvider();
	private static final @Nullable Field LOOT_TABLE_FIELD;
	private static final @Nullable Method LOOT_TABLE_METHOD;

	static {
		Field field;
		try {
			// 1.12.2: EntityLiving.deathLootTable is private; the modern getLootTable()
			// accessor does not exist. MCP name works in the dev runtime; in a
			// production (SRG) runtime the lookup fails and the feature degrades to
			// no output, matching the MobSpawnerCooldownProvider field-access fallback.
			field = EntityLiving.class.getDeclaredField("deathLootTable");
			field.setAccessible(true);
		} catch (Exception e) {
			field = null;
		}
		LOOT_TABLE_FIELD = field;
		Method method;
		try {
			// 1.12.2: the effective loot table falls back to the protected
			// EntityLiving.getLootTable() entity-type default when deathLootTable is
			// unset, mirroring dropLoot().
			method = EntityLiving.class.getDeclaredMethod("getLootTable");
			method.setAccessible(true);
		} catch (Exception e) {
			method = null;
		}
		LOOT_TABLE_METHOD = method;
	}

	@Override
	public @Nullable ResourceLocation streamData(EntityAccessor accessor) {
		if (!shouldRequestData(accessor)) {
			return null;
		}
		// 1.12.2: loot containers (e.g. minecart chests) expose the loot table
		// publicly through ILootContainer; EntityLiving keeps it in private fields.
		ResourceLocation lootTable = null;
		if (accessor.getEntity() instanceof ILootContainer container) {
			lootTable = container.getLootTable();
		}
		if (lootTable == null && accessor.getEntity() instanceof EntityLiving) {
			if (LOOT_TABLE_FIELD != null) {
				try {
					lootTable = (ResourceLocation) LOOT_TABLE_FIELD.get(accessor.getEntity());
				} catch (IllegalAccessException | ClassCastException e) {
					lootTable = null;
				}
			}
			if (lootTable == null && LOOT_TABLE_METHOD != null) {
				try {
					lootTable = (ResourceLocation) LOOT_TABLE_METHOD.invoke(accessor.getEntity());
				} catch (ReflectiveOperationException | ClassCastException e) {
					lootTable = null;
				}
			}
		}
		return lootTable;
	}

	@Override
	public DataCodec<ResourceLocation> streamCodec() {
		return new DataCodec<>() {
			@Override
			public ResourceLocation decode(PacketBuffer buf) {
				NBTTagCompound tag = DataCodec.readTag(buf);
				return new ResourceLocation(tag.getString("LootTable"));
			}

			@Override
			public void encode(PacketBuffer buf, ResourceLocation value) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setString("LootTable", value.toString());
				buf.writeCompoundTag(tag);
			}
		};
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		// 1.12.2: permission level 2 = gamemaster, matching modern COMMANDS_GAMEMASTER.
		return accessor.getPlayer().canUseCommand(2, "");
	}

	public static class Client extends EntityLootTableProvider implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			//noinspection SimplifyOptionalCallChains
			ResourceLocation lootTable = decodeFromData(accessor).orElse(null);
			if (lootTable != null) {
				tooltip.add(new TextComponentTranslation("jade.lootTable", lootTable.toString()));
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
