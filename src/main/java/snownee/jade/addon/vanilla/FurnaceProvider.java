package snownee.jade.addon.vanilla;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.LayoutSettings;

public class FurnaceProvider implements StreamServerDataProvider<BlockAccessor, FurnaceProvider.Data> {
	public static final FurnaceProvider INSTANCE = new FurnaceProvider();

	@Override
	public Data streamData(BlockAccessor accessor) {
		TileEntityFurnace furnace = accessor.typedBlockEntity();
		return new Data(
				furnace.getField(2),
				furnace.getField(3),
				Arrays.asList(furnace.getStackInSlot(0), furnace.getStackInSlot(1), furnace.getStackInSlot(2)));
	}

	@Override
	public DataCodec<Data> streamCodec() {
		return Data.STREAM_CODEC;
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_FURNACE;
	}

	public record Data(int progress, int total, List<ItemStack> inventory) {
		public static final DataCodec<Data> STREAM_CODEC = new DataCodec<>() {
			@Override
			public Data decode(PacketBuffer buf) {
				NBTTagCompound tag = DataCodec.readTag(buf);
				int progress = tag.getInteger("Progress");
				int total = tag.getInteger("Total");
				List<ItemStack> inventory = new ArrayList<>(3);
				for (int i = 0; i < 3; i++) {
					inventory.add(new ItemStack(tag.getCompoundTag("Slot" + i)));
				}
				return new Data(progress, total, inventory);
			}

			@Override
			public void encode(PacketBuffer buf, Data value) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("Progress", value.progress());
				tag.setInteger("Total", value.total());
				for (int i = 0; i < value.inventory().size(); i++) {
					tag.setTag("Slot" + i, value.inventory().get(i).writeToNBT(new NBTTagCompound()));
				}
				buf.writeCompoundTag(tag);
			}
		};
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Data data = FurnaceProvider.INSTANCE.decodeFromData(accessor).orElse(null);
			if (data == null || data.inventory().stream().allMatch(ItemStack::isEmpty)) {
				return;
			}
			tooltip.add(JadeUI.item(data.inventory().get(0)).alignSelfCenter());
			tooltip.append(JadeUI.item(data.inventory().get(1)).alignSelfCenter());
			tooltip.append(JadeUI.progressArrow(data.total() == 0 ? 0 : (float) data.progress() / data.total()).alignSelfCenter().settings($ -> {
				return ((LayoutSettings) $).paddingHorizontal(3);
			}));
			tooltip.append(JadeUI.item(data.inventory().get(2)).alignSelfCenter());
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_FURNACE;
		}
	}
}
