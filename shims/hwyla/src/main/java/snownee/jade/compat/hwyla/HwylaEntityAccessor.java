package snownee.jade.compat.hwyla;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Adapts Jade's {@link snownee.jade.api.EntityAccessor} to the HWYLA {@link mcp.mobius.waila.api.IWailaEntityAccessor}.
 */
public class HwylaEntityAccessor implements mcp.mobius.waila.api.IWailaEntityAccessor {

	private final snownee.jade.api.EntityAccessor accessor;

	public HwylaEntityAccessor(snownee.jade.api.EntityAccessor accessor) {
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
	public Entity getEntity() {
		return accessor.getEntity();
	}

	@Override
	public RayTraceResult getMOP() {
		return accessor.getHitResult();
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
}
