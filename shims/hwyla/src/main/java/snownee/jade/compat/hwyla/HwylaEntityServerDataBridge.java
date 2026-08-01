package snownee.jade.compat.hwyla;

import net.minecraft.nbt.NBTTagCompound;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Bridges an HWYLA {@link mcp.mobius.waila.api.IWailaEntityProvider NBT method} to a Jade {@link IServerDataProvider}.
 * <p>
 * Calls {@code getNBTData(player, ent, tag, world)} on the wrapped HWYLA provider and merges
 * the returned tag into the accessor's server data.
 */
public class HwylaEntityServerDataBridge implements IServerDataProvider<EntityAccessor> {

	private final mcp.mobius.waila.api.IWailaEntityProvider hwylaProvider;
	private final net.minecraft.util.ResourceLocation uid;

	public HwylaEntityServerDataBridge(mcp.mobius.waila.api.IWailaEntityProvider hwylaProvider,
			net.minecraft.util.ResourceLocation uid) {
		this.hwylaProvider = hwylaProvider;
		this.uid = uid;
	}

	@Override
	public void appendServerData(NBTTagCompound data, EntityAccessor accessor) {
		NBTTagCompound tag = hwylaProvider.getNBTData(
				(net.minecraft.entity.player.EntityPlayerMP) accessor.getPlayer(),
				accessor.getEntity(),
				data,
				accessor.getLevel());
		if (tag != null && tag != data) {
			data.merge(tag);
		}
	}

	@Override
	public net.minecraft.util.ResourceLocation getUid() {
		return uid;
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		return true;
	}
}
