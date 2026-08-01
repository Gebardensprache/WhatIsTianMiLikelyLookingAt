package snownee.jade.api.harvest;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.collect.Lists;

import net.minecraft.util.ResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;

/**
 * Default implementation of {@link ToolTier}.
 */
public class SimpleToolTier implements ToolTier {
	private final ResourceLocation uid;
	private final ToolResult success;
	private final List<Block> extraBlocks = Lists.newArrayListWithExpectedSize(0);
	private final Predicate<IBlockState> predicate;

	/**
	 * Creates a simple tool tier.
	 *
	 * @param uid tier identifier
	 * @param tool display stack
	 * @param predicate matching predicate
	 */
	public SimpleToolTier(ResourceLocation uid, ItemStack tool, Predicate<IBlockState> predicate) {
		Objects.requireNonNull(uid);
		Objects.requireNonNull(tool);
		Objects.requireNonNull(predicate);
		this.uid = uid;
		this.success = ToolResult.of(tool);
		this.predicate = predicate;
	}

	@Override
	public ResourceLocation getUid() {
		return uid;
	}

	@Override
	public ToolResult isCorrectTool(IBlockState state) {
		return extraBlocks.contains(state.getBlock()) || predicate.test(state) ? success : ToolResult.fail();
	}

	@Override
	public ToolTier addExtraBlocks(Collection<? extends Block> blocks) {
		extraBlocks.addAll(blocks);
		return this;
	}

	@Override
	public ToolTier replaceExtraBlocks(Collection<? extends Block> blocks) {
		extraBlocks.clear();
		extraBlocks.addAll(blocks);
		return this;
	}

	/**
	 * Returns a predicate that uses the tool's native mining rules.
	 *
	 * @param toolItem tool stack
	 * @return matching predicate
	 */
	public static Predicate<IBlockState> isEffectiveTool(ItemStack toolItem) {
		return state -> toolItem.canHarvestBlock(state) || toolItem.getDestroySpeed(state) > 1.0F;
	}
}
