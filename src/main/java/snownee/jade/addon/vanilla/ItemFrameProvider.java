package snownee.jade.addon.vanilla;

import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IDisplayHelper;

public class ItemFrameProvider implements IEntityComponentProvider {
	public static final ItemFrameProvider INSTANCE = new ItemFrameProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		EntityItemFrame itemFrame = (EntityItemFrame) accessor.getEntity();
		// 1.12.2: ItemFrame.getItem() is named getDisplayedItem()
		ItemStack stack = itemFrame.getDisplayedItem();
		if (!stack.isEmpty()) {
			// 1.12.2: ItemStack.getDisplayName() returns a String, wrap it for stripColor
			tooltip.add(IDisplayHelper.get().stripColor(new TextComponentString(stack.getDisplayName())));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ITEM_FRAME;
	}
}
