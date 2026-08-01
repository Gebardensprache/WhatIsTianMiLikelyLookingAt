package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityMinecartMobSpawner;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import snownee.jade.addon.core.ObjectNameProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IToggleableProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public abstract class MobSpawnerProvider implements IToggleableProvider {

	public static class ForBlock extends MobSpawnerProvider implements IBlockComponentProvider {
		public static final ForBlock INSTANCE = new ForBlock();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			TileEntity blockEntity = accessor.getBlockEntity();
			if (blockEntity instanceof TileEntityMobSpawner) {
				Entity displayEntity = getDisplayEntity(((TileEntityMobSpawner) blockEntity).getSpawnerBaseLogic());
				appendTooltip(tooltip, displayEntity, (ITextComponent) new TextComponentTranslation(accessor.getBlock().getTranslationKey() + ".name"));
			}
		}
	}

	public static class ForEntity extends MobSpawnerProvider implements IEntityComponentProvider {
		public static final ForEntity INSTANCE = new ForEntity();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			EntityMinecartMobSpawner spawner = (EntityMinecartMobSpawner) accessor.getEntity();
			// 1.12.2: EntityMinecartMobSpawner exposes neither its MobSpawnerBaseLogic nor a display
			// entity. Its serialized SpawnData is public through writeToNBT, so construct the client-only
			// display entity from that data without an access transformer.
			Entity displayEntity = getDisplayEntity(spawner.writeToNBT(new NBTTagCompound()), accessor.getLevel());
			appendTooltip(tooltip, displayEntity, ObjectNameProvider.getEntityName(spawner, false).createCopy());
		}
	}

	@Nullable
	private static Entity getDisplayEntity(MobSpawnerBaseLogic spawner) {
		// 1.12.2: MobSpawnerBaseLogic#getEntityId is private; getCachedEntity is its public client-side
		// replacement and preserves vanilla's selected SpawnData entity.
		return spawner.getCachedEntity();
	}

	@Nullable
	private static Entity getDisplayEntity(NBTTagCompound spawnerData, World world) {
		if (!spawnerData.hasKey("SpawnData", 10)) {
			return null;
		}
		// 1.12.2: AnvilChunkLoader is the legacy non-spawning NBT entity factory used by the spawner itself.
		return AnvilChunkLoader.readWorldEntity(spawnerData.getCompoundTag("SpawnData"), world, false);
	}

	public static void appendTooltip(ITooltip tooltip, @Nullable Entity displayEntity, ITextComponent name) {
		if (displayEntity == null) {
			return;
		}
		ITextComponent title = (ITextComponent) new TextComponentTranslation("jade.spawner", name, displayEntity.getDisplayName());
		tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(title));
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_MOB_SPAWNER;
	}

	@Override
	public int getDefaultPriority() {
		return ObjectNameProvider.ForEntity.INSTANCE.getDefaultPriority() + 10;
	}

}
