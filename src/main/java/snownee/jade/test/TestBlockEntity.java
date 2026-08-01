package snownee.jade.test;

import org.jspecify.annotations.Nullable;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

public class TestBlockEntity extends TileEntity {

	TestEnergyStorage energyStorage = new TestEnergyStorage();

	FluidTank fluidStorage = new FluidTank(10000);

	public TestBlockEntity() {
	}

	public void tick() {
		energyStorage.energy += world.rand.nextInt(1000);
		if (energyStorage.energy > energyStorage.getMaxEnergyStored()) {
			energyStorage.energy = 0;
		}
	}

	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return true;
		}
		return super.hasCapability(capability, facing);
	}

	@Override
	@Nullable
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY) {
			return CapabilityEnergy.ENERGY.cast(energyStorage);
		}
		if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
			return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(fluidStorage);
		}
		return super.getCapability(capability, facing);
	}

	public static class TestEnergyStorage implements IEnergyStorage {

		int energy;

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getEnergyStored() {
			// TODO Auto-generated method stub
			return energy;
		}

		@Override
		public int getMaxEnergyStored() {
			// TODO Auto-generated method stub
			return 100000;
		}

		@Override
		public boolean canExtract() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canReceive() {
			// TODO Auto-generated method stub
			return false;
		}

	}

}
