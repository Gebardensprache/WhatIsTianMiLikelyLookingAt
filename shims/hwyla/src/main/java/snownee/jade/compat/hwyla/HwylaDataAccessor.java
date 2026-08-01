package snownee.jade.compat.hwyla;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Adapts Jade's {@link snownee.jade.api.BlockAccessor} to the HWYLA {@link mcp.mobius.waila.api.IWailaDataAccessor}.
 */
public class HwylaDataAccessor implements mcp.mobius.waila.api.IWailaDataAccessor {

	private final snownee.jade.api.BlockAccessor accessor;

	public HwylaDataAccessor(snownee.jade.api.BlockAccessor accessor) {
		this.accessor = accessor;
	}

	@Override
	public World getWorld() {
		return accessor.getLevel();
	}

	@Override
	public EntityPlayer getPlayer() {
		return accessor.getPlayer();
	}

	@Override
	public Block getBlock() {
		return accessor.getBlock();
	}

	@Override
	public int getMetadata() {
		return accessor.getBlockState().getBlock().getMetaFromState(accessor.getBlockState());
	}

	@Override
	public IBlockState getBlockState() {
		return accessor.getBlockState();
	}

	@Override
	public TileEntity getTileEntity() {
		return accessor.getBlockEntity();
	}

	@Override
	public RayTraceResult getMOP() {
		return accessor.getHitResult();
	}

	@Override
	public BlockPos getPosition() {
		return accessor.getPosition();
	}

	@Override
	public Vec3d getRenderingPosition() {
		RayTraceResult hit = accessor.getHitResult();
		return new Vec3d(hit.hitVec.x, hit.hitVec.y, hit.hitVec.z);
	}

	@Override
	public NBTTagCompound getNBTData() {
		return accessor.getServerData();
	}

	@Override
	public int getNBTInteger(NBTTagCompound tag, String keyname) {
		return tag.getInteger(keyname);
	}

	@Override
	public double getPartialFrame() {
		return 0;
	}

	@Override
	public EnumFacing getSide() {
		return accessor.getSide();
	}

	@Override
	public ItemStack getStack() {
		ItemStack picked = accessor.getPickedResult();
		return picked.isEmpty() ? ItemStack.EMPTY : picked;
	}
}
