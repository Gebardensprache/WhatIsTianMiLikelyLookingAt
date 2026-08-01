package snownee.jade.addon.harvest;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import snownee.jade.api.callback.CallbackContainer;
import snownee.jade.api.harvest.ToolResult;
import snownee.jade.api.harvest.ToolTier;
import snownee.jade.api.harvest.ToolTierAddedCallback;
import snownee.jade.api.harvest.ToolType;

public class SimpleToolType implements ToolType {

	private final ResourceLocation uid;
	protected final List<ToolTier> tiers = Lists.newArrayList();
	protected final boolean skipInstaBreakingBlock;
	private final CallbackContainer<ToolTierAddedCallback> callbacks = new CallbackContainer<>();

	protected SimpleToolType(ResourceLocation uid, boolean skipInstaBreakingBlock) {
		this.uid = uid;
		this.skipInstaBreakingBlock = skipInstaBreakingBlock;
	}

	public static ToolType of(ResourceLocation uid) {
		return of(uid, true);
	}

	public static ToolType of(ResourceLocation uid, boolean skipInstaBreakingBlock) {
		return new SimpleToolType(uid, skipInstaBreakingBlock);
	}

	@Override
	public ToolResult test(IBlockState state, World level, BlockPos pos) {
		// 1.12.2: there is no block-state correct-tool flag; zero hardness is the usable
		// approximation for filtering instant-break blocks.
		if (skipInstaBreakingBlock && state.getBlockHardness(level, pos) == 0) {
			return ToolResult.fail();
		}
		for (ToolTier tier : tiers) {
			ToolResult result = tier.isCorrectTool(state);
			if (result.isSuccess()) {
				return result;
			}
		}
		return ToolResult.fail();
	}

	@Override
	public List<ToolTier> tiers() {
		return tiers;
	}

	@Override
	public CallbackContainer<ToolTierAddedCallback> tierAddedCallbacks() {
		return callbacks;
	}

	@Override
	public ResourceLocation getUid() {
		return uid;
	}
}
