package snownee.jade.addon.access;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.addon.core.BlockFaceProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

public class BlockDetailsBodyProvider implements IBlockComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		IBlockState blockState = accessor.getBlockState();
		Block block = blockState.getBlock();
		if (block instanceof BlockRedstoneWire) {
			List<ITextComponent> list = Lists.newArrayListWithExpectedSize(4);
			// 1.12.2: BlockRedstoneWire has separate NORTH/EAST/SOUTH/WEST properties
			// instead of the modern PROPERTY_BY_DIRECTION map.
			addIfNotNone(list, blockState, EnumFacing.NORTH, BlockRedstoneWire.NORTH);
			addIfNotNone(list, blockState, EnumFacing.EAST, BlockRedstoneWire.EAST);
			addIfNotNone(list, blockState, EnumFacing.SOUTH, BlockRedstoneWire.SOUTH);
			addIfNotNone(list, blockState, EnumFacing.WEST, BlockRedstoneWire.WEST);
			if (list.isEmpty()) {
				tooltip.add(new TextComponentTranslation("jade.access.block.redstone_wire.dot"));
			} else {
				tooltip.add(new TextComponentTranslation(
						"jade.access.block.redstone_wire",
						join(list)));
			}
			return;
		}
		// The modern early-return branch for FlowerBedBlock / CampfireBlock / DecoratedPotBlock /
		// dripleaf blocks is dropped -- none of those blocks exist in 1.12.2.

		BlockRailBase.EnumRailDirection railShape = null;
		// 1.12.2: there is no shared BaseRailBlock/BlockRailBase#SHAPE property -- each rail
		// subclass declares its own "shape" property instance -- and no RAIL_SHAPE_STRAIGHT
		// variant (that property came with 1.16+ rails).
		if (blockState.getPropertyKeys().contains(BlockRail.SHAPE)) {
			railShape = blockState.getValue(BlockRail.SHAPE);
		} else if (blockState.getPropertyKeys().contains(BlockRailPowered.SHAPE)) {
			railShape = blockState.getValue(BlockRailPowered.SHAPE);
		} else if (blockState.getPropertyKeys().contains(BlockRailDetector.SHAPE)) {
			railShape = blockState.getValue(BlockRailDetector.SHAPE);
		}
		if (railShape != null) {
			tooltip.add(new TextComponentTranslation("jade.access.block.rail." + railShape.getName()));
		}

		EnumFacing facing = null;
		// 1.12.2: BlockDirectional.FACING also covers end rods, pistons, dispensers and
		// droppers (they share the same property instance); BlockHorizontal.FACING covers
		// furnaces, chests and similar; BlockHopper.FACING is a distinct instance.
		if (blockState.getPropertyKeys().contains(BlockDirectional.FACING)) {
			facing = (EnumFacing) blockState.getValue(BlockDirectional.FACING);
		} else if (blockState.getPropertyKeys().contains(BlockHorizontal.FACING)) {
			facing = (EnumFacing) blockState.getValue(BlockHorizontal.FACING);
		} else if (blockState.getPropertyKeys().contains(BlockHopper.FACING)) {
			facing = (EnumFacing) blockState.getValue(BlockHopper.FACING);
		}
		if (facing != null) {
			tooltip.add(new TextComponentTranslation("jade.access.block.facing", BlockFaceProvider.directionName(facing)));
		}
	}

	private static void addIfNotNone(List<ITextComponent> list, IBlockState blockState, EnumFacing facing, IProperty<?> property) {
		// 1.12.2: BlockRedstoneWire.EnumAttachPosition is package-private, so the values
		// are read through the public IStringSerializable interface instead.
		Object side = blockState.getValue(property);
		if (side instanceof IStringSerializable) {
			if (!"none".equals(((IStringSerializable) side).getName())) {
				list.add(BlockFaceProvider.directionName(facing));
			}
		} else if (!"none".equals(String.valueOf(side))) {
			list.add(BlockFaceProvider.directionName(facing));
		}
	}

	/**
	 * 1.12.2: ComponentUtils.formatList does not exist -- the wire directions are joined
	 * with a ", " literal separator instead of the modern DEFAULT_NO_STYLE_SEPARATOR.
	 */
	private static ITextComponent join(List<ITextComponent> list) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(list.get(i).getUnformattedText());
		}
		return new TextComponentString(builder.toString());
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_BLOCK_DETAILS_BODY;
	}

	@Override
	public boolean isRequired() {
		return true;
	}
}
