package snownee.jade.addon.vanilla;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.init.Blocks;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class RedstoneProvider implements StreamServerDataProvider<BlockAccessor, Integer> {
	public static final RedstoneProvider INSTANCE = new RedstoneProvider();

	@Override
	public @Nullable Integer streamData(BlockAccessor accessor) {
		TileEntity blockEntity = accessor.getBlockEntity();
		if (blockEntity instanceof TileEntityComparator) {
			return ((TileEntityComparator) blockEntity).getOutputSignal();
		}
		// 1.12.2: calibrated sculk sensors do not exist.
		return null;
	}

	@Override
	public DataCodec<Integer> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Integer decode(PacketBuffer buf) {
				return buf.readVarInt();
			}

			@Override
			public void encode(PacketBuffer buf, Integer value) {
				buf.writeVarInt(value);
			}
		};
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_REDSTONE;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Block block = accessor.getBlockState().getBlock();
			IThemeHelper t = IThemeHelper.get();
			if (block instanceof BlockLever) {
				ITextComponent info;
				if (accessor.getBlockState().getValue(BlockLever.POWERED)) {
					info = t.success((ITextComponent) new TextComponentTranslation("tooltip.jade.state_on"));
				} else {
					info = t.danger((ITextComponent) new TextComponentTranslation("tooltip.jade.state_off"));
				}
				tooltip.add((ITextComponent) new TextComponentTranslation("tooltip.jade.state", info));
				return;
			}

			// 1.12.2: powered and unpowered repeaters are separate blocks.
			if (block == Blocks.POWERED_REPEATER || block == Blocks.UNPOWERED_REPEATER) {
				int delay = accessor.getBlockState().getValue(BlockRedstoneRepeater.DELAY);
				tooltip.add((ITextComponent) new TextComponentTranslation("tooltip.jade.delay", t.info(delay)));
				return;
			}

			Optional<Integer> signal = RedstoneProvider.INSTANCE.decodeFromData(accessor);
			// 1.12.2: powered and unpowered comparators are separate blocks.
			if (block == Blocks.POWERED_COMPARATOR || block == Blocks.UNPOWERED_COMPARATOR) {
				BlockRedstoneComparator.Mode mode = accessor.getBlockState().getValue(BlockRedstoneComparator.MODE);
				ITextComponent modeInfo = t.info(new TextComponentTranslation(
						"tooltip.jade.mode_" + (mode == BlockRedstoneComparator.Mode.COMPARE ? "comparator" : "subtractor")));
				tooltip.add(new TextComponentTranslation("tooltip.jade.mode", modeInfo));
				signal.ifPresent(value -> tooltip.add(new TextComponentTranslation("tooltip.jade.power", t.info(value))));
				return;
			}

			if (block instanceof BlockRedstoneWire) {
				int power = accessor.getBlockState().getValue(BlockRedstoneWire.POWER);
				tooltip.add(new TextComponentTranslation("tooltip.jade.power", t.info(power)));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_REDSTONE;
		}
	}
}
