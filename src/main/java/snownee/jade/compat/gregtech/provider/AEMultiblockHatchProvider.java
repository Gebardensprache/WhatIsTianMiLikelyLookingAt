package snownee.jade.compat.gregtech.provider;

import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.common.metatileentities.multi.multiblockpart.appeng.MetaTileEntityAEHostablePart;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.text.TextComponentTranslation;

import org.jspecify.annotations.Nullable;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.compat.gregtech.GTIds;
import snownee.jade.mixin.gregtech.AccessorMTEAEHostablePart;

public class AEMultiblockHatchProvider implements StreamServerDataProvider<BlockAccessor, Boolean> {
	public static final AEMultiblockHatchProvider INSTANCE = new AEMultiblockHatchProvider();

	@Override
	public @Nullable Boolean streamData(BlockAccessor accessor) {
		MetaTileEntityHolder te = accessor.typedBlockEntity();
		if (te.getMetaTileEntity() instanceof MetaTileEntityAEHostablePart<?> aeHostablePart) {
			return ((AccessorMTEAEHostablePart) aeHostablePart).isOnline();
		}
		return null;
	}

	@Override
	public DataCodec<Boolean> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Boolean decode(PacketBuffer buf) {
				return buf.readBoolean();
			}

			@Override
			public void encode(PacketBuffer buf, Boolean value) {
				buf.writeBoolean(value);
			}
		};
	}

	@Override
	public ResourceLocation getUid() {
		return GTIds.GT_AE_PART;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Boolean isOnline = AEMultiblockHatchProvider.INSTANCE.decodeFromData(accessor).orElse(false);
			if (!isOnline) return;
			tooltip.add(new TextComponentTranslation("gregtech.gui.me_network.online"));
		}

		@Override
		public ResourceLocation getUid() {
			return GTIds.GT_AE_PART;
		}
	}
}
