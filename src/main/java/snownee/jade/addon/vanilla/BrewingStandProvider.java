package snownee.jade.addon.vanilla;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.JadeUI;

public class BrewingStandProvider implements StreamServerDataProvider<BlockAccessor, BrewingStandProvider.Data> {
	public static final BrewingStandProvider INSTANCE = new BrewingStandProvider();

	@Override
	public Data streamData(BlockAccessor accessor) {
		TileEntityBrewingStand brewingStand = accessor.typedBlockEntity();
		return new Data(brewingStand.getField(1), brewingStand.getField(0));
	}

	@Override
	public DataCodec<Data> streamCodec() {
		return Data.STREAM_CODEC;
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_BREWING_STAND;
	}

	public record Data(int fuel, int time) {
		public static final DataCodec<Data> STREAM_CODEC = new DataCodec<>() {
			@Override
			public Data decode(PacketBuffer buf) {
				NBTTagCompound tag = DataCodec.readTag(buf);
				return new Data(tag.getInteger("Fuel"), tag.getInteger("Time"));
			}

			@Override
			public void encode(PacketBuffer buf, Data value) {
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("Fuel", value.fuel());
				tag.setInteger("Time", value.time());
				buf.writeCompoundTag(tag);
			}
		};
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Data data = BrewingStandProvider.INSTANCE.decodeFromData(accessor).orElse(null);
			if (data == null) {
				return;
			}
			tooltip.add(JadeUI.smallItem(new ItemStack(Items.BLAZE_POWDER)).narration(""));
			tooltip.append(JadeUI.text(IThemeHelper.get().info(data.fuel()))
					.narration(new TextComponentTranslation("narration.jade.brewingStand.fuel", data.fuel())));
			if (data.time() > 0) {
				tooltip.append(JadeUI.spacer(5, 0));
				tooltip.append(JadeUI.smallItem(new ItemStack(Items.CLOCK)).narration(""));
				// 1.12.2: fixed 20-tick seconds formatting.
				tooltip.append(IThemeHelper.get().seconds(data.time(), accessor.tickRate()));
			}
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_BREWING_STAND;
		}
	}
}
