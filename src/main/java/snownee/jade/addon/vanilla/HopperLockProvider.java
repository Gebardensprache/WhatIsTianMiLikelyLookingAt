package snownee.jade.addon.vanilla;

import net.minecraft.block.BlockHopper;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import snownee.jade.addon.access.AccessibilityPlugin;
import snownee.jade.addon.core.ObjectNameProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;

public class HopperLockProvider implements StreamServerDataProvider<BlockAccessor, Boolean> {
	public static final HopperLockProvider INSTANCE = new HopperLockProvider();

	@Override
	public Boolean streamData(BlockAccessor accessor) {
		return !accessor.getBlockState().getValue(BlockHopper.ENABLED);
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
		return JadeIds.MC_HOPPER_LOCK;
	}

	public static class Client implements IBlockComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			if (!HopperLockProvider.INSTANCE.decodeFromData(accessor).orElse(false)) {
				return;
			}
			if (config.get(JadeIds.MC_REDSTONE) ||
					(IWailaConfig.get().accessibility().getEnableAccessibilityPlugin() && config.get(JadeIds.ACCESS_BLOCK_DETAILS))) {
				String objectName = tooltip.getString(JadeIds.CORE_OBJECT_NAME);
				AccessibilityPlugin.replaceTitle(tooltip, objectName, "block.locked");
			}
		}

		@Override
		public boolean isRequired() {
			return true;
		}

		@Override
		public int getDefaultPriority() {
			return ObjectNameProvider.ForBlock.INSTANCE.getDefaultPriority() + 10;
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_HOPPER_LOCK;
		}
	}
}
