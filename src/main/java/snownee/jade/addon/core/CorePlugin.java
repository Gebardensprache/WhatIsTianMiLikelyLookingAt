package snownee.jade.addon.core;

import net.minecraft.entity.Entity;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.RayTraceResult;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EmptyAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.config.TargetOperationRepository;
import snownee.jade.impl.BlockAccessorClientHandler;
import snownee.jade.impl.EmptyAccessorClientHandler;
import snownee.jade.impl.EntityAccessorClientHandler;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.util.ModIdentification;

@WailaPlugin
public class CorePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(ObjectNameProvider.BlockData.INSTANCE, TileEntity.class);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.registerAccessorHandler(EmptyAccessor.class, new EmptyAccessorClientHandler());
		registration.registerAccessorHandler(BlockAccessor.class, new BlockAccessorClientHandler());
		registration.registerAccessorHandler(EntityAccessor.class, new EntityAccessorClientHandler());

		registration.addConfig(JadeIds.CORE_DISTANCE, false);
		registration.addConfig(JadeIds.CORE_COORDINATES, false);
		registration.addConfig(JadeIds.CORE_REL_COORDINATES, false);
		registration.addConfig(JadeIds.CORE_MOD_NAME, ModNameProvider.Mode.ON);
		registration.addConfig(JadeIds.CORE_TRANSLATE_MOD_NAME, true);
		registration.addConfigListener(
				JadeIds.CORE_TRANSLATE_MOD_NAME,
				$ -> ModIdentification.setTranslated(IWailaConfig.get().plugin().get($)));

		registration.registerBlockComponent(ObjectNameProvider.ForBlock.INSTANCE, Block.class);
		registration.registerBlockComponent(ModNameProvider.ForBlock.INSTANCE, Block.class);
		registration.registerBlockComponent(DistanceProvider.ForBlock.INSTANCE, Block.class);
		registration.registerBlockComponent(BlockFaceProvider.INSTANCE, Block.class);

		registration.registerEntityComponent(ObjectNameProvider.ForEntity.INSTANCE, Entity.class);
		registration.registerEntityComponent(ModNameProvider.ForEntity.INSTANCE, Entity.class);
		registration.registerEntityComponent(DistanceProvider.ForEntity.INSTANCE, Entity.class);

		registration.markAsClientFeature(JadeIds.CORE_OBJECT_NAME);
		registration.markAsClientFeature(JadeIds.CORE_DISTANCE);
		registration.markAsClientFeature(JadeIds.CORE_COORDINATES);
		registration.markAsClientFeature(JadeIds.CORE_REL_COORDINATES);
		registration.markAsClientFeature(JadeIds.CORE_MOD_NAME);
		registration.markAsClientFeature(JadeIds.CORE_BLOCK_FACE);

		registration.addRayTraceCallback(-10000, this::hideBlocks);
	}

	private Accessor<?> hideBlocks(RayTraceResult hit, Accessor<?> accessor, Accessor<?> original) {
		if (!accessor.isServersideContent() && accessor instanceof BlockAccessor blockAccessor) {
			TargetOperationRepository<Block, IBlockState> operations = WailaCommonRegistration.instance().blockOperations();
			if (operations.shouldHide(blockAccessor.getBlockState())) {
				IBlockState blockState = blockAccessor.getBlockState();
				// 1.12.2: no FluidState; use material check for liquid blocks. In 1.12.2 the liquid
				// block IS the block state, so we keep it rather than replacing with fluidState.
				Material material = blockState.getMaterial();
				boolean isLiquid = material.isLiquid();
				if (isLiquid) {
					if (blockState.getCollisionBoundingBox(accessor.getLevel(), blockAccessor.getPosition()) == null
							|| blockState.getBlock() == Blocks.BARRIER && operations.shouldHide(blockState)) {
						// 1.12.2: keep the liquid block state (no FluidState to convert)
						return WailaClientRegistration.instance()
								.blockAccessor()
								.from(blockAccessor)
								.blockState(blockState)
								.build();
					}
				}
				return WailaClientRegistration.instance().emptyAccessor().hit(blockAccessor.getHitResult()).build();
			}
		}
		return accessor;
	}
}
