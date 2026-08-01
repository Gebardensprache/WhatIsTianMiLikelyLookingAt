package snownee.jade.addon.vanilla;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.util.ClientProxy;

public class TotalEnchantmentPowerProvider implements IBlockComponentProvider {
	public static final TotalEnchantmentPowerProvider INSTANCE = new TotalEnchantmentPowerProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		World world = accessor.getLevel();
		BlockPos pos = accessor.getPosition();
		float power = 0;
		// ContainerEnchantment.java
		// 1.12.2: there is no EnchantingTableBlock.BOOKSHELF_OFFSETS / isValidBookShelf. The
		// vanilla scan lives inline in ContainerEnchantment#onCraftMatrixChanged: for each of
		// the 8 horizontal neighbours that is clear (air at both y and y+1), count the shelf
		// two blocks out on that axis at y and y+1, plus the two diagonal in-between columns.
		for (int j = -1; j <= 1; ++j) {
			for (int k = -1; k <= 1; ++k) {
				if ((j != 0 || k != 0) && world.isAirBlock(pos.add(k, 0, j)) && world.isAirBlock(pos.add(k, 1, j))) {
					power += getPower(world, pos.add(k * 2, 0, j * 2));
					power += getPower(world, pos.add(k * 2, 1, j * 2));
					if (k != 0 && j != 0) {
						power += getPower(world, pos.add(k * 2, 0, j));
						power += getPower(world, pos.add(k * 2, 1, j));
						power += getPower(world, pos.add(k, 0, j * 2));
						power += getPower(world, pos.add(k, 1, j * 2));
					}
				}
			}
		}

		if (power > 0) {
			tooltip.add(new TextComponentTranslation("jade.ench_power", IThemeHelper.get().info(DisplayHelper.dfCommas.format(power))));
		}
	}

	public static float getPower(World world, BlockPos pos) {
		return ClientProxy.getEnchantPowerBonus(world.getBlockState(pos), world, pos);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_TOTAL_ENCHANTMENT_POWER;
	}

	@Override
	public int getDefaultPriority() {
		return -400;
	}
}
