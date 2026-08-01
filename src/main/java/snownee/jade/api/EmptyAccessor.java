package snownee.jade.api;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.math.RayTraceResult;

/**
 * Accessor used when Jade needs context without a specific target.
 */
public interface EmptyAccessor extends Accessor<RayTraceResult> {

	@Override
	default Class<? extends Accessor<?>> getAccessorType() {
		return EmptyAccessor.class;
	}

	@NonExtendable
	interface Builder {
		Builder level(World level);

		Builder player(EntityPlayer player);

		Builder serverData(@Nullable NBTTagCompound serverData);

		Builder serverConnected(boolean connected);

		Builder showDetails(boolean showDetails);

		Builder hit(RayTraceResult hit);

		/**
		 * Copies values from another empty accessor.
		 *
		 * @param accessor source accessor
		 * @return this builder
		 */
		Builder from(EmptyAccessor accessor);

		default Builder requireVerification() {
			return requireVerification(true);
		}

		Builder requireVerification(boolean verify);

		EmptyAccessor build();
	}

}
