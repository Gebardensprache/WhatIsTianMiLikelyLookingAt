package snownee.jade.compat.gregtech;

import gregtech.api.block.machines.BlockMachine;
import gregtech.api.metatileentity.MetaTileEntityHolder;

import gregtech.common.blocks.BlockLamp;
import gregtech.common.blocks.BlockOre;

import org.jspecify.annotations.NullMarked;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.compat.gregtech.provider.AEMultiblockHatchProvider;
import snownee.jade.compat.gregtech.provider.BlockOreDataProvider;
import snownee.jade.compat.gregtech.provider.ControllableDataProvider;
import snownee.jade.compat.gregtech.provider.ConverterDataProvider;
import snownee.jade.compat.gregtech.provider.DiodeDataProvider;
import snownee.jade.compat.gregtech.provider.ElectricContainerDataProvider;
import snownee.jade.compat.gregtech.provider.LDPipeDataProvider;
import snownee.jade.compat.gregtech.provider.LampDataProvider;

@WailaPlugin("gregtech")
@NullMarked
public class GregTechPlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(AEMultiblockHatchProvider.INSTANCE, MetaTileEntityHolder.class);
		registration.registerBlockDataProvider(ControllableDataProvider.INSTANCE, MetaTileEntityHolder.class);
		registration.registerBlockDataProvider(ConverterDataProvider.INSTANCE, MetaTileEntityHolder.class);
		registration.registerBlockDataProvider(ElectricContainerDataProvider.INSTANCE, MetaTileEntityHolder.class);
		registration.registerBlockDataProvider(DiodeDataProvider.INSTANCE, MetaTileEntityHolder.class);
		registration.registerBlockDataProvider(LDPipeDataProvider.INSTANCE, MetaTileEntityHolder.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addConfig(GTIds.GT_AE_PART, true);
		registration.addConfig(GTIds.GT_ORE, true);
		registration.addConfig(GTIds.GT_CONTROLLABLE, true);
		registration.addConfig(GTIds.GT_CONVERTER, true);
		registration.addConfig(GTIds.GT_ENERGY_CONTAINER, true);
		registration.addConfig(GTIds.GT_DIODE, true);
		registration.addConfig(GTIds.GT_LD_PIPE, true);
		registration.addConfig(GTIds.GT_LAMP, true);

		registration.registerBlockComponent(AEMultiblockHatchProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(BlockOreDataProvider.INSTANCE, BlockOre.class);
		registration.registerBlockComponent(ControllableDataProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(ConverterDataProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(ElectricContainerDataProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(DiodeDataProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(LDPipeDataProvider.Client.INSTANCE, BlockMachine.class);
		registration.registerBlockComponent(LampDataProvider.INSTANCE, BlockLamp.class);
	}
}
