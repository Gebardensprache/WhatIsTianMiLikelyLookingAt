package snownee.jade.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import snownee.jade.Jade;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IJadeProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.TargetOperationRepository;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.impl.config.TargetOperationRepositoryImpl;
import snownee.jade.impl.lookup.HierarchyLookup;
import snownee.jade.impl.lookup.PairHierarchyLookup;
import snownee.jade.impl.lookup.WrappedHierarchyLookup;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.JadeCodecs;

public class WailaCommonRegistration implements IWailaCommonRegistration {

	private static WailaCommonRegistration INSTANCE = new WailaCommonRegistration();

	public final PairHierarchyLookup<IServerDataProvider<BlockAccessor>> blockDataProviders;
	public final HierarchyLookup<IServerDataProvider<EntityAccessor>> entityDataProviders;
	public final PriorityStore<ResourceLocation, IJadeProvider> priorities;

	public final WrappedHierarchyLookup<IServerExtensionProvider<ItemStack>> itemStorageProviders;
	public final WrappedHierarchyLookup<IServerExtensionProvider<FluidView.Data>> fluidStorageProviders;
	public final WrappedHierarchyLookup<IServerExtensionProvider<EnergyView.Data>> energyStorageProviders;
	public final WrappedHierarchyLookup<IServerExtensionProvider<ProgressView.Data>> progressProviders;

	private final TargetOperationRepositoryImpl<Block, IBlockState> blockOperations;
	private final TargetOperationRepositoryImpl<Class<? extends Entity>, Entity> entityTypeOperations;
	private final TargetOperationRepositoryImpl<Potion, PotionEffect> mobEffectOperations;

	WailaCommonRegistration() {
		blockDataProviders = new PairHierarchyLookup<>(new HierarchyLookup<>(Block.class), new HierarchyLookup<>(TileEntity.class));
		blockDataProviders.idMapped();
		entityDataProviders = new HierarchyLookup<>(Entity.class);
		entityDataProviders.idMapped();
		priorities = new PriorityStore<>(IJadeProvider::getDefaultPriority, IJadeProvider::getUid);
		priorities.setSortingFunction((store, allKeys) -> {
			List<ResourceLocation> keys = allKeys.stream()
					.filter(IPluginConfig::isPrimaryKey)
					.sorted(Comparator.comparingInt(store::byKey))
					.collect(Collectors.toCollection(ArrayList::new));
			allKeys.stream().filter(Predicate.not(IPluginConfig::isPrimaryKey)).forEach($ -> {
				int index = keys.indexOf(IPluginConfig.getPrimaryKey($));
				keys.add(index + 1, $);
			});
			return keys;
		});
		priorities.configurable(Jade.ID + "/sort-order", JadeCodecs.RESOURCE_LOCATION);

		itemStorageProviders = WrappedHierarchyLookup.forAccessor();
		fluidStorageProviders = WrappedHierarchyLookup.forAccessor();
		energyStorageProviders = WrappedHierarchyLookup.forAccessor();
		progressProviders = WrappedHierarchyLookup.forAccessor();

		blockOperations = new TargetOperationRepositoryImpl<>(
				$ -> ((IBlockState) $).getBlock().getRegistryName(),
				"hide-blocks",
				() -> CommonProxy.isPhysicallyClient() ? List.of("minecraft:barrier") : List.of());
		entityTypeOperations = new TargetOperationRepositoryImpl<>(
				$ -> EntityList.getKey($),
				"hide-entities",
				() -> CommonProxy.isPhysicallyClient() ?
						List.of("minecraft:area_effect_cloud", "minecraft:firework_rocket", "minecraft:interaction", "minecraft:text_display", "minecraft:lightning_bolt") :
						List.of());
		mobEffectOperations = new TargetOperationRepositoryImpl<>(
				$ -> Potion.REGISTRY.getNameForObject(((PotionEffect) $).getPotion()),
				"hide-mob-effects",
				List::of);
	}

	public static WailaCommonRegistration instance() {
		return INSTANCE;
	}

	public static void reset() {
		INSTANCE = new WailaCommonRegistration();
	}

	@Override
	public void registerBlockDataProvider(IServerDataProvider<BlockAccessor> dataProvider, Class<?> blockOrBlobkEntityClass) {
		checkDataProvider(dataProvider);
		blockDataProviders.register(blockOrBlobkEntityClass, dataProvider);
	}

	@Override
	public void registerEntityDataProvider(IServerDataProvider<EntityAccessor> dataProvider, Class<? extends Entity> entityClass) {
		checkDataProvider(dataProvider);
		entityDataProviders.register(entityClass, dataProvider);
	}

	private static void checkDataProvider(IServerDataProvider<?> dataProvider) {
		if (CommonProxy.isPhysicallyClient() && dataProvider instanceof IComponentProvider) {
			throw new IllegalArgumentException(
					"Data providers cannot implement IComponentProvider since Minecraft 1.21.6. Use a separate client provider instead.");
		}
	}

	/* PROVIDER GETTERS */
	public List<IServerDataProvider<BlockAccessor>> blockDataProvidersOf(
			IBlockState blockState,
			@Nullable TileEntity blockEntity,
			boolean checkIsHidden) {
		if (checkIsHidden && blockOperations().shouldHide(blockState)) {
			return List.of();
		}
		if (blockEntity == null) {
			return blockDataProviders.first.get(blockState.getBlock());
		}
		return blockDataProviders.getMerged(blockState.getBlock(), blockEntity);
	}

	public List<IServerDataProvider<EntityAccessor>> entityDataProvidersOf(Entity entity) {
		if (entityTypeOperations().shouldHide(entity)) {
			return List.of();
		}
		return entityDataProviders.get(entity);
	}

	public void loadComplete() {
		blockDataProviders.loadComplete(priorities);
		entityDataProviders.loadComplete(priorities);
		itemStorageProviders.loadComplete(priorities);
		fluidStorageProviders.loadComplete(priorities);
		energyStorageProviders.loadComplete(priorities);
		progressProviders.loadComplete(priorities);
	}

	public void reloadOperations() {
		blockOperations.reload();
		entityTypeOperations.reload();
		mobEffectOperations.reload();
	}

	@Override
	public TargetOperationRepository<Block, IBlockState> blockOperations() {
		return blockOperations;
	}

	@Override
	public TargetOperationRepository<Class<? extends Entity>, Entity> entityTypeOperations() {
		return entityTypeOperations;
	}

	@Override
	public TargetOperationRepository<Potion, PotionEffect> mobEffectOperations() {
		return mobEffectOperations;
	}

	@Override
	public <T> void registerItemStorage(IServerExtensionProvider<ItemStack> provider, Class<? extends T> clazz) {
		itemStorageProviders.register(clazz, provider);
	}

	@Override
	public <T> void registerFluidStorage(IServerExtensionProvider<FluidView.Data> provider, Class<? extends T> clazz) {
		fluidStorageProviders.register(clazz, provider);
	}

	@Override
	public <T> void registerEnergyStorage(IServerExtensionProvider<EnergyView.Data> provider, Class<? extends T> clazz) {
		energyStorageProviders.register(clazz, provider);
	}

	@Override
	public <T> void registerProgress(IServerExtensionProvider<ProgressView.Data> provider, Class<? extends T> clazz) {
		progressProviders.register(clazz, provider);
	}
}
