package snownee.jade.addon.vanilla;

import java.util.Collection;
import java.util.Collections;

import org.jspecify.annotations.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.IGrowable;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

public class CropProgressProvider implements IBlockComponentProvider {
	public static final CropProgressProvider INSTANCE = new CropProgressProvider();

	@Override
	public @Nullable Element getIcon(BlockAccessor accessor, IPluginConfig config, @Nullable Element currentIcon) {
		if (accessor.getBlock() == Blocks.WHEAT) {
			return JadeUI.item(new ItemStack(Items.WHEAT));
		}

		if (accessor.getBlock() == Blocks.BEETROOTS) {
			return JadeUI.item(new ItemStack(Items.BEETROOT));
		}

		return null;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		IBlockState state = accessor.getBlockState();
		Block block = state.getBlock();

		// 1.12.2: BlockCrops.getAge/getMaxAge are protected, and BlockBeetroot overrides the age
		// property with its own 0-3 PropertyInteger (BEETROOT_AGE). Both are still named "age", so
		// the growth fraction is read off whichever integer property carries that name.
		if (block instanceof BlockCrops) {
			addAgeTooltip(tooltip, state);
		} else if (block instanceof BlockNetherWart || block instanceof IGrowable) {
			// 1.12.2: BlockNetherWart does not implement IGrowable, so it needs its own branch.
			// The BlockStateProperties.AGE_* ladder is replaced by the same generic "age" lookup:
			// it covers cocoa (0-2), nether wart (0-3) and any modded IGrowable using that name.
			addAgeTooltip(tooltip, state);
		}
		// 1.12.2: the BlockTags.MAINTAINS_FARMLAND / FarmlandBlock fallback is dropped -- block tags
		// do not exist, and no vanilla 1.12.2 block needs the "fully grown" shortcut it provided.
	}

	/**
	 * 1.12.2: replacement for the modern {@code state.hasProperty(BlockStateProperties.AGE_n)} ladder.
	 * Finds the block's integer age property by name and reports {@code age / maxAge}.
	 */
	private static void addAgeTooltip(ITooltip tooltip, IBlockState state) {
		for (IProperty<?> property : state.getPropertyKeys()) {
			if (!"age".equals(property.getName()) || property.getValueClass() != Integer.class) {
				continue;
			}
			@SuppressWarnings("unchecked")
			IProperty<Integer> ageProperty = (IProperty<Integer>) property;
			Collection<Integer> allowed = ageProperty.getAllowedValues();
			if (allowed.isEmpty()) {
				return;
			}
			int maxAge = Collections.max(allowed);
			if (maxAge <= 0) {
				return;
			}
			addMaturityTooltip(tooltip, state.getValue(ageProperty) / (float) maxAge);
			return;
		}
	}

	private static void addMaturityTooltip(ITooltip tooltip, float growthValue) {
		ITextComponent component;
		if (growthValue < 1) {
			component = IThemeHelper.get().info(String.format("%.0f%%", growthValue * 100));
		} else {
			component = IThemeHelper.get().success(new TextComponentTranslation("tooltip.jade.crop_mature"));
		}
		tooltip.add(new TextComponentTranslation("tooltip.jade.crop_growth", component));
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_CROP_PROGRESS;
	}

}
