package snownee.jade.addon.core;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.block.BlockChest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.IWorldNameable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IToggleableProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.impl.ui.ItemStackElement;

public abstract class ObjectNameProvider implements IToggleableProvider {
	public static ITextComponent getEntityName(Entity entity, boolean accessibilityDetails) {
		ITextComponent customName = entity.hasCustomName() ? entity.getDisplayName() : null;
		if (customName != null && !accessibilityDetails) {
			return customName;
		}
		ITextComponent displayName = null;
		boolean wantTypeName = accessibilityDetails;
		if (WailaCommonRegistration.instance().entityTypeOperations().shouldPick(entity)) {
			ItemStack stack = entity.getPickedResult(new RayTraceResult(entity));
			if (stack != null && !stack.isEmpty()) {
				// 1.12.2: ItemStack#getDisplayName returns a String rather than a component.
				displayName = new TextComponentString(stack.getDisplayName());
			}
		}
		if (displayName == null) {
			if (entity instanceof EntityPlayer) {
				wantTypeName = false;
				displayName = entity.getDisplayName();
			} else if (entity instanceof EntityVillager) {
				// 1.12.2: villager professions do not expose a separate entity-type description.
				wantTypeName = false;
				displayName = entity.getDisplayName();
			} else if (entity instanceof EntityItem itemEntity) {
				displayName = new TextComponentString(itemEntity.getItem().getDisplayName());
			} else {
				// 1.12.2: Display entities were introduced after this version.
				displayName = entity.getDisplayName();
			}
		}
		Objects.requireNonNull(displayName);
		if (accessibilityDetails) {
			if (customName != null && displayName.getUnformattedComponentText().equals(customName.getUnformattedComponentText())) {
				displayName = customName;
				customName = null;
			}
			if (wantTypeName) {
				// 1.12.2: no Entity#getTypeName; derive the type name from the entity registry so a
				// custom-named mob still reports its species. Key format is "entity.<oldId>.name".
				ResourceLocation entityKey = EntityList.getKey(entity);
				String oldId = entityKey == null ? null : EntityList.getTranslationName(entityKey);
				ITextComponent typeName = oldId == null
						? new TextComponentString("Unknown")
						: new TextComponentTranslation("entity." + oldId + ".name");
				if (!displayName.getUnformattedComponentText().equals(typeName.getUnformattedComponentText())) {
					displayName = new TextComponentTranslation("jade.typeNameEntity", displayName, typeName);
				}
			}
			if (customName != null) {
				return new TextComponentTranslation("jade.customNameEntity", customName, displayName);
			}
		}
		return displayName;
	}

	public static class ForBlock extends ObjectNameProvider implements IBlockComponentProvider {
		public static final ForBlock INSTANCE = new ForBlock();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			ITextComponent name;
			if (accessor.isServersideContent()) {
				name = new TextComponentString(accessor.getServersideRep().getDisplayName());
			} else {
				name = BlockData.INSTANCE.decodeFromData(accessor).orElse(null);
			}
			if (name == null && WailaCommonRegistration.instance().blockOperations().shouldPick(accessor.getBlockState())) {
				ItemStack pick = accessor.getPickedResult();
				if (!pick.isEmpty()) {
					name = new TextComponentString(pick.getDisplayName());
				}
			}
			if (name == null) {
				String key = accessor.getBlock().getTranslationKey();
				if (JadeUI.hasTranslation(key)) {
					name = new TextComponentString(accessor.getBlock().getLocalizedName());
				} else {
					ItemStack pick = accessor.getPickedResult();
					if (!pick.isEmpty()) {
						name = new TextComponentString(pick.getDisplayName());
					} else {
						name = new TextComponentString(key);
					}
				}
			}
			ForEntity.addName(tooltip, name);
		}
	}

	public static class ForEntity extends ObjectNameProvider implements IEntityComponentProvider {
		public static final ForEntity INSTANCE = new ForEntity();

		public static void addName(ITooltip tooltip, ITextComponent name) {
			name = IThemeHelper.get().title(name);
			if (IWailaConfig.get().overlay().getIconMode() != IWailaConfig.IconMode.INLINE) {
				tooltip.add(name);
				return;
			}

			Element icon = tooltip.getIcon();
			Element newIcon;
			if (icon instanceof ItemStackElement itemStackElement) {
				newIcon = JadeUI.smallItem(itemStackElement.getItem());
			} else {
				newIcon = null;
			}
			if (newIcon == null) {
				tooltip.add(name);
				return;
			}
			tooltip.add(newIcon.tag(JadeIds.CORE_ROOT_ICON));
			tooltip.append(name);
		}

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			ITextComponent name;
			if (accessor.isServersideContent()) {
				name = new TextComponentString(accessor.getServersideRep().getDisplayName());
			} else {
				name = getEntityName(
						accessor.getEntity(),
						IWailaConfig.get().accessibility().getEnableAccessibilityPlugin() && config.get(JadeIds.ACCESS_ENTITY_DETAILS));
			}
			addName(tooltip, name);
		}
	}

	public static class BlockData extends ObjectNameProvider implements StreamServerDataProvider<BlockAccessor, ITextComponent> {
		public static final BlockData INSTANCE = new BlockData();
		private static final DataCodec<ITextComponent> COMPONENT_CODEC = new DataCodec<>() {
			@Override
			public ITextComponent decode(PacketBuffer buf) {
				return ITextComponent.Serializer.jsonToComponent(buf.readString(32767));
			}

			@Override
			public void encode(PacketBuffer buf, ITextComponent value) {
				buf.writeString(ITextComponent.Serializer.componentToJson(value));
			}
		};

		@Override
		@Nullable
		public ITextComponent streamData(BlockAccessor accessor) {
			TileEntity blockEntity = accessor.getBlockEntity();
			if (!(blockEntity instanceof IWorldNameable nameable)) {
				return null;
			}
			if (accessor.getBlock() instanceof BlockChest chest) {
				// 1.12.2: double chests are exposed as InventoryLargeChest rather than state properties.
				ILockableContainer container = chest.getLockableContainer(accessor.getLevel(), accessor.getPosition());
				if (container instanceof InventoryLargeChest) {
					ITextComponent name = container.getDisplayName();
					if (!(name instanceof TextComponentTranslation translation) ||
							!"container.chestDouble".equals(translation.getKey())) {
						return name;
					}
				}
			}
			if (nameable.hasCustomName()) {
				return nameable.getDisplayName();
			}
			// 1.12.2: tile-entity data components, including ITEM_NAME, do not exist.
			return null;
		}

		@Override
		public DataCodec<ITextComponent> streamCodec() {
			return COMPONENT_CODEC;
		}

		@Override
		public boolean shouldRequestData(BlockAccessor accessor) {
			// 1.12.2: only IWorldNameable tile entities can provide a server-side custom name.
			return accessor.getBlockEntity() instanceof IWorldNameable;
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.CORE_OBJECT_NAME;
	}

	@Override
	public int getDefaultPriority() {
		return TooltipPosition.HEAD - 100;
	}
}
