package snownee.jade.test;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.BlockFurnace;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityDispenser;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import snownee.jade.Jade;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.harvest.ToolTier;
import snownee.jade.api.view.HideThingsExtensionProvider;

public class ExamplePlugin implements IWailaPlugin {

	public static final ResourceLocation UID_TEST_FUEL = new ResourceLocation("debug:furnace_fuel");
	public static final ResourceLocation UID_TEST_BREWING = new ResourceLocation("debug:item_storage");
	public static final ResourceLocation UID_TEST_FLUIDS = new ResourceLocation("debug:fluid_storage");
	public static final ResourceLocation UID_TEST_ENERGY = new ResourceLocation("debug:energy_storage");
	public static final ResourceLocation UID_TEST_PROGRESS = new ResourceLocation("debug:progress");
	public static final ResourceLocation UID_TEST_STR_CFG = new ResourceLocation("debug:furnace_fuel.str_cfg");
	public static final ResourceLocation UID_TEST_FLOAT_CFG = new ResourceLocation("debug:furnace_fuel.float_cfg");

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(ExampleDataProvider.INSTANCE, TileEntityFurnace.class);
		registration.registerItemStorage(ExampleItemStorageProvider.INSTANCE, TileEntityBrewingStand.class);
		registration.registerItemStorage(HideThingsExtensionProvider.instance(), TileEntityDispenser.class);
		registration.registerFluidStorage(ExampleFluidStorageProvider.INSTANCE, EntitySlime.class);
		registration.registerEnergyStorage(ExampleEnergyStorageProvider.INSTANCE, EntitySheep.class);
		registration.registerProgress(ExampleProgressProvider.INSTANCE, TileEntityFurnace.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerBlockComponent(ExampleComponentProvider.INSTANCE, BlockFurnace.class);
		// 1.12.2: ResourceLocation never validates its input, so the modern Identifier.tryParse
		// check is reimplemented here: optional "namespace:" prefix + non-empty [a-z0-9_.-]+ path.
		registration.addConfig(UID_TEST_STR_CFG, "", $ -> $ != null && $.matches("^[a-z0-9_.-]+(:[a-z0-9_.-]+)?$") && !$.endsWith(":"));
		registration.addConfigListener(UID_TEST_STR_CFG, $ -> Jade.LOGGER.info("Changed: $: " + IWailaConfig.get().plugin().getString($)));
		registration.addConfig(UID_TEST_FLOAT_CFG, 0F, 0F, 100F, false);

		registration.addRayTraceCallback((_, accessor, _) -> {
			if (IWailaConfig.get().general().isDebug() && accessor instanceof BlockAccessor blockAccessor) {
				if (blockAccessor.getBlock() == Blocks.GRASS) {
					return registration.blockAccessor().from(blockAccessor).blockState(Blocks.TNT.getDefaultState()).build();
				}
			}
			return accessor;
		});

		registration.addRayTraceCallback(
				(_, accessor, _) -> {
					if (accessor instanceof BlockAccessor blockAccessor) {
						if (blockAccessor.getBlock().equals(Blocks.FURNACE)) {
							BlockPos newPos = blockAccessor.getPosition().down();
							return registration.blockAccessor()
									.from(blockAccessor)
									.hit(new RayTraceResult(
											blockAccessor.getHitResult().hitVec,
											blockAccessor.getHitResult().sideHit,
											newPos))
									.blockState(blockAccessor.getLevel().getBlockState(newPos))
									.blockEntity(blockAccessor.getLevel().getTileEntity(newPos))
									.build();
						}
					}
					return accessor;
				});

		registration.registerItemStorageClient(ExampleItemStorageProvider.INSTANCE);
		registration.registerFluidStorageClient(ExampleFluidStorageProvider.INSTANCE);
		registration.registerEnergyStorageClient(ExampleEnergyStorageProvider.INSTANCE);
		registration.registerProgressClient(ExampleProgressProvider.INSTANCE);

		// expected behavior: shows iron pickaxe on stone
		// 1.12.2: no copper tools; IRON_PICKAXE stands in for the modern COPPER_PICKAXE demo
		registration.addHarvestPlugin(registry -> {
			registry.insertTierBefore(
					JadeIds.JADE("pickaxe"),
					new ResourceLocation("test_pickaxe"),
					ToolTier.item(Items.IRON_PICKAXE));
			registry.type(new ResourceLocation("aaa"));
			registry.type(new ResourceLocation("bbb")).addTier(ToolTier.alwaysFail(Items.DIAMOND)
					.addExtraBlocks(Collections.singletonList(Blocks.OBSIDIAN)));
		});
		registration.addHarvestPlugin(registry -> registry.insertTierBefore(
				JadeIds.JADE("pickaxe"),
				new ResourceLocation("wooden_pickaxe"),
				ToolTier.item(new ResourceLocation("test_pickaxe"), Items.DIAMOND_PICKAXE)));
	}

}
