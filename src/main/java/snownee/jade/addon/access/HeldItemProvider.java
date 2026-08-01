package snownee.jade.addon.access;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

public class HeldItemProvider implements IEntityComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		EntityLivingBase entity = (EntityLivingBase) accessor.getEntity();
		// 1.12.2: ItemStack#isEmpty did not exist until 1.13 -- empty stacks are detected
		// via getItem() == Items.AIR (the only empty ItemStack instance is ItemStack.EMPTY,
		// which is what getHeldItem* returns for empty hands).
		if (entity.getHeldItemMainhand().getItem() != Items.AIR) {
			tooltip.add(new TextComponentTranslation("jade.access.held_item.main", entity.getHeldItemMainhand().getTextComponent()));
		}
		if (entity.getHeldItemOffhand().getItem() != Items.AIR) {
			tooltip.add(new TextComponentTranslation("jade.access.held_item.off", entity.getHeldItemOffhand().getTextComponent()));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_HELD_ITEM;
	}
}
