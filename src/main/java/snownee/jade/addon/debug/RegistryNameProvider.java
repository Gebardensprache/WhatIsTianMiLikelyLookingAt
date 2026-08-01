package snownee.jade.addon.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IToggleableProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.ModIdentification;

public abstract class RegistryNameProvider implements IToggleableProvider {

	public static ForBlock getBlock() {
		return ForBlock.INSTANCE;
	}

	public static ForEntity getEntity() {
		return ForEntity.INSTANCE;
	}

	public static class ForBlock extends RegistryNameProvider implements IBlockComponentProvider {
		private static final ForBlock INSTANCE = new ForBlock();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			ResourceLocation id = CommonProxy.getId(accessor.getBlock());
			if (accessor.isServersideContent()) {
				id = ModIdentification.getSpecialId(accessor.getServersideRep()).orElse(id);
			}
			if (append(tooltip, id, config) && config.get(JadeIds.DEBUG_SPECIAL_REGISTRY_NAME)) {
				TileEntity blockEntity = accessor.getBlockEntity();
				if (blockEntity != null) {
					id = TileEntity.getKey(blockEntity.getClass());
					String s = I18n.format("config.jade.plugin_jade.registry_name.special.block_entity_type", id);
					tooltip.add(IWailaConfig.get().formatting().registryName(s), JadeIds.DEBUG_SPECIAL_REGISTRY_NAME);
				}
				// 1.12.2: no FluidState in block states; use the Forge fluid
				// registry mapping for the block instead. Fluid names may contain
				// invalid ResourceLocation characters, so guard the conversion.
				Fluid fluid = FluidRegistry.lookupFluidForBlock(accessor.getBlock());
				if (fluid != null) {
					try {
						id = new ResourceLocation(fluid.getName());
						String s = I18n.format("config.jade.plugin_jade.registry_name.special.fluid", id);
						tooltip.add(IWailaConfig.get().formatting().registryName(s), JadeIds.DEBUG_SPECIAL_REGISTRY_NAME);
					} catch (IllegalArgumentException e) {
						// skip fluids whose names are not valid ResourceLocations
					}
				}
				// 1.12.2: no POI registry exists; the POI special-id branch is dropped.
			}
		}
	}

	public static class ForEntity extends RegistryNameProvider implements IEntityComponentProvider {
		private static final ForEntity INSTANCE = new ForEntity();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			ResourceLocation id = CommonProxy.getId(accessor.getEntity().getClass());
			if (accessor.isServersideContent()) {
				id = ModIdentification.getSpecialId(accessor.getServersideRep()).orElse(id);
			}
			if (append(tooltip, id, config) && config.get(JadeIds.DEBUG_SPECIAL_REGISTRY_NAME)) {
				if (accessor.getEntity() instanceof EntityPainting painting) {
					// 1.12.2: paintings have no registry variants; use the art title.
					id = new ResourceLocation(painting.art.title);
					String s = I18n.format("config.jade.plugin_jade.registry_name.special.painting", id);
					tooltip.add(IWailaConfig.get().formatting().registryName(s), JadeIds.DEBUG_SPECIAL_REGISTRY_NAME);
				}
			}
		}
	}

	public boolean append(ITooltip tooltip, ResourceLocation id, IPluginConfig config) {
		Mode mode = config.getEnum(JadeIds.DEBUG_REGISTRY_NAME);
		if (mode == Mode.OFF) {
			return false;
		}
		if (mode == Mode.ADVANCED_TOOLTIPS && !Minecraft.getMinecraft().gameSettings.advancedItemTooltips) {
			return false;
		}
		tooltip.add(IWailaConfig.get().formatting().registryName(id.toString()));
		return true;
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.DEBUG_REGISTRY_NAME;
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public int getDefaultPriority() {
		return TooltipPosition.HEAD + 100;
	}

	public enum Mode {
		ON, OFF, ADVANCED_TOOLTIPS
	}

}
