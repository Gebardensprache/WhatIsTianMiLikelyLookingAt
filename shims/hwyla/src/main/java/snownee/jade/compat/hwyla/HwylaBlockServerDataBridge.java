package snownee.jade.compat.hwyla;

import net.minecraft.nbt.NBTTagCompound;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Bridges an HWYLA {@link mcp.mobius.waila.api.IWailaDataProvider NBT method} to a Jade {@link IServerDataProvider}.
 * <p>
 * Calls {@code getNBTData(player, te, tag, world, pos)} on the wrapped HWYLA provider and merges
 * the returned tag into the accessor's server data.
 */
public class HwylaBlockServerDataBridge implements IServerDataProvider<BlockAccessor> {

	private final mcp.mobius.waila.api.IWailaDataProvider hwylaProvider;
	private final net.minecraft.util.ResourceLocation uid;

	public HwylaBlockServerDataBridge(mcp.mobius.waila.api.IWailaDataProvider hwylaProvider,
			net.minecraft.util.ResourceLocation uid) {
		this.hwylaProvider = hwylaProvider;
		this.uid = uid;
	}

	@Override
	public void appendServerData(NBTTagCompound data, BlockAccessor accessor) {
		NBTTagCompound tag = hwylaProvider.getNBTData(
				(net.minecraft.entity.player.EntityPlayerMP) accessor.getPlayer(),
				accessor.getBlockEntity(),
				data,
				accessor.getLevel(),
				accessor.getPosition());
		if (tag != null && tag != data) {
			data.merge(tag);
		}
	}

	@Override
	public net.minecraft.util.ResourceLocation getUid() {
		return uid;
	}

	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		return true;
	}
}
