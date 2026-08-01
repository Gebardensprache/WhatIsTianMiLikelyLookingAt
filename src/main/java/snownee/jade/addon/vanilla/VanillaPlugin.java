package snownee.jade.addon.vanilla;

import java.util.List;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecartMobSpawner;
import net.minecraft.entity.item.EntityPainting;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntityComparator;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import snownee.jade.JadeClient;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.harvest.ToolTier;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.overlay.DatapackBlockManager;

@WailaPlugin
public class VanillaPlugin implements IWailaPlugin {

	private static final Cache<IBlockState, IBlockState> CHEST_CACHE = CacheBuilder.newBuilder().build();

	public static IBlockState getCorrespondingNormalChest(IBlockState state) {
		try {
			return CHEST_CACHE.get(state, () -> {
				ResourceLocation trappedName = state.getBlock().getRegistryName();
				if (trappedName == null) {
					return state;
				}
				Block block = Blocks.AIR;
				if (trappedName.getPath().startsWith("trapped_")) {
					ResourceLocation chestName = new ResourceLocation(
							trappedName.getNamespace(), trappedName.getPath().substring("trapped_".length()));
					block = ForgeRegistries.BLOCKS.getValue(chestName);
				} else if (trappedName.getPath().endsWith("_trapped_chest")) {
					ResourceLocation chestName = new ResourceLocation(
							trappedName.getNamespace(),
							trappedName.getPath().substring(0, trappedName.getPath().length() - "_trapped_chest".length()) + "_chest");
					block = ForgeRegistries.BLOCKS.getValue(chestName);
				}
				if (block != null && block != Blocks.AIR) {
					return copyProperties(state, block.getDefaultState());
				}
				return state;
			});
		} catch (Exception e) {
			return state;
		}
	}

	private static IBlockState copyProperties(IBlockState oldState, IBlockState newState) {
		for (IProperty<?> property : oldState.getPropertyKeys()) {
			if (newState.getPropertyKeys().contains(property)) {
				newState = copyProperty(oldState, newState, property);
			}
		}
		return newState;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static IBlockState copyProperty(IBlockState oldState, IBlockState newState, IProperty<?> property) {
		IProperty typedProperty = property;
		return newState.withProperty(typedProperty, oldState.getValue(typedProperty));
	}

	@Override
	public void register(IWailaCommonRegistration registration) {
		registration.registerBlockDataProvider(BrewingStandProvider.INSTANCE, TileEntityBrewingStand.class);
		registration.registerBlockDataProvider(CommandBlockProvider.INSTANCE, TileEntityCommandBlock.class);
		registration.registerBlockDataProvider(HopperLockProvider.INSTANCE, TileEntityHopper.class);
		registration.registerBlockDataProvider(JukeboxProvider.INSTANCE, BlockJukebox.TileEntityJukebox.class);
		registration.registerBlockDataProvider(RedstoneProvider.INSTANCE, TileEntityComparator.class);
		registration.registerBlockDataProvider(FurnaceProvider.INSTANCE, TileEntityFurnace.class);

		registration.registerEntityDataProvider(AnimalOwnerProvider.INSTANCE, Entity.class);
		registration.registerEntityDataProvider(StatusEffectsProvider.INSTANCE, EntityLivingBase.class);
		registration.registerEntityDataProvider(MobGrowthProvider.INSTANCE, EntityAgeable.class);
		registration.registerEntityDataProvider(MobBreedingProvider.INSTANCE, EntityAnimal.class);
		registration.registerEntityDataProvider(MobBreedingProvider.INSTANCE, EntityVillager.class);
		registration.registerEntityDataProvider(ZombieVillagerProvider.INSTANCE, EntityZombieVillager.class);
		registration.registerEntityDataProvider(EntityHealthAndArmorProvider.INSTANCE, EntityLivingBase.class);

		// 1.12.2: beehives, lecterns, shelves, campfires, trial spawners, display entities,
		// copper golems, pet armour and sulfur cubes do not exist. Their providers are not registered.
		// MobSpawnerCooldownProvider remains trial-spawner-specific and is likewise not registered.
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		registration.addConfig(JadeIds.MC_EFFECTIVE_TOOL, true);
		registration.addConfig(JadeIds.MC_HARVEST_TOOL_NEW_LINE, false);
		registration.addConfig(JadeIds.MC_SHOW_UNBREAKABLE, false);
		registration.addConfig(JadeIds.MC_HARVEST_TOOL_CREATIVE, false);
		registration.addConfig(JadeIds.MC_BREAKING_PROGRESS, true);
		registration.addConfig(JadeIds.MC_ENTITY_HEALTH, true);
		registration.addConfig(JadeIds.MC_ENTITY_ARMOR, true);
		registration.addConfig(JadeIds.MC_ENTITY_ARMOR_MAX_FOR_RENDER, 20, 0, 200, false);
		registration.addConfig(JadeIds.MC_ENTITY_HEALTH_MAX_FOR_RENDER, 40, 0, 200, false);
		registration.addConfig(JadeIds.MC_ENTITY_HEALTH_ICONS_PER_LINE, 10, 5, 40, false);
		registration.addConfig(JadeIds.MC_ENTITY_HEALTH_SHOW_FRACTIONS, false);
		registration.addConfig(JadeIds.MC_POTION_EFFECTS_LIMIT, 7, 1, 99, false);

		registration.registerBlockComponent(BrewingStandProvider.Client.INSTANCE, BlockBrewingStand.class);
		registration.registerEntityComponent(HorseStatsProvider.INSTANCE, AbstractHorse.class);
		registration.registerEntityComponent(ItemFrameProvider.INSTANCE, EntityItemFrame.class);
		registration.registerEntityComponent(StatusEffectsProvider.Client.INSTANCE, EntityLivingBase.class);
		registration.registerEntityComponent(MobGrowthProvider.Client.INSTANCE, EntityAgeable.class);
		registration.registerEntityComponent(MobBreedingProvider.Client.INSTANCE, EntityAnimal.class);
		registration.registerEntityComponent(MobBreedingProvider.Client.INSTANCE, EntityVillager.class);
		registration.registerBlockComponent(NoteBlockProvider.INSTANCE, BlockNote.class);
		registration.registerEntityComponent(ArmorStandProvider.INSTANCE, EntityArmorStand.class);
		registration.registerEntityComponent(PaintingProvider.INSTANCE, EntityPainting.class);
		registration.registerBlockComponent(HarvestToolProvider.INSTANCE, Block.class);
		registration.registerBlockComponent(CommandBlockProvider.Client.INSTANCE, BlockCommandBlock.class);
		registration.registerBlockComponent(EnchantmentPowerProvider.INSTANCE, Block.class);
		registration.registerBlockComponent(TotalEnchantmentPowerProvider.INSTANCE, BlockEnchantmentTable.class);
		registration.registerBlockComponent(PlayerHeadProvider.INSTANCE, BlockSkull.class);
		registration.registerBlockIcon(ItemBERProvider.INSTANCE, BlockSkull.class);
		registration.registerEntityComponent(VillagerProfessionProvider.INSTANCE, EntityVillager.class);
		registration.registerEntityComponent(VillagerProfessionProvider.INSTANCE, EntityZombieVillager.class);
		registration.registerEntityComponent(ItemTooltipProvider.INSTANCE, EntityItem.class);
		registration.registerBlockComponent(FurnaceProvider.Client.INSTANCE, BlockFurnace.class);
		registration.registerEntityComponent(AnimalOwnerProvider.Client.INSTANCE, Entity.class);
		registration.registerEntityIcon(FallingBlockProvider.INSTANCE, EntityFallingBlock.class);
		registration.registerEntityComponent(EntityHealthAndArmorProvider.Client.INSTANCE, EntityLivingBase.class);
		registration.registerBlockComponent(RedstoneProvider.Client.INSTANCE, Block.class);
		registration.registerBlockComponent(HopperLockProvider.Client.INSTANCE, BlockHopper.class);
		registration.registerBlockComponent(CropProgressProvider.INSTANCE, Block.class);
		registration.registerBlockComponent(JukeboxProvider.Client.INSTANCE, BlockJukebox.class);
		registration.registerBlockComponent(MobSpawnerProvider.ForBlock.INSTANCE, BlockMobSpawner.class);
		registration.registerEntityComponent(MobSpawnerProvider.ForEntity.INSTANCE, EntityMinecartMobSpawner.class);
		registration.registerEntityComponent(ZombieVillagerProvider.Client.INSTANCE, EntityZombieVillager.class);

		// 1.12.2: DatapackBlockManager is a translated no-op and still has the legacy callback signature.
		registration.addRayTraceCallback(-10010, DatapackBlockManager::override);
		// 1.12.2: JadeClient's upstream fog and camouflage callbacks use modern rendering APIs
		// and are intentionally not registered until independently translated.
		registration.addAfterRenderCallback(100, JadeClient::drawBreakingProgress);

		registration.markAsClientFeature(JadeIds.MC_EFFECTIVE_TOOL);
		registration.markAsClientFeature(JadeIds.MC_HARVEST_TOOL_NEW_LINE);
		registration.markAsClientFeature(JadeIds.MC_SHOW_UNBREAKABLE);
		registration.markAsClientFeature(JadeIds.MC_HARVEST_TOOL_CREATIVE);
		registration.markAsClientFeature(JadeIds.MC_BREAKING_PROGRESS);
		registration.markAsClientFeature(JadeIds.MC_ENTITY_ARMOR_MAX_FOR_RENDER);
		registration.markAsClientFeature(JadeIds.MC_ENTITY_HEALTH_MAX_FOR_RENDER);
		registration.markAsClientFeature(JadeIds.MC_ENTITY_HEALTH_ICONS_PER_LINE);
		registration.markAsClientFeature(JadeIds.MC_ENTITY_HEALTH_SHOW_FRACTIONS);
		registration.markAsClientFeature(JadeIds.MC_HORSE_STATS);
		registration.markAsClientFeature(JadeIds.MC_ITEM_FRAME);
		registration.markAsClientFeature(JadeIds.MC_NOTE_BLOCK);
		registration.markAsClientFeature(JadeIds.MC_ARMOR_STAND);
		registration.markAsClientFeature(JadeIds.MC_PAINTING);
		registration.markAsClientFeature(JadeIds.MC_HARVEST_TOOL);
		registration.markAsClientFeature(JadeIds.MC_ENCHANTMENT_POWER);
		registration.markAsClientFeature(JadeIds.MC_TOTAL_ENCHANTMENT_POWER);
		registration.markAsClientFeature(JadeIds.MC_PLAYER_HEAD);
		registration.markAsClientFeature(JadeIds.MC_VILLAGER_PROFESSION);
		registration.markAsClientFeature(JadeIds.MC_ITEM_TOOLTIP);
		registration.markAsClientFeature(JadeIds.MC_ENTITY_ARMOR);
		registration.markAsClientFeature(JadeIds.MC_CROP_PROGRESS);
		registration.markAsClientFeature(JadeIds.MC_MOB_SPAWNER);

		ITextComponent block = new TextComponentTranslation("config.jade.plugin_minecraft.block");
		ITextComponent entity = new TextComponentTranslation("config.jade.plugin_minecraft.entity");
		List<ITextComponent> both = List.of(block, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_ANIMAL_OWNER, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_ARMOR_STAND, both);
		registration.setConfigCategoryOverride(JadeIds.MC_BREAKING_PROGRESS, block);
		registration.setConfigCategoryOverride(JadeIds.MC_BREWING_STAND, block);
		registration.setConfigCategoryOverride(JadeIds.MC_COMMAND_BLOCK, block);
		registration.setConfigCategoryOverride(JadeIds.MC_CROP_PROGRESS, block);
		registration.setConfigCategoryOverride(JadeIds.MC_ENCHANTMENT_POWER, block);
		registration.setConfigCategoryOverride(JadeIds.MC_ENTITY_ARMOR, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_ENTITY_HEALTH, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_FURNACE, block);
		registration.setConfigCategoryOverride(JadeIds.MC_HARVEST_TOOL, block);
		registration.setConfigCategoryOverride(JadeIds.MC_HORSE_STATS, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_ITEM_FRAME, both);
		registration.setConfigCategoryOverride(JadeIds.MC_ITEM_TOOLTIP, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_JUKEBOX, block);
		registration.setConfigCategoryOverride(JadeIds.MC_MOB_BREEDING, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_MOB_GROWTH, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_MOB_SPAWNER, block);
		registration.setConfigCategoryOverride(JadeIds.MC_NOTE_BLOCK, block);
		registration.setConfigCategoryOverride(JadeIds.MC_PAINTING, both);
		registration.setConfigCategoryOverride(JadeIds.MC_PLAYER_HEAD, block);
		registration.setConfigCategoryOverride(JadeIds.MC_POTION_EFFECTS, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_REDSTONE, block);
		registration.setConfigCategoryOverride(JadeIds.MC_TOTAL_ENCHANTMENT_POWER, block);
		registration.setConfigCategoryOverride(JadeIds.MC_VILLAGER_PROFESSION, entity);
		registration.setConfigCategoryOverride(JadeIds.MC_ZOMBIE_VILLAGER, entity);

		WailaCommonRegistration.instance().priorities.putUnsafe(JadeIds.MC_ENTITY_ARMOR, -4499);

		registration.addHarvestPlugin(registry -> {
			registry.type(JadeIds.JADE("pickaxe"))
					.addTier(ToolTier.item(Items.WOODEN_PICKAXE))
					.addTier(ToolTier.item(Items.GOLDEN_PICKAXE))
					.addTier(ToolTier.item(Items.STONE_PICKAXE))
					.addTier(ToolTier.item(Items.IRON_PICKAXE))
					.addTier(ToolTier.item(Items.DIAMOND_PICKAXE));
			registry.type(JadeIds.JADE("axe"))
					.addTier(ToolTier.item(Items.WOODEN_AXE))
					.addTier(ToolTier.item(Items.GOLDEN_AXE))
					.addTier(ToolTier.item(Items.STONE_AXE))
					.addTier(ToolTier.item(Items.IRON_AXE))
					.addTier(ToolTier.item(Items.DIAMOND_AXE));
			registry.type(JadeIds.JADE("shovel"))
					.addTier(ToolTier.item(Items.WOODEN_SHOVEL))
					.addTier(ToolTier.item(Items.GOLDEN_SHOVEL))
					.addTier(ToolTier.item(Items.STONE_SHOVEL))
					.addTier(ToolTier.item(Items.IRON_SHOVEL))
					.addTier(ToolTier.item(Items.DIAMOND_SHOVEL));
			registry.type(JadeIds.JADE("hoe"))
					.addTier(ToolTier.item(Items.WOODEN_HOE))
					.addTier(ToolTier.item(Items.GOLDEN_HOE))
					.addTier(ToolTier.item(Items.STONE_HOE))
					.addTier(ToolTier.item(Items.IRON_HOE))
					.addTier(ToolTier.item(Items.DIAMOND_HOE));
			registry.type(JadeIds.JADE("sword"))
					.addTier(ToolTier.item(Items.WOODEN_SWORD))
					.addTier(ToolTier.item(Items.GOLDEN_SWORD))
					.addTier(ToolTier.item(Items.STONE_SWORD))
					.addTier(ToolTier.item(Items.IRON_SWORD))
					.addTier(ToolTier.item(Items.DIAMOND_SWORD));
			registry.type(JadeIds.JADE("shears"), false).addTier(registry.defaultShearsTier());
		});
	}
}
