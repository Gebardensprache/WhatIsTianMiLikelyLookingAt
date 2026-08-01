package snownee.jade.compat.gregtech.provider;

import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import net.minecraft.nbt.NBTTagCompound;
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
import snownee.jade.api.ui.JadeUI;
import snownee.jade.compat.gregtech.GTIds;

public class ElectricContainerDataProvider implements StreamServerDataProvider<BlockAccessor, ElectricContainerDataProvider.Data> {
	public static final ElectricContainerDataProvider INSTANCE = new ElectricContainerDataProvider();

	@Override
	public @Nullable Data streamData(BlockAccessor accessor) {
		MetaTileEntityHolder holder = accessor.typedBlockEntity();
		if (holder.getMetaTileEntity().hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null)) {
			IEnergyContainer capability = holder.getMetaTileEntity().getCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null);
			return new Data(capability.getEnergyCapacity(), capability.getEnergyStored(), capability.isOneProbeHidden());
		}
		return null;
	}

	@Override
	public DataCodec<Data> streamCodec() {
		return Data.STREAM_CODEC;
	}

	@Override
	public ResourceLocation getUid() {
		return GTIds.GT_ENERGY_CONTAINER;
	}

	public record Data(long capacity, long stored, boolean isHudHidden) {
		public static final DataCodec<Data> STREAM_CODEC = new DataCodec<>() {
			@Override
			public Data decode(PacketBuffer buf) {
				NBTTagCompound tag = DataCodec.readTag(buf);
				long capacity = tag.getLong("Capacity");
				long stored = tag.getLong("Stored");
				boolean isHudHidden = tag.getBoolean("IsHudHidden");
				return new Data(capacity, stored, isHudHidden);
			}

			@Override
			public void encode(PacketBuffer buf, Data value) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setLong("Capacity", value.capacity);
				tag.setLong("Stored", value.stored);
				tag.setBoolean("IsHudHidden", value.isHudHidden);
				buf.writeCompoundTag(tag);
			}
		};
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!config.get(GTIds.GT_ENERGY_CONTAINER) || accessor.getBlockEntity() == null) return;

			if (accessor.typedBlockEntity() instanceof MetaTileEntityHolder holder && holder.getMetaTileEntity().hasCapability(GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER, null)) {
				Data data = ElectricContainerDataProvider.INSTANCE.decodeFromData(accessor).orElse(null);
				if (data != null && !data.isHudHidden) {
					tooltip.add(new TextComponentTranslation("gregtech.waila.energy_stored", data.stored, data.capacity));
					// TODO: convert to EnergyStorageProvider
				}
			}
		}

		@Override
		public ResourceLocation getUid() {
			return GTIds.GT_ENERGY_CONTAINER;
		}
	}

}
