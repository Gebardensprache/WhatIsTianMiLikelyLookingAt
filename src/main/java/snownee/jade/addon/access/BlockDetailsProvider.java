package snownee.jade.addon.access;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import snownee.jade.addon.core.ObjectNameProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

public class BlockDetailsProvider implements IBlockComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		IBlockState blockState = accessor.getBlockState();
		Block block = blockState.getBlock();
		String objectName = tooltip.getString(JadeIds.CORE_OBJECT_NAME);
		if (blockState.getPropertyKeys().contains(BlockDoor.OPEN)) {
			// 1.12.2: BarrelBlock does not exist, so the modern exclusion of barrels from the
			// door branch is irrelevant -- all blocks carrying an "open" property are doors,
			// trapdoors or fence gates, all of which read it the same way.
			AccessibilityPlugin.replaceTitle(
					tooltip,
					objectName,
					"block.door_" + (blockState.getValue(BlockDoor.OPEN) ? "open" : "closed"));
		}
		// WATERLOGGED, LIT, OMINOUS, CAN_SUMMON, VAULT_STATE, TRIAL_SPAWNER_STATE and
		// CREAKING_HEART_STATE are all 1.13+/1.19+ properties with no 1.12.2 equivalents --
		// dropped. "INVERTED" exists only on the redstone comparator in 1.12.2, and the
		// modern "INVERTED" branch only triggers for the daylight detector (its modern
		// INVERTED property), which does not exist here either -- the comparator equivalent
		// is covered by its POWERED branch below.
		if (blockState.getPropertyKeys().contains(BlockEndPortalFrame.EYE) && blockState.getValue(BlockEndPortalFrame.EYE)) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.eye");
		}
		if (blockState.getPropertyKeys().contains(BlockFarmland.MOISTURE) && blockState.getValue(BlockFarmland.MOISTURE) == 7) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.hydrated");
		}
		if (blockState.getPropertyKeys().contains(BlockRedstoneRepeater.LOCKED) && blockState.getValue(BlockRedstoneRepeater.LOCKED)) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.locked");
		}
		if (blockState.getPropertyKeys().contains(BlockPistonBase.EXTENDED) && blockState.getValue(BlockPistonBase.EXTENDED)) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.extended");
		}
		// 1.12.2: turtle eggs do not exist, so the modern HATCH branch is dropped.
		if (blockState.getBlock() instanceof BlockStairs && blockState.getValue(BlockStairs.HALF) == BlockStairs.EnumHalf.TOP) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.upside_down");
		}
		// 1.12.2: ShelfBlock does not exist; the modern RepeaterBlock/BaseRailBlock/ShelfBlock
		// branch reduces to its BaseRailBlock intent (rail, powered rail, detector rail).
		// The property-name check alone cannot stand: BlockStateContainer maps properties by
		// structural equality (valueClass + name), so levers, buttons, pressure plates and
		// tripwire also declare a PropertyBool "powered" and would be mislabeled.
		if (blockState.getBlock() instanceof BlockRailBase && blockState.getPropertyKeys().contains(BlockRailPowered.POWERED) && blockState.getValue(BlockRailPowered.POWERED)) {
			AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.powered");
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_BLOCK_DETAILS;
	}

	@Override
	public int getDefaultPriority() {
		return ObjectNameProvider.ForBlock.INSTANCE.getDefaultPriority() + 10;
	}
}
