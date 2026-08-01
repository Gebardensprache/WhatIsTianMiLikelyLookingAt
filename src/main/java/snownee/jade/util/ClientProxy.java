package snownee.jade.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.block.BlockBookshelf;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Keyboard;

import com.google.common.base.Strings;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.GameType;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.api.Accessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeKeys;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.command.JadeClientCommand;
import snownee.jade.gui.PreviewOptionsScreen;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.ui.FluidStackElement;
import snownee.jade.impl.theme.ThemeHelper;
import snownee.jade.network.ClientHandshakePacket;
import snownee.jade.network.JadeNetwork;
import snownee.jade.overlay.DatapackBlockManager;
import snownee.jade.overlay.OverlayRenderer;

@SideOnly(Side.CLIENT)
public final class ClientProxy {

	public static JadeMetadata metadata = new ForgeJadeMetadata();
	private static final List<KeyBinding> keys = Lists.newArrayList();
	private static boolean bossbarShown;
	private static int bossbarHeight;
	private static boolean pendingHandshake;

	public static Optional<String> getModName(String namespace, boolean translate) {
		if (translate) {
			String modMenuKey = "modmenu.nameTranslation.%s".formatted(namespace);
			if (JadeUI.hasTranslation(modMenuKey)) {
				return Optional.of(I18n.format(modMenuKey));
			}
		}
		// 1.12.2: Use ModList equivalent
		return Loader.instance().getIndexedModList().containsKey(namespace)
				? Optional.ofNullable(Loader.instance().getIndexedModList().get(namespace).getName())
				: Optional.empty();
	}

	public static void registerCommands() {
		ClientCommandHandler.instance.registerCommand(new JadeClientCommand());
	}

	@SubscribeEvent
	public static void onEntityJoin(EntityJoinWorldEvent event) {
		DatapackBlockManager.onEntityJoin(event.getEntity());
		if (pendingHandshake && event.getEntity() == Minecraft.getMinecraft().player) {
			// 1.12.2: client player joins the world (loadWorld -> spawnEntity) on the client
			// thread AFTER the connection and mc.player exist — the faithful translation of
			// upstream's ClientPlayerNetworkEvent.LoggingIn. Send the handshake here.
			pendingHandshake = false;
			sendPacket(new ClientHandshakePacket(Jade.PROTOCOL_VERSION));
		}
	}

	@SubscribeEvent
	public static void onEntityLeave(EntityEvent event) {
		// 1.12.2: no EntityLeaveWorldEvent exists; the modern equivalent is
		// EntityJoinWorldEvent's inverse. Forge fires EntityLeaveWorldEvent only in
		// newer versions — 1.12.2 unloads track the world directly. DatapackBlockManager
		// is an intentional no-op here, so the event type itself does not matter.
		DatapackBlockManager.onEntityLeave(event.getEntity());
	}

	/**
	 * Appends the owning mod's name to item tooltips.
	 *
	 * <p>1.12.2: replaces both {@code GuiGraphicsExtractorMixin.jade$appendModName} and
	 * {@code CreativeModeInventoryScreenMixin}. Forge's {@code ItemTooltipEvent} fires for
	 * every tooltip including the creative inventory, so one handler covers both.
	 *
	 * <p>1.12.2: the mixin's creative-tab dedup (suppressing the mod-name line when the
	 * hovered tab's own name already starts with the mod name) is dropped. It relied on
	 * {@code @Local}/{@code @Share} captures of {@code CreativeModeTabs.tabs()} internals
	 * that have no 1.12.2 counterpart, and {@code ItemTooltipEvent} carries no tab context.
	 * Net effect: inside a mod's own creative tab the mod name may be shown redundantly.
	 * Users can turn the line off entirely via {@code showItemModNameTooltip}.
	 *
	 * @param event the item tooltip event
	 */
	@SubscribeEvent
	public static void onTooltipEvent(ItemTooltipEvent event) {
		if (event.getItemStack().isEmpty()) {
			return;
		}
		ITextComponent name = JadeClient.appendModName(event.getItemStack());
		if (name != null) {
			// 1.12.2: ItemTooltipEvent carries List<String>, not List<ITextComponent>.
			event.getToolTip().add(1, name.getFormattedText());
		}
	}

	public static void onRenderTick(float tickDelta) {
		try {
			OverlayRenderer.renderOverlay478757(tickDelta);
		} catch (Throwable e) {
			WailaExceptionHandler.handleErr(e, null, null);
		} finally {
			bossbarShown = false;
		}
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			try {
				if (pendingHandshake && Minecraft.getMinecraft().getConnection() != null) {
					// Fallback for any join path where EntityJoinWorldEvent for the client
					// player did not fire (e.g. respawn/dimension change ordering): once the
					// client->server network manager exists, send the handshake.
					pendingHandshake = false;
					sendPacket(new ClientHandshakePacket(Jade.PROTOCOL_VERSION));
				}
				JadeClient.tickHandler().tickClient();
			} catch (Throwable e) {
				WailaExceptionHandler.handleErr(e, null, null);
			}
		}
	}

	@SubscribeEvent
	public static void onServerConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		// 1.12.2: mirrors upstream ClientProxy's client-join handshake send. Without
		// this the server never replies with ServerHandshakePacket, so serverConnected
		// stays false, setServerConfig is never called, and the provider id spaces are
		// never remapped — RequestBlockPacket then sends indices the server resolves
		// against a different ordering.
		//
		// 1.12.2 CAVEAT: PlayerEvent.PlayerLoggedInEvent is fired from
		// FMLCommonHandler.firePlayerLoggedIn on the SERVER only, so on a dedicated
		// server the client never sees it. The client-side equivalent is
		// FMLNetworkEvent.ClientConnectedToServerEvent (and
		// ClientDisconnectionFromServerEvent for the leave hook below).
		//
		// The event fires on the netty thread before Minecraft.player exists, so the
		// packet cannot be sent here — see the deferred send in onClientTick.
		pendingHandshake = true;
	}

	@SubscribeEvent
	public static void onServerDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
		// 1.12.2: must clear last-object and tick-handler state too, hence the full
		// ObjectDataCenter.disconnect() rather than just flipping serverConnected.
		pendingHandshake = false;
		ObjectDataCenter.disconnect();
		WailaClientRegistration.instance().setServerConfig(Map.of());
	}

	@SubscribeEvent
	public static void onKeyPressed(InputEvent.KeyInputEvent event) {
		JadeClient.onKeyPressed();
	}

	@SubscribeEvent
	public static void onDrawScreen(RenderGameOverlayEvent.Post event) {
		if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
			try {
				OverlayRenderer.renderOverlay478757(event.getPartialTicks());
			} catch (Throwable e) {
				WailaExceptionHandler.handleErr(e, null, null);
			} finally {
				bossbarShown = false;
			}
		}
	}

	public static KeyBinding registerKeyBinding(String desc, int defaultKey) {
		KeyBinding key = new KeyBinding(
				"key.jade." + desc,
				defaultKey,
				"key.jade.category");
		keys.add(key);
		return key;
	}

	public static boolean shouldRegisterRecipeViewerKeys() {
		// 1.12.2: the modern Polydex/REI ports do not exist; only the JEI/HEI plugin is
		// wired. Registering the keys is harmless even when JEI is absent -- the lookups
		// no-op with no plugin -- but matches the modern gating on a loaded recipe viewer.
		return CommonProxy.isModLoaded("jei");
	}

	public static Element elementFromLiquid(IBlockState blockState) {
		Fluid fluid = null;
		if (blockState.getBlock() instanceof BlockLiquid) {
			fluid = FluidRegistry.lookupFluidForBlock(blockState.getBlock());
		}
		if (fluid != null) {
			return new FluidStackElement(JadeFluidObject.of(fluid));
		}
		return new FluidStackElement(JadeFluidObject.empty());
	}

	/**
	 * Suppresses vanilla boss bars while Jade's overlay is shown.
	 *
	 * <p>1.12.2: replaces {@code BossHealthOverlayMixin}'s cancelling {@code @Inject}.
	 * {@code GuiIngameForge.renderBossHealth} fires a {@code Pre} with
	 * {@code ElementType.BOSSHEALTH} before drawing all bars. The eligibility decision
	 * must come from {@link OverlayRenderer#shouldRender()} -- a side-effect-free
	 * current-frame check -- and never from the previous frame's {@code shown} flag.
	 * {@code RenderGameOverlayEvent.BossInfo} (a {@code Pre} subclass) is kept only for
	 * the per-bar {@code PUSH_DOWN} position capture below.
	 *
	 * @param event the pre-boss-health event
	 */
	@SubscribeEvent
	public static void drawBossBarPre(RenderGameOverlayEvent.Pre event) {
		if (event.getType() != RenderGameOverlayEvent.ElementType.BOSSHEALTH) {
			return;
		}
		IWailaConfig.BossBarOverlapMode mode = IWailaConfig.get().general().getBossBarOverlapMode();
		if (mode == IWailaConfig.BossBarOverlapMode.HIDE_BOSS_BAR && OverlayRenderer.shouldShow()) {
			event.setCanceled(true);
		}
	}

	/**
	 * Captures the boss-bar stack height so the overlay can be pushed below it.
	 *
	 * <p>1.12.2: replaces {@code BossHealthOverlayMixin}'s {@code jade$captureHeight}
	 * {@code @Inject}, which read a {@code yOffset} local. {@code BossInfo} exposes
	 * {@code getY()} and {@code getIncrement()} directly, so the position capture is
	 * fully preserved rather than dropped.
	 *
	 * @param event the per-boss-bar pre-render event
	 */
	@SubscribeEvent
	public static void drawBossBarPostInternal(RenderGameOverlayEvent.BossInfo event) {
		IWailaConfig.BossBarOverlapMode mode = IWailaConfig.get().general().getBossBarOverlapMode();
		if (mode == IWailaConfig.BossBarOverlapMode.PUSH_DOWN) {
			bossbarHeight = event.getY() + event.getIncrement();
			bossbarShown = true;
		}
	}

	/**
	 * Removed in B7: {@code EntityViewRenderEvent.RenderFogEvent} does not run under
	 * special fog branches and only fed {@code JadeClient.limitMobEffectFog}, which had
	 * no live callers. The fog-limit feature is dead on 1.12.2; see the B7-mixin review.
	 */

	@Nullable
	public static Rect2f getBossBarRect() {
		if (!bossbarShown) {
			return null;
		}
		int i = new ScaledResolution(Minecraft.getMinecraft()).getScaledWidth();
		int k = i / 2 - 91;
		return new Rect2f(k, 0, 182, bossbarHeight - 12);
	}

	public static boolean isShowDetailsPressed() {
		// 1.12.2: the showDetails binding is registered with the 3-arg KeyBinding
		// constructor, so its KeyModifier is NONE and NONE.isActive() is hardcoded true.
		// The vanilla event loop drives setKeyBindState(KEY_LSHIFT, ...) per key event,
		// and the pressed flag is shared across every HASH entry for a key code, so the
		// binding's isKeyDown() can read stale/raced state and report the key as down
		// only on release. Read the physical key state instead, like JadeUI.hasShiftDown().
		return JadeUI.hasShiftDown();
	}

	public static boolean shouldHideWithGui(Minecraft mc, @Nullable GuiScreen screen) {
		return screen != null && !shouldShowBeforeGui(mc, screen) && !shouldShowAfterGui(mc, screen);
	}

	public static boolean shouldShowAfterGui(Minecraft mc, GuiScreen screen) {
		return screen instanceof PreviewOptionsScreen || JadeUI.isPinned();
	}

	public static boolean shouldShowBeforeGui(Minecraft mc, GuiScreen screen) {
		if (mc.world == null || screen == null) {
			return false;
		}
		IWailaConfig.General config = IWailaConfig.get().general();
		return !config.shouldHideFromGUIs();
	}

	public static void getFluidSpriteAndColor(JadeFluidObject fluid, BiConsumer<@Nullable TextureAtlasSprite, Integer> consumer) {
		// 1.12.2: Simplified fluid sprite/color lookup
		Fluid forgeFluid = fluid.getFluid();
		if (forgeFluid == null) {
			consumer.accept(null, -1);
			return;
		}
		TextureAtlasSprite sprite = Minecraft.getMinecraft().getTextureMapBlocks()
				.getAtlasSprite(forgeFluid.getStill().toString());
		consumer.accept(sprite, forgeFluid.getColor());
	}

	public static void renderItemDecorationsExtra(FontRenderer font, ItemStack stack, int x, int y) {
		// 1.12.2: No ItemDecoratorHandler; forge itself handles item decorations
	}

	public static GameType getGameMode() {
		NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
		if (connection != null) {
			PlayerControllerMP controller = Minecraft.getMinecraft().playerController;
			if (controller != null) {
				return controller.getCurrentGameType();
			}
		}
		return GameType.SURVIVAL;
	}

	@Nullable
	public static <IN, OUT> List<ClientViewGroup<OUT>> mapToClientGroups(
			Accessor<?> accessor,
			ResourceLocation key,
			DataCodec<Map.Entry<ResourceLocation, List<ViewGroup<IN>>>> codec,
			Function<ResourceLocation, @Nullable IClientExtensionProvider<IN, OUT>> mapper,
			ITooltip tooltip) {
		NBTTagCompound serverData = accessor.getServerData();
		if (serverData == null || !serverData.hasKey(key.toString())) {
			return null;
		}
		NBTBase tag = serverData.getTag(key.toString());
		Map.Entry<ResourceLocation, List<ViewGroup<IN>>> entry = accessor.decodeFromNbt(codec, tag).orElse(null);
		if (entry == null) {
			return null;
		}
		IClientExtensionProvider<IN, OUT> provider = mapper.apply(entry.getKey());
		if (provider == null) {
			return null;
		}
		try {
			return provider.getClientGroups(accessor, entry.getValue());
		} catch (Exception e) {
			WailaExceptionHandler.handleErr(e, provider, tooltip::add);
			return null;
		}
	}

	public static float getEnchantPowerBonus(IBlockState state, World world, BlockPos pos) {
		if (WailaClientRegistration.instance().customEnchantPowers.containsKey(state.getBlock())) {
			return WailaClientRegistration.instance().customEnchantPowers.get(state.getBlock()).getEnchantPowerBonus(state, world, pos);
		}
		// 1.12.2: Bookshelf check
		if (state.getBlock() instanceof BlockBookshelf) {
			return 1;
		}
		return 0;
	}

	public static void init() {
		MinecraftForge.EVENT_BUS.register(ClientProxy.class);

		// 1.12.2: replaces LanguageManagerMixin. JadeLanguages implements
		// IResourceManagerReloadListener, so it reloads its metadata alongside vanilla.
		// ThemeHelper is registered right after so the jade_themes/*.json load
		// alongside the language metadata on every resource reload.
		IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
		if (resourceManager instanceof IReloadableResourceManager reloadable) {
			reloadable.registerReloadListener(JadeLanguages.INSTANCE);
			reloadable.registerReloadListener(ThemeHelper.INSTANCE);
		}

		// B7 order: JadeClient.init() queues the KeyBinding instances first, then they
		// are registered with the client registry, then the queue is cleared.
		JadeClient.init();
		for (KeyBinding key : keys) {
			ClientRegistry.registerKeyBinding(key);
		}
		keys.clear();

		// 1.12.2: JEI/HEI recipe lookup. The modern PolydexCompat and REICompat ports do
		// not exist; JeiCompat is loaded reflectively like modern Jade does, gated on the
		// same mod id ("jei" -- HEI 4.32 declares itself with that id).
		if (CommonProxy.isModLoaded("jei")) {
			JadeClient.addRecipeLookupPlugin("snownee.jade.compat.JeiCompat");
		}

		registerCommands();
	}

	public static void sendPacket(IMessage payload) {
		JadeNetwork.sendToServer(payload);
	}

	public static boolean shouldFetchFromServer(@Nullable UUID uuid) {
		String name = lookupPlayerName(uuid);
		return name == null || name.equals(PlayerNameLookup.DUMMY_NAME);
	}

	@Nullable
	public static String lookupPlayerName(@Nullable UUID uuid) {
		if (uuid == null) {
			return null;
		}
		// Connected to a server (integrated or dedicated): the player's info carries the name.
		NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
		if (connection != null) {
			NetworkPlayerInfo info = connection.getPlayerInfo(uuid);
			if (info != null) {
				return info.getGameProfile().getName();
			}
		}
		World world = Minecraft.getMinecraft().world;
		if (world != null) {
			EntityPlayer player = world.getPlayerEntityByUUID(uuid);
			if (player != null) {
				return player.getName();
			}
		}
		// 1.12.2: the client has no getProfileCache(); singleplayer exposes one through the
		// integrated server. The cache can be null while joining, so guard it.
		PlayerProfileCache profileCache =
				Minecraft.getMinecraft().getIntegratedServer() != null ?
						Minecraft.getMinecraft().getIntegratedServer().getPlayerProfileCache() : null;
		return profileCache != null ? PlayerNameLookup.get(uuid, profileCache) : null;
	}

	public static void runWithContext(Minecraft client, Runnable runnable) {
		client.addScheduledTask(runnable);
	}
}
