package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;

public class CommandBlockProvider implements StreamServerDataProvider<BlockAccessor, String> {
	public static final CommandBlockProvider INSTANCE = new CommandBlockProvider();

	@Override
	@Nullable
	public String streamData(BlockAccessor accessor) {
		if (!accessor.getPlayer().canUseCommandBlock()) {
			return null;
		}
		String command = accessor.<TileEntityCommandBlock>typedBlockEntity().getCommandBlockLogic().getCommand();
		if (command.length() > 40) {
			command = command.substring(0, 37) + "...";
		}
		return command;
	}

	@Override
	public DataCodec<String> streamCodec() {
		return new DataCodec<>() {
			@Override
			public String decode(PacketBuffer buf) {
				return buf.readString(32767);
			}

			@Override
			public void encode(PacketBuffer buf, String value) {
				buf.writeString(value);
			}
		};
	}

	@Override
	public boolean shouldRequestData(BlockAccessor accessor) {
		return accessor.getPlayer().canUseCommandBlock();
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_COMMAND_BLOCK;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			String command = CommandBlockProvider.INSTANCE.decodeFromData(accessor).orElse("");
			if (command.trim().isEmpty()) {
				return;
			}
			tooltip.add(new TextComponentString("> " + command));
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_COMMAND_BLOCK;
		}
	}
}
