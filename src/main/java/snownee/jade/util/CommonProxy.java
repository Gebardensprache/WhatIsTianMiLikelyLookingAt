package snownee.jade.util;

import java.io.File;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.server.MinecraftServer;

import net.minecraftforge.fluids.Fluid;

import net.minecraftforge.fluids.FluidRegistry;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.cache.Cache;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.MultiPartEntityPart;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntityMooshroom;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.boss.EntityDragon;
import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.inventory.IInventory;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import snownee.jade.Jade;
import snownee.jade.addon.universal.ItemCollector;
import snownee.jade.addon.universal.ItemIterator;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.TraceableException;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.EnergyView;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.command.JadeServerCommand;
import snownee.jade.impl.lookup.WrappedHierarchyLookup;
import snownee.jade.network.JadeNetwork;
import snownee.jade.network.ServerPayloadContext;

// Network stubs — B3 will implement SimpleNetworkWrapper
import snownee.jade.network.ClientHandshakePacket;
import snownee.jade.network.ReceiveDataPacket;
import snownee.jade.network.RequestBlockPacket;
import snownee.jade.network.RequestEntityPacket;
import snownee.jade.network.ServerHandshakePacket;
import snownee.jade.network.ShowOverlayPacket;

//@Mod is handled by B7
public final class CommonProxy {

	public static File getConfigDirectory() {
		return Loader.instance().getConfigDir();
	}

	public static boolean isCorrectToolForDrops(IBlockState state, EntityPlayer player, World level, BlockPos pos) {
		return ForgeHooks.canHarvestBlock(state.getBlock(), player, level, pos);
	}

	public static String getModIdFromItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return "minecraft";
		}
		String modid = stack.getItem().getCreatorModId(stack);
		if (modid != null && !"minecraft".equals(modid)) {
			return modid;
		}
		return "minecraft";
	}

	public static boolean isPhysicallyClient() {
		return FMLLaunchHandler.side().isClient();
	}

	public static ItemCollector<?> createItemCollector(Accessor<?> accessor, Cache<Object, ItemCollector<?>> containerCache) {
		Object target = accessor.getTarget();
		if (target instanceof EntityPlayer) {
			return ItemCollector.EMPTY;
		}
		// 1.12.2: the AbstractHorseAccess mixin was replaced with a jade_at.cfg entry
		// exposing AbstractHorse.horseChest directly (see B7-mixin).
		if (target instanceof AbstractHorse) {
			return new ItemCollector<>(new ItemIterator.ContainerItemIterator(
					o -> {
						if (o instanceof AbstractHorse horse) {
							return horse.horseChest;
						}
						return null;
					}, 2));
		}
		if (!(target instanceof TileEntityChest)) {
			try {
				IItemHandler storage = findItemHandler(accessor);
				if (storage != null) {
					return containerCache.get(storage, () -> new ItemCollector<>(JadeForgeUtils.fromItemHandler(storage, 0)));
				}
			} catch (Throwable e) {
				WailaExceptionHandler.handleErr(e, null, null);
			}
		}
		final IInventory container = findContainer(accessor);
		if (container != null) {
			if (container instanceof TileEntityChest) {
				return new ItemCollector<>(new ItemIterator.ContainerItemIterator(
						a -> {
							if (a.getTarget() instanceof TileEntityChest be) {
								if (be.getBlockType() instanceof BlockChest chestBlock) {
									IInventory compound = chestBlock.getLockableContainer(
											be.getWorld(),
											be.getPos());
									if (compound != null) {
										return compound;
									}
								}
								return be;
							}
							return null;
						}, 0));
			}
			return new ItemCollector<>(new ItemIterator.ContainerItemIterator(0));
		}
		return ItemCollector.EMPTY;
	}

	@Nullable
	public static List<ViewGroup<ItemStack>> containerGroup(IInventory container, Accessor<?> accessor) {
		return containerGroup(container, accessor, CommonProxy::findContainer);
	}

	@Nullable
	public static List<ViewGroup<ItemStack>> containerGroup(
			IInventory container,
			Accessor<?> accessor,
			Function<Accessor<?>, @Nullable IInventory> containerFinder) {
		try {
			return ItemStorageProvider.containerCache.get(
							container,
							() -> new ItemCollector<>(new ItemIterator.ContainerItemIterator(containerFinder, 0)))
					.update(accessor);
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	public static List<ViewGroup<ItemStack>> storageGroup(Object storage, Accessor<?> accessor) {
		return storageGroup(storage, accessor, CommonProxy::findItemHandler);
	}

	@Nullable
	public static List<ViewGroup<ItemStack>> storageGroup(
			Object storage,
			Accessor<?> accessor,
			Function<Accessor<?>, @Nullable Object> storageFinder) {
		try {
			//noinspection unchecked
			return ItemStorageProvider.containerCache.get(
					storage,
					() -> new ItemCollector<>(JadeForgeUtils.fromItemHandler(
							(IItemHandler) storage,
							0,
							(Function<Accessor<?>, @Nullable IItemHandler>) (Object) storageFinder))).update(
					accessor
			);
		} catch (Exception e) {
			return null;
		}
	}

	@Nullable
	public static IItemHandler findItemHandler(Accessor<?> accessor) {
		if (accessor instanceof BlockAccessor blockAccessor) {
			Object target = accessor.getTarget();
			if (target instanceof TileEntity te && te.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, blockAccessor.getSide())) {
				return te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, blockAccessor.getSide());
			}
		} else if (accessor instanceof EntityAccessor entityAccessor) {
			Entity entity = entityAccessor.getEntity();
			if (entity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null)) {
				return entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
			}
		}
		return null;
	}

	@Nullable
	public static IInventory findContainer(Accessor<?> accessor) {
		Object target = accessor.getTarget();
		if (target == null && accessor instanceof BlockAccessor blockAccessor &&
				blockAccessor.getBlock() instanceof IInventory) {
			return (IInventory) blockAccessor.getBlock();
		} else if (target instanceof IInventory container) {
			return container;
		}
		return null;
	}

	@Nullable
	public static List<ViewGroup<FluidView.Data>> wrapFluidStorage(Accessor<?> accessor) {
		IFluidHandler fluidHandler = getDefaultStorage(accessor, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
		if (fluidHandler != null) {
			return JadeForgeUtils.fromFluidHandler(fluidHandler);
		}
		return null;
	}

	@Nullable
	public static List<ViewGroup<EnergyView.Data>> wrapEnergyStorage(Accessor<?> accessor) {
		IEnergyStorage energyStorage = getDefaultStorage(accessor, CapabilityEnergy.ENERGY);
		if (energyStorage != null) {
			var group = new ViewGroup<>(List.of(new EnergyView.Data(energyStorage.getEnergyStored(), energyStorage.getMaxEnergyStored())));
			group.getExtraData().setString("Unit", "FE");
			return List.of(group);
		}
		return null;
	}

	public static boolean isDevEnv() {
		return FMLLaunchHandler.isDeobfuscatedEnvironment();
	}

	public static ResourceLocation getId(Block block) {
		return block.getRegistryName();
	}

	public static ResourceLocation getId(Class<? extends Entity> entityType) {
		return EntityList.getKey(entityType);
	}

	public static String getPlatformIdentifier() {
		return "forge";
	}

	public static boolean isBoss(Entity entity) {
		return entity instanceof EntityDragon || entity instanceof EntityWither;
	}

	public static ItemStack getBlockPickedResult(IBlockState state, EntityPlayer player, RayTraceResult hitResult) {
		BlockPos pos = hitResult.getBlockPos();
		ItemStack pick = state.getBlock().getPickBlock(state, hitResult, player.getEntityWorld(), pos, player);
		return pick != null ? pick : ItemStack.EMPTY;
	}

	public static ItemStack getEntityPickedResult(Entity entity, EntityPlayer player, RayTraceResult hitResult) {
		ItemStack pick = entity.getPickedResult(hitResult);
		return pick != null ? pick : ItemStack.EMPTY;
	}

	public static boolean isModLoaded(String modid) {
		return Loader.isModLoaded(modid);
	}

	public static Optional<String> getModVersion(String modid) {
		return Loader.instance().getIndexedModList().containsKey(modid) ?
				Optional.ofNullable(Loader.instance().getIndexedModList().get(modid).getVersion()) :
				Optional.empty();
	}

	public static List<Entrypoint> loadEntrypoints(ASMDataTable asmDataTable) {
		List<Entrypoint> entrypoints = Lists.newArrayList();
		Set<ModContainer> processedMods = Sets.newHashSet();
		Set<String> classNames = Sets.newHashSet();

		Set<ASMDataTable.ASMData> annotations = asmDataTable.getAll(WailaPlugin.class.getName());
		for (ASMDataTable.ASMData data : annotations) {
			String modid = data.getAnnotationInfo().getOrDefault("value", "").toString();
			ModContainer container = null;
			String candidateClassName = data.getClassName();
			// Find the mod container that owns this class. getOwnedPackages() returns
			// package names without a trailing dot, so strip it from the class package.
			for (ModContainer mc : Loader.instance().getActiveModList()) {
				if (mc.getOwnedPackages().contains(candidateClassName.substring(0, candidateClassName.lastIndexOf('.')))) {
					container = mc;
					break;
				}
			}
			if (container == null) {
				// Fallback: just use the first mod
				if (!Loader.instance().getActiveModList().isEmpty()) {
					container = Loader.instance().getActiveModList().getFirst();
				}
			}
			if (container != null && classNames.add(candidateClassName)) {
				entrypoints.add(new Entrypoint(container, modid, candidateClassName));
			}
		}
		return entrypoints;
	}

	public static String getFluidName(JadeFluidObject fluid) {
		FluidStack fs = toFluidStack(fluid);
		if (fs != null) {
			return fs.getLocalizedName();
		}
		return "";
	}

	@Nullable
	public static FluidStack toFluidStack(JadeFluidObject fluid) {
		if (fluid.isEmpty()) {
			return null;
		}
		Fluid forgeFluid = fluid.getFluid();
		if (forgeFluid == null) {
			return null;
		}
		long amount = fluid.getAmount();
		if (amount > Integer.MAX_VALUE) {
			amount = Integer.MAX_VALUE;
		}
		return new FluidStack(forgeFluid, (int) amount);
	}

	public static boolean isMultipartEntity(Entity target) {
		return target instanceof MultiPartEntityPart;
	}

	public static Entity wrapPartEntityParent(Entity target) {
		if (target instanceof MultiPartEntityPart part && part.parent instanceof Entity parent) {
			return parent;
		}
		return target;
	}

	@SuppressWarnings("NullableProblems")
	public static int getPartEntityIndex(Entity entity) {
		if (!(entity instanceof MultiPartEntityPart part)) {
			return -1;
		}
		if (part.parent instanceof EntityDragon dragon) {
			MultiPartEntityPart[] parts = dragon.dragonPartArray;
			for (int i = 0; i < parts.length; i++) {
				if (parts[i] == part) {
					return i;
				}
			}
		}
		return -1;
	}

	@Nullable
	public static Entity getPartEntity(@Nullable Entity parent, int index) {
		if (parent == null) {
			return null;
		}
		if (index < 0) {
			return parent;
		}
		if (parent instanceof EntityDragon dragon) {
			if (dragon.dragonPartArray != null && index < dragon.dragonPartArray.length) {
				return dragon.dragonPartArray[index];
			}
		}
		return parent;
	}

	@Nullable
	public static <T> T getDefaultStorage(
			Accessor<?> accessor,
			Capability<T> capability) {
		if (accessor instanceof BlockAccessor blockAccessor) {
			Object target = accessor.getTarget();
			if (target instanceof TileEntity te && te.hasCapability(capability, blockAccessor.getSide())) {
				return te.getCapability(capability, blockAccessor.getSide());
			}
		} else if (accessor instanceof EntityAccessor entityAccessor) {
			Entity entity = entityAccessor.getEntity();
			if (entity.hasCapability(capability, null)) {
				return entity.getCapability(capability, null);
			}
		}
		return null;
	}

	public static <T> boolean hasDefaultStorage(
			Accessor<?> accessor,
			Capability<T> capability) {
		if (accessor instanceof BlockAccessor || accessor instanceof EntityAccessor) {
			return getDefaultStorage(accessor, capability) != null;
		}
		return true;
	}

	public static boolean hasDefaultItemStorage(Accessor<?> accessor) {
		if (accessor.getTarget() == null && accessor instanceof BlockAccessor blockAccessor &&
				blockAccessor.getBlock() instanceof IInventory) {
			return true;
		}
		return hasDefaultStorage(accessor, CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
	}

	public static boolean hasDefaultFluidStorage(Accessor<?> accessor) {
		return hasDefaultStorage(accessor, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
	}

	public static boolean hasDefaultEnergyStorage(Accessor<?> accessor) {
		return hasDefaultStorage(accessor, CapabilityEnergy.ENERGY);
	}

	public static long bucketVolume() {
		return 1000;
	}

	public static long blockVolume() {
		return 1000;
	}

	public static void registerNetwork() {
		JadeNetwork.init();
	}

	/**
	 * Stamps the effect-update timestamp on newly added potion effects.
	 *
	 * <p>1.12.2: replaces both {@code LivingEntityMixin}'s {@code @Inject} into
	 * {@code LivingEntity.addEffect} and {@code MobEffectInstanceMixin}'s {@code @Inject} into
	 * {@code MobEffectInstance.onEffectAdded}, neither of which has a 1.12.2 counterpart --
	 * {@link net.minecraft.potion.PotionEffect} has no {@code onEffectAdded} method at all.
	 * {@link net.minecraftforge.event.entity.living.PotionEvent.PotionAddedEvent} is fired from
	 * {@code EntityLivingBase.addPotionEffect} and carries the new effect instance, so both
	 * timestamps can be stamped on it directly.
	 *
	 * <p>Sets update-time for every add/refresh and add-time only for a new effect, matching
	 * upstream's {@code onEffectAdded}. {@code StatusEffectsProvider} reads both; leaving
	 * add-time unset would pin it at 0 forever.
	 *
	 * <p>Server-side only, matching the modern {@code !level().isClientSide()} guard.
	 *
	 * @param event the potion-added event
	 */
	@SubscribeEvent
	public static void onPotionAdded(PotionEvent.PotionAddedEvent event) {
		if (event.getEntityLiving().world.isRemote) {
			return;
		}
		PotionEffect effect = event.getPotionEffect();
		if (effect instanceof JadeMobEffectInstance jadeEffect) {
			long time = System.currentTimeMillis();
			if (event.getOldPotionEffect() == null) {
				jadeEffect.jade$setAddTime(time);
			}
			jadeEffect.jade$setUpdateTime(time);
		}
	}

	public static void sendPacket(EntityPlayerMP player, IMessage payload) {
		JadeNetwork.sendTo(payload, player);
	}

	/**
	 * Sends a chat message to a command sender.
	 *
	 * <p>1.12.2: {@code ICommandSender.sendMessage} has a default implementation but
	 * {@code EntityPlayer.sendMessage} requires a non-null component.
	 *
	 * @param sender the recipient
	 * @param message the component to send
	 */
	public static void sendMessage(ICommandSender sender, ITextComponent message) {
		sender.sendMessage(message);
	}

	/**
	 * Runs a server-side packet handler on the main server thread.
	 *
	 * <p>1.12.2: replaces the modern context-scoped executor. The netty worker thread
	 * hands the work to the server thread so handlers can touch world state safely.
	 *
	 * @param context the packet context whose player identifies the target server
	 * @param runnable the handler body
	 */
	public static void runWithContext(ServerPayloadContext context, Runnable runnable) {
		MinecraftServer server = context.player().getServer();
		if (server == null) {
			runnable.run();
		} else {
			server.addScheduledTask(runnable);
		}
	}

	/**
	 * Marks whether a player has the Jade client connected.
	 *
	 * <p>1.12.2: delegates to the {@link JadeServerPlayer} duck interface that
	 * {@code ServerPlayerMixin} implements on {@code EntityPlayerMP}, same as upstream. An
	 * earlier revision kept a separate weak set here to avoid the mixin; that left two stores
	 * for one fact, and because {@code JadeServerCommand} reads the duck interface while the
	 * handshake wrote the set, the command filtered out every player.
	 *
	 * @param player the player
	 * @param connected whether the Jade client handshake completed
	 */
	public static void setConnected(EntityPlayerMP player, boolean connected) {
		((JadeServerPlayer) player).jade$setConnected(connected);
	}

	/**
	 * Returns whether a player has the Jade client connected.
	 *
	 * @param player the player
	 * @return {@code true} if the Jade client handshake completed
	 */
	public static boolean isConnected(EntityPlayerMP player) {
		return ((JadeServerPlayer) player).jade$isConnected();
	}

	public static String defaultEnergyUnit() {
		return "FE";
	}

	public static <T> Map.@Nullable Entry<ResourceLocation, List<ViewGroup<T>>> getServerExtensionData(
			Accessor<?> accessor,
			WrappedHierarchyLookup<IServerExtensionProvider<T>> lookup) {
		for (var provider : lookup.wrappedGet(accessor)) {
			List<ViewGroup<T>> groups;
			try {
				groups = provider.getGroups(accessor);
			} catch (Exception e) {
				WailaExceptionHandler.handleErr(e, provider, null);
				continue;
			}
			if (groups != null) {
				return Map.entry(provider.getUid(), groups);
			}
		}
		return null;
	}

	// 1.12.2 FML lifecycle — called from @Mod class (B7)
	public static void preInit(FMLPreInitializationEvent event) {
		// 1.12.2: registers onPotionAdded (the LivingEntityMixin replacement). Fired on
		// both sides by Forge; the handler itself guards on world.isRemote.
		MinecraftForge.EVENT_BUS.register(CommonProxy.class);
		// Config directory setup etc. for B7
	}

	public static boolean isCorrectConditions(
			List<LootCondition> conditions,
			ItemStack toolItem) {
		// Simplified — 1.12.2 loot conditions API is different; just check if tool is effective
		if (conditions.isEmpty()) {
			return true;
		}
		return !toolItem.isEmpty();
	}

	@FunctionalInterface
	public interface TagsUpdatedListener {
		// 1.12.2: upstream passes a HolderLookup.Provider; the only real need is loot-table
		// lookup, so pass the server instead. `client` keeps upstream's call-site shape.
		void onTagsUpdated(@Nullable MinecraftServer server, boolean client);
	}

	private static final List<TagsUpdatedListener> TAGS_UPDATED_LISTENERS = new ArrayList<>();

	public static void registerTagsUpdatedListener(TagsUpdatedListener listener) {
		TAGS_UPDATED_LISTENERS.add(listener);
	}

	public static void fireTagsUpdated(@Nullable MinecraftServer server, boolean client) {
		for (TagsUpdatedListener listener : TAGS_UPDATED_LISTENERS) {
			listener.onTagsUpdated(server, client);
		}
	}

	public record Entrypoint(ModContainer container, String requiredMod, String className) {
		public String modId() {
			return container.getModId();
		}

		public String modName() {
			return container.getName();
		}

		public IWailaPlugin newInstance() {
			try {
				return (IWailaPlugin) Class.forName(className).getDeclaredConstructor().newInstance();
			} catch (Throwable e) {
				throwError("Failed to instantiate plugin class");
				throw new AssertionError();
			}
		}

		public void throwError(String message, @Nullable Throwable cause) {
			message = "Error in plugin class %s from %s: %s".formatted(className(), modName(), message);
			if (cause == null) {
				cause = new IllegalStateException(message);
			} else {
				cause = new IllegalStateException(message, cause);
			}
			throw new TraceableException(cause, modId());
		}

		public void throwError(String message) {
			throwError(message, null);
		}
	}
}
