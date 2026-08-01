package snownee.jade.addon.vanilla;

import org.jspecify.annotations.Nullable;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

public class ItemBERProvider implements IBlockComponentProvider {
	public static final ItemBERProvider INSTANCE = new ItemBERProvider();

	@Override
	public @Nullable Element getIcon(BlockAccessor accessor, IPluginConfig config, @Nullable Element currentIcon) {
		TileEntity blockEntity = accessor.getBlockEntity();
		if (blockEntity != null) {
			ItemStack itemStack = accessor.getPickedResult();
			// 1.12.2: no data components, no TagValueOutput/ProblemReporter. The block entity is
			// serialised straight to NBT and stashed under "BlockEntityTag" -- the tag vanilla's own
			// item renderers read back -- after dropping the position keys, which are meaningless on
			// an item and would otherwise make otherwise-identical stacks compare unequal.
			NBTTagCompound tag = blockEntity.writeToNBT(new NBTTagCompound());
			tag.removeTag("x");
			tag.removeTag("y");
			tag.removeTag("z");
			itemStack.setTagInfo("BlockEntityTag", tag);
			return JadeUI.item(itemStack);
		}
		return null;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ITEM_BER;
	}

	@Override
	public boolean isRequired() {
		return true;
	}

}
