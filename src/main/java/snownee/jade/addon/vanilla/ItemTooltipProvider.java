package snownee.jade.addon.vanilla;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TraceableException;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.util.ModIdentification;
import snownee.jade.util.WailaExceptionHandler;

public class ItemTooltipProvider implements IEntityComponentProvider {
	public static final ItemTooltipProvider INSTANCE = new ItemTooltipProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		ItemStack stack = ((EntityItem) accessor.getEntity()).getItem();
		// 1.12.2: no Item.TooltipContext, and getTooltip yields already-formatted Strings instead of
		// Components. Keep those codes for rendering; only the mod-name comparison strips them.
		List<String> lines = Lists.newArrayList();
		try {
			lines.addAll(stack.getTooltip(Minecraft.getMinecraft().player, ITooltipFlag.TooltipFlags.NORMAL));
		} catch (Throwable e) {
			ResourceLocation id = stack.getItem().getRegistryName();
			String namespace = id == null ? "minecraft" : id.getNamespace();
			WailaExceptionHandler.handleErr(TraceableException.create(e, namespace), this, tooltip::add);
		}
		if (lines.size() < 2) {
			return;
		}
		lines.remove(0);
		String modName = ModIdentification.getModName(stack);
		FontRenderer font = DisplayHelper.font().raw();
		int maxWidth = 250;
		for (String text : lines) {
			if (Objects.equals(TextFormatting.getTextWithoutFormattingCodes(text), modName)) {
				continue;
			}
			int width = font.getStringWidth(text);
			if (width > maxWidth) {
				tooltip.add(new TextComponentString(font.trimStringToWidth(text, maxWidth - 5) + ".."));
			} else {
				tooltip.add(new TextComponentString(text));
			}
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ITEM_TOOLTIP;
	}

}
