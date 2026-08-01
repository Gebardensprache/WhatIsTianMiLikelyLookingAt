package snownee.jade.addon.vanilla;

import java.util.Optional;

import net.minecraft.block.BlockJukebox;
import net.minecraft.item.ItemRecord;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;

public class JukeboxProvider implements StreamServerDataProvider<BlockAccessor, ItemStack> {
	public static final JukeboxProvider INSTANCE = new JukeboxProvider();

	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		return accessor.getBlockState().getValue(BlockJukebox.HAS_RECORD);
	}

	@Override
	public ItemStack streamData(BlockAccessor accessor) {
		return accessor.<BlockJukebox.TileEntityJukebox>typedBlockEntity().getRecord();
	}

	@Override
	public DataCodec<ItemStack> streamCodec() {
		return new DataCodec<>() {
			@Override
			public ItemStack decode(PacketBuffer buf) {
				return DataCodec.readStack(buf);
			}

			@Override
			public void encode(PacketBuffer buf, ItemStack value) {
				buf.writeItemStack(value);
			}
		};
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_JUKEBOX;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			Optional<ItemStack> result = JukeboxProvider.INSTANCE.decodeFromData(accessor);
			if (!result.isPresent()) {
				return;
			}
			ItemStack stack = result.get();
			if (stack.isEmpty()) {
				tooltip.add((ITextComponent) new TextComponentTranslation("tooltip.jade.empty"));
				return;
			}
			ITextComponent name;
			if (stack.getItem() instanceof ItemRecord) {
				name = (ITextComponent) new TextComponentString(((ItemRecord) stack.getItem()).getRecordNameLocal());
			} else {
				name = stack.getTextComponent();
			}
			tooltip.add((ITextComponent) new TextComponentTranslation("record.nowPlaying", IDisplayHelper.get().stripColor(name)));
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_JUKEBOX;
		}
	}
}
