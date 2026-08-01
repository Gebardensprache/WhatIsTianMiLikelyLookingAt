package snownee.jade.addon.debug;

import net.minecraft.block.Block;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class BlockPropertiesProvider implements IBlockComponentProvider {
	public static final BlockPropertiesProvider INSTANCE = new BlockPropertiesProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		IThemeHelper themes = IThemeHelper.get();
		// 1.12.2: no BlockBehaviour.Properties; read the effective values from the
		// state (hardness is location-aware in 1.12.2). Unbreakable blocks return
		// a negative value, same as the modern destroyTime of -1.
		float destroyTime = accessor.getBlockState().getBlockHardness(accessor.getLevel(), accessor.getPosition());
		tooltip.add(new TextComponentTranslation("jade.block_destroy_time", themes.info(destroyTime)));
		// 1.12.2: explosion resistance is the raw resistance, not the modern
		// resistance / 5.0F. No jumpFactor/speedFactor on 1.12.2 blocks.
		tooltip.add(new TextComponentTranslation("jade.block_explosion_resistance", themes.info(accessor.getBlock().getExplosionResistance(accessor.getLevel(), accessor.getPosition(), null, null))));
		// 1.12.2: no FireBlock.getIgniteOdds/getBurnOdds; the Forge
		// location-aware methods on the block itself perform the same map lookup.
		Block block = accessor.getBlock();
		int igniteOdds = block.getFlammability(accessor.getLevel(), accessor.getPosition(), EnumFacing.UP);
		if (igniteOdds != 0) {
			tooltip.add(new TextComponentTranslation("jade.block_ignite_odds", themes.info(igniteOdds)));
		}
		int burnOdds = block.getFireSpreadSpeed(accessor.getLevel(), accessor.getPosition(), EnumFacing.UP);
		if (burnOdds != 0) {
			tooltip.add(new TextComponentTranslation("jade.block_burn_odds", themes.info(burnOdds)));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.DEBUG_BLOCK_PROPERTIES;
	}

	@Override
	public boolean enabledByDefault() {
		return false;
	}
}
