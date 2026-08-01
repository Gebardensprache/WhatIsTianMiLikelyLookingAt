package snownee.jade.test;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidUtil;

public class TestBlock extends BlockContainer {

	public TestBlock() {
		super(Material.ROCK);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		FluidUtil.interactWithFluidHandler(playerIn, hand, worldIn, pos, facing);
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new TestBlockEntity();
	}

	/**
	 * Like the old updateEntity(), except more generic.
	 */
	@Override
	public void updateTick(World worldIn, BlockPos pos, IBlockState state, java.util.Random rand) {
		TileEntity tileEntity = worldIn.getTileEntity(pos);
		if (tileEntity instanceof TestBlockEntity) {
			((TestBlockEntity) tileEntity).tick();
		}
	}
}
