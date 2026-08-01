package snownee.jade;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import snownee.jade.addon.core.ModNameProvider;
import snownee.jade.addon.harvest.LootTableMineableCollector;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.command.JadeServerCommand;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.impl.config.WailaConfig;
import snownee.jade.test.ExamplePlugin;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.JadeCodecs;
import snownee.jade.util.JsonConfig;

/**
 * The Forge 1.12.2 entrypoint of Jade.
 * <p>
 * B7 wiring: pre-init captures the {@link ASMDataTable} and hands it to the plugin
 * discovery machinery, init initializes the physical client, load-complete runs the
 * plugin reset/retry + finalization sequence directly (FMLLoadCompleteEvent is a marker
 * event on 1.12.2 and has no {@code enqueueWork}), and the server lifecycle owns the
 * reach game rule and the tag-refresh signal. The HWYLA/TOP compatibility bridges no
 * longer live here: they are standalone shim mods whose {@code @WailaPlugin} entrypoints
 * are discovered by {@link #loadPlugins} just like any other plugin.
 */
@Mod(
		modid = Jade.ID,
		name = "Jade",
//		dependencies = "after:*",
		acceptableRemoteVersions = "*",
		guiFactory = "snownee.jade.gui.JadeGuiFactory")
public class Jade {
	public static final String ID = "jade";
	public static final String PROTOCOL_VERSION = "9";
	public static final Logger LOGGER = LogManager.getLogger(ID);
	public static final Set<String> DISABLED_PLUGINS = Sets.newHashSet();
	private static final String MAX_POSITION_DEVIATION = "jade:max_position_deviation";
	private static final Supplier<JsonConfig<WailaConfig.Root>> rootConfig = Suppliers.memoize(() -> new JsonConfig<>(
			ID + "/" + ID,
			WailaConfig.Root.CODEC,
			WailaConfig::fixData));
	private static List<JsonConfig<? extends WailaConfig>> configs = List.of();

	/** Retained from {@link FMLServerStartingEvent}; {@link FMLServerStartedEvent} has no server accessor. */
	private static @Nullable MinecraftServer server;
	private static boolean tagsListenerRegistered;

	/** Captured during pre-init; required by {@link CommonProxy#loadEntrypoints}. */
	private static @Nullable ASMDataTable asmData;

	private static JsonConfig<? extends WailaConfig> configHolder() {
		WailaConfig.Root root = rootConfig();
		if (root.isEnableProfiles() && root.profileIndex > 0 && root.profileIndex < configs.size()) {
			return configs.get(root.profileIndex);
		}
		return rootConfig.get();
	}

	/**
	 * addons: Use {@link IWailaConfig#get()}
	 */
	public static WailaConfig config() {
		return configHolder().get();
	}

	public static void saveConfig() {
		configHolder().save();
		if (config() != rootConfig()) {
			rootConfig.get().save();
		}
	}

	public static void invalidateConfig() {
		configHolder().invalidate();
	}

	public static WailaConfig.History history() {
		return rootConfig().history;
	}

	public static void resetConfig() {
		int themesHash = history().themesHash;
		Preconditions.checkState(configHolder().getFile().delete());
		invalidateConfig();
		history().themesHash = themesHash;
		configHolder().save();
	}

	public static WailaConfig.Root rootConfig() {
		return rootConfig.get().get();
	}

	/**
	 * Finalizes registrations and profiles after the plugin loading phase.
	 * <p>
	 * Registered exactly once from {@link #onLoadComplete}. The HWYLA/TOP compatibility
	 * bridges used to sit between the last plugin reset and the priority sort; they now
	 * register through the standard plugin loader (the shims' {@code @WailaPlugin}
	 * entrypoints), so this runs directly after {@link #loadPlugins(ASMDataTable)}.
	 */
	private static void loadComplete() {
		if (CommonProxy.isDevEnv()) {
			try {
				IWailaPlugin plugin = new ExamplePlugin();
				plugin.register(WailaCommonRegistration.instance());
				if (CommonProxy.isPhysicallyClient()) {
					plugin.registerClient(WailaClientRegistration.instance());
				}
			} catch (Throwable _) {
			}
		}

		Set<ResourceLocation> extraKeys;
		if (CommonProxy.isPhysicallyClient()) {
			extraKeys = WailaClientRegistration.instance().getConfigKeys();
		} else {
			extraKeys = Set.of();
		}
		WailaCommonRegistration.instance().priorities.sort(extraKeys);
		WailaCommonRegistration.instance().loadComplete();
		registerTagsUpdatedListenerOnce();
		if (CommonProxy.isPhysicallyClient()) {
			WailaClientRegistration.instance().loadComplete();

			Codec<WailaConfig> codec = WailaConfig.MAP_CODEC.codec();
			ImmutableList.Builder<JsonConfig<? extends WailaConfig>> list = ImmutableList.builder();
			list.add(rootConfig.get());
			Supplier<WailaConfig> defaultFactory = () -> JadeCodecs.createFromEmptyMap(codec);
			for (int i = 1; i < 4; ++i) {
				Supplier<WailaConfig> factory = getProfilePreset(defaultFactory, i);
				list.add(new JsonConfig<>("%s/profiles/%s/%s".formatted(ID, i, ID), codec, WailaConfig::fixData, factory));
			}
			configs = list.build();
			rootConfig().history.checkNewUser(CommonProxy.getConfigDirectory().getAbsolutePath().hashCode());
			rootConfig().fixData();
			WailaConfig.init();
			for (JsonConfig<? extends WailaConfig> config : configs) {
				config.save();
			}
			JadeClient.refreshKeyState();
		}
	}

	private static void registerTagsUpdatedListenerOnce() {
		if (tagsListenerRegistered) {
			return;
		}
		tagsListenerRegistered = true;
		// B5a contract: reload operations; on the server side also refresh loot-table mineability.
		CommonProxy.registerTagsUpdatedListener((server, client) -> {
			WailaCommonRegistration.instance().reloadOperations();
			if (!client) {
				LootTableMineableCollector.onTagsUpdated(server, false);
			}
		});
	}

	private static Supplier<WailaConfig> getProfilePreset(Supplier<WailaConfig> defaultFactory, int i) {
		Supplier<WailaConfig> factory = defaultFactory;
		if (i == 1) {
			factory = () -> {
				WailaConfig config = defaultFactory.get();
				config.setName("@jade.profile_preset.accessibility");
				config.accessibility().setEnableAccessibilityPlugin(true);
				config.overlay().setAnimation(false);
				config.overlay().setAlpha(1);
				config.plugin().set(JadeIds.CORE_BLOCK_FACE, true);
				config.plugin().set(JadeIds.CORE_MOD_NAME, ModNameProvider.Mode.OFF);
				return config;
			};
		} else if (i == 2) {
			factory = () -> {
				WailaConfig config = defaultFactory.get();
				config.setName("@jade.profile_preset.minimalism");
				config.general().setDisplayMode(IWailaConfig.DisplayMode.LITE);
				config.general().setBossBarOverlapMode(IWailaConfig.BossBarOverlapMode.HIDE_TOOLTIP);
				config.overlay().setAlpha(0);
				config.overlay().activeTheme = JadeIds.JADE("dark/slim");
				config.overlay().setIconMode(IWailaConfig.IconMode.INLINE);
				config.plugin().set(JadeIds.MC_BREAKING_PROGRESS, false);
				config.plugin().set(JadeIds.MC_HARVEST_TOOL, false);
				config.plugin().set(JadeIds.MC_ITEM_TOOLTIP, false);
				return config;
			};
		}
		return factory;
	}

	public static List<JsonConfig<? extends WailaConfig>> configs() {
		return configs;
	}

	public static void useProfile(int index) {
		rootConfig().setEnableProfiles(true);
		rootConfig().profileIndex = index;
		rootConfig.get().save();
		JadeClient.refreshKeyState();
	}

	public static void saveProfile(int index) {
		JsonConfig<? extends WailaConfig> dest = configs().get(index);
		configHolder().saveTo(dest.getFile());
		dest.invalidate();
	}

	/**
	 * Runs the plugin reset/retry phase. Called from load-complete with the ASM data
	 * captured during pre-init; kept public for the parked GUI's reload-plugins action.
	 */
	public static void loadPlugins(ASMDataTable asmData) {
		List<CommonProxy.Entrypoint> entrypoints = CommonProxy.loadEntrypoints(asmData);
		Set<String> disabledClasses = DISABLED_PLUGINS;
		JsonConfig<List<String>> config = new JsonConfig<>(
				ID + "/disabled_plugins",
				// 1.12.2: ExtraCodecs.NON_EMPTY_STRING does not exist here; plain STRING keeps
				// the file format (a JSON list of plugin class names) identical to upstream.
				Codec.STRING.listOf().optionalFieldOf("values", List.of()).codec(),
				null,
				List::of);
		if (config.getFile().exists() && !config.get().isEmpty()) {
			disabledClasses = Sets.newHashSet(disabledClasses);
			disabledClasses.addAll(config.get());
		}
		Set<String> erroneousClasses = Sets.newHashSet();
		loadPlugins(entrypoints, disabledClasses, erroneousClasses, Set.of());
		if (!erroneousClasses.isEmpty()) {
			LOGGER.info("Trying to load plugins again without erroneous plugins");
			loadPlugins(entrypoints, disabledClasses, erroneousClasses, erroneousClasses);
		}
	}

	/**
	 * No-arg {@code loadPlugins()} for the parked GUI's reload-plugins action. Uses the
	 * ASM data captured during pre-init, re-running the same reset/retry phase as
	 * {@link #loadPlugins(ASMDataTable)} and re-finalizing registrations (modern
	 * {@code loadPlugins()} also ends in {@code loadComplete()}).
	 */
	public static void loadPlugins() {
		loadPlugins(asmData);
		loadComplete();
	}

	private static void loadPlugins(
			List<CommonProxy.Entrypoint> entrypoints,
			Set<String> disabledClasses,
			Set<String> erroneousClasses,
			Set<String> excludedClasses) {
		WailaCommonRegistration.reset();
		if (CommonProxy.isPhysicallyClient()) {
			WailaClientRegistration.reset();
		}
		Set<String> classes = Sets.newHashSet();
		Stopwatch stopwatch = null;
		if (CommonProxy.isDevEnv()) {
			stopwatch = Stopwatch.createUnstarted();
		}
		for (CommonProxy.Entrypoint entrypoint : entrypoints) {
			String className = entrypoint.className();
			try {
				if (disabledClasses.contains(className)) {
					LOGGER.info("Skipping disabled plugin: {}", className);
					continue;
				}
				if (excludedClasses.contains(className)) {
					continue;
				}
				if (className.startsWith("snownee.jade.") && !entrypoint.modId().equals(ID)) {
					entrypoint.throwError("Built-in plugin registered by non-Jade mod");
				}
				String requiredMod = entrypoint.requiredMod();
				if (!requiredMod.isEmpty() && !CommonProxy.isModLoaded(requiredMod)) {
					continue;
				}
				if (!classes.add(className)) {
					entrypoint.throwError("Duplicate plugin class");
				}
				IWailaPlugin plugin = entrypoint.newInstance();
				LOGGER.info("Start loading plugin from {}: {}", entrypoint.modName(), className);
				if (stopwatch != null) {
					stopwatch.reset().start();
				}
				WailaCommonRegistration common = WailaCommonRegistration.instance();
				plugin.register(common);
				if (CommonProxy.isPhysicallyClient()) {
					WailaClientRegistration client = WailaClientRegistration.instance();
					plugin.registerClient(client);
				}
				if (stopwatch != null) {
					LOGGER.info("{} loaded: {}", className, stopwatch.stop());
				}
			} catch (Throwable e) {
				LOGGER.error("Failed to load plugin from {}: {}", entrypoint.modName(), className, e);
				if (CommonProxy.isDevEnv() || entrypoint.modId().equals(ID)) {
					throw e;
				}
				erroneousClasses.add(className);
			}
		}
	}

	/**
	 * Returns whether the target position is out of the player's reach.
	 * <p>
	 * 1.12.2: singleplayer owner check through the server owner name (no
	 * {@code NameAndId} equivalent), deviation rule read via {@code GameRules.getInt}.
	 */
	public static boolean isOutOfReach(EntityPlayerMP player, BlockPos pos, double baseReach) {
		MinecraftServer server = player.getServer();
		if (server != null
				&& server.isSinglePlayer()
				&& player.getName().equals(server.getServerOwner())) {
			return false;
		}

		WorldServer world = player.getServerWorld();
		double reach = baseReach
				+ world.getGameRules().getInt(MAX_POSITION_DEVIATION);
		return pos.distanceSq(player.getPosition()) > reach * reach;
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		asmData = event.getAsmData();
		CommonProxy.preInit(event);
		CommonProxy.registerNetwork();
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		if (CommonProxy.isPhysicallyClient()) {
			ClientProxy.init();
		}
	}

	@Mod.EventHandler
	public void onLoadComplete(FMLLoadCompleteEvent event) {
		// The HWYLA/TOP compat bridges now come through the standard plugin loading: the shims'
		// @WailaPlugin entrypoints are discovered by loadPlugins(asmData) just like any other plugin.
		loadPlugins(asmData);
		loadComplete();
	}

	@Mod.EventHandler
	public void onServerStarting(FMLServerStartingEvent event) {
		server = event.getServer();
		event.registerServerCommand(new JadeServerCommand());
	}

	@Mod.EventHandler
	public void onServerStarted(FMLServerStartedEvent event) {
		MinecraftServer server = Jade.server;
		if (server == null) {
			return;
		}
		for (WorldServer world : server.worlds) {
			if (world != null && !world.getGameRules().hasRule(MAX_POSITION_DEVIATION)) {
				world.getGameRules().addGameRule(
						MAX_POSITION_DEVIATION,
						"21",
						GameRules.ValueType.NUMERICAL_VALUE);
			}
		}
		CommonProxy.fireTagsUpdated(server, false);
	}

}
