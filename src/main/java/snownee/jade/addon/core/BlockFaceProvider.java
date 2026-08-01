package snownee.jade.addon.core;

import java.util.List;

import snownee.jade.api.ui.Element;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

public class BlockFaceProvider implements IBlockComponentProvider {
	public static final BlockFaceProvider INSTANCE = new BlockFaceProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		tooltip.replace(
				JadeIds.CORE_OBJECT_NAME, lists -> {
					// 1.12.2: no List.getLast() in Java 8
					List<Element> lastList = lists.get(lists.size() - 1);
					lastList.add(JadeUI.text(new TextComponentTranslation("jade.blockFace", directionName(accessor.getSide()))));
					return lists;
				});
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.CORE_BLOCK_FACE;
	}

	@Override
	public int getDefaultPriority() {
		return ObjectNameProvider.ForBlock.INSTANCE.getDefaultPriority() + 30;
	}

	@Override
	public boolean enabledByDefault() {
		return false;
	}

	public static ITextComponent directionName(EnumFacing direction) {
		return new TextComponentTranslation("jade." + direction.getName());
	}

}
