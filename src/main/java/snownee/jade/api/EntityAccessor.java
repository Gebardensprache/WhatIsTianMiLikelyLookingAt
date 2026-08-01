package snownee.jade.api;

import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.util.math.RayTraceResult;

/**
 * Accessor describing the entity currently under the Jade crosshair.
 */
public interface EntityAccessor extends Accessor<RayTraceResult> {

	/**
	 * Returns the resolved entity.
	 *
	 * @return the target entity
	 */
	Entity getEntity();

	/**
	 * Returns the raw entity that was hit before any part-to-parent resolution.
	 *
	 * @return the raw target entity
	 */
	Entity getRawEntity();

	@Override
	default Class<? extends Accessor<?>> getAccessorType() {
		return EntityAccessor.class;
	}

	@NonExtendable
	interface Builder {
		Builder level(World level);

		Builder player(EntityPlayer player);

		Builder serverData(@Nullable NBTTagCompound serverData);

		Builder serverConnected(boolean connected);

		Builder showDetails(boolean showDetails);

		/**
		 * Sets the hit result supplier.
		 *
		 * @param hit supplier for the entity hit result
		 * @return this builder
		 */
		default Builder hit(RayTraceResult hit) {
			return hit(() -> hit);
		}

		/**
		 * Sets the hit result supplier.
		 *
		 * @param hit supplier for the entity hit result
		 * @return this builder
		 */
		Builder hit(Supplier<RayTraceResult> hit);

		/**
		 * Sets the entity supplier.
		 *
		 * @param entity supplier for the entity
		 * @return this builder
		 */
		default Builder entity(Entity entity) {
			return entity(() -> entity);
		}

		/**
		 * Sets the entity supplier.
		 *
		 * @param entity supplier for the entity
		 * @return this builder
		 */
		Builder entity(Supplier<Entity> entity);

		Builder from(EntityAccessor accessor);

		default Builder requireVerification() {
			return requireVerification(true);
		}

		Builder requireVerification(boolean verify);

		EntityAccessor build();
	}
}
