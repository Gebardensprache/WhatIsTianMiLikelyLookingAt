package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

public class FallingBlockProvider implements IEntityComponentProvider {
	public static final FallingBlockProvider INSTANCE = new FallingBlockProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		// No additional tooltip for falling blocks
	}

	@Override
	public @Nullable Element getIcon(EntityAccessor accessor, IPluginConfig config, @Nullable Element currentIcon) {
		EntityFallingBlock entity = (EntityFallingBlock) accessor.getEntity();
		// 1.12.2: EntityFallingBlock.getBlock() is the IBlockState accessor
		ItemStack stack = new ItemStack(entity.getBlock().getBlock());
		if (stack.isEmpty()) {
			return currentIcon;
		}
		return JadeUI.item(stack);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_FALLING_BLOCK;
	}

	@Override
	public boolean isRequired() {
		return true;
	}

}
