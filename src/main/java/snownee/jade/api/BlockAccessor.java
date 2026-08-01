package snownee.jade.api;

import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.RayTraceResult;

/**
 * Accessor describing the block currently under the Jade crosshair.
 */
public interface BlockAccessor extends Accessor<RayTraceResult> {

	/**
	 * Returns the resolved block.
	 *
	 * @return the target block
	 */
	Block getBlock();

	/**
	 * Returns the target block state.
	 *
	 * @return the current block state
	 */
	IBlockState getBlockState();

	/**
	 * Returns the target block entity, if present.
	 *
	 * @return the block entity or {@code null}
	 */
	@Nullable
	TileEntity getBlockEntity();

	/**
	 * Returns the block entity cast to a specific subtype.
	 *
	 * @param <T> block entity subtype
	 * @return the block entity
	 * @throws NullPointerException if the target has no block entity
	 * @throws ClassCastException if the block entity is not of the requested type
	 */
	default <T extends TileEntity> T typedBlockEntity() {
		@SuppressWarnings("unchecked")
		T blockEntity = (T) getBlockEntity();
		return Objects.requireNonNull(blockEntity);
	}

	/**
	 * Returns the block position.
	 *
	 * @return the target position
	 */
	BlockPos getPosition();

	/**
	 * Returns the side that was hit.
	 *
	 * @return the hit face
	 */
	EnumFacing getSide();

	@Override
	default Class<? extends Accessor<?>> getAccessorType() {
		return BlockAccessor.class;
	}

	@NonExtendable
	interface Builder {
		Builder level(World level);

		Builder player(EntityPlayer player);

		Builder serverData(@Nullable NBTTagCompound serverData);

		Builder serverConnected(boolean connected);

		Builder showDetails(boolean showDetails);

		Builder hit(RayTraceResult hit);

		Builder blockState(IBlockState state);

		/**
		 * Sets the target block entity using a fixed instance.
		 *
		 * @param blockEntity the block entity or {@code null}
		 * @return this builder
		 */
		default Builder blockEntity(@Nullable TileEntity blockEntity) {
			return blockEntity(() -> blockEntity);
		}

		/**
		 * Sets the target block entity supplier.
		 *
		 * @param blockEntity supplier for the block entity
		 * @return this builder
		 */
		Builder blockEntity(Supplier<@Nullable TileEntity> blockEntity);

		Builder serversideRep(ItemStack stack);

		Builder from(BlockAccessor accessor);

		default Builder requireVerification() {
			return requireVerification(true);
		}

		Builder requireVerification(boolean verify);

		BlockAccessor build();
	}

}
