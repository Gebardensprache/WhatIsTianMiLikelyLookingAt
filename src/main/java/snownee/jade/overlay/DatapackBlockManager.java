package snownee.jade.overlay;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import snownee.jade.api.Accessor;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.util.ClientProxy;

/**
 * 1.12.2: upstream tracks vanilla {@code Display.BlockDisplay}/{@code Display.ItemDisplay}
 * entities (a post-1.19.4 vanilla feature) so a datapack-camouflaged block/entity can borrow
 * that display entity's rendered item/block for its tooltip icon. Neither the {@code Display}
 * entity hierarchy nor datapack block/entity camouflage exist in 1.12.2, and there is no
 * equivalent mechanism to substitute. This class is therefore reduced to a permanent no-op:
 * every method keeps its upstream name and signature (needed by
 * {@link snownee.jade.impl.WailaClientRegistration#getBlockCamouflage(World, BlockPos)} and
 * {@link snownee.jade.util.ClientProxy}'s entity join/leave listeners) but the join/leave
 * tracking set is dropped, {@link #isAcceptableEntity(Entity)} always returns {@code false},
 * {@link #getFakeBlock(World, BlockPos)} always returns {@link ItemStack#EMPTY}, and
 * {@link #override(RayTraceResult, Accessor, Accessor)} returns the accessor unchanged.
 */
public class DatapackBlockManager {

	public static void onEntityJoin(Entity entity) {
		// 1.12.2: no display-entity tracking; intentional no-op.
	}

	public static void onEntityLeave(Entity entity) {
		// 1.12.2: no display-entity tracking; intentional no-op.
	}

	public static ItemStack getFakeBlock(World level, BlockPos pos) {
		return ItemStack.EMPTY;
	}

	@Nullable
	public static Accessor<?> override(RayTraceResult hitResult, @Nullable Accessor<?> accessor, @Nullable Accessor<?> originalAccessor) {
		return accessor;
	}

	public static boolean isAcceptableEntity(Entity entity) {
		return false;
	}

}
