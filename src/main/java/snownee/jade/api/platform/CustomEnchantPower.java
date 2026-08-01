package snownee.jade.api.platform;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Calculates an enchantment power bonus for a block.
 */
@FunctionalInterface
public interface CustomEnchantPower {

	/**
	 * Returns the enchantment power bonus for a block state.
	 *
	 * @param state target block state
	 * @param world current level
	 * @param pos block position
	 * @return enchantment power bonus
	 */
	float getEnchantPowerBonus(IBlockState state, World world, BlockPos pos);

}
