package snownee.jade.overlay;

import java.util.List;

import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;

import com.google.common.base.Preconditions;

import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import snownee.jade.api.Accessor;
import snownee.jade.api.AccessorClientHandler;
import snownee.jade.api.EmptyAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.JadeIds;
import snownee.jade.api.callback.JadeBeforeTooltipCollectCallback;
import snownee.jade.api.callback.JadeRayTraceCallback;
import snownee.jade.api.callback.JadeTooltipCollectedCallback;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.config.IWailaConfig.DisplayMode;
import snownee.jade.api.config.IWailaConfig.General;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.gui.PreviewOptionsScreen;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.impl.Tooltip;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.impl.ui.BoxElementImpl;
import snownee.jade.track.ProgressTracker;
import snownee.jade.util.ClientProxy;

/**
 * 1.12.2 translation notes:
 * <ul>
 *   <li>{@code GameNarrator} system dropped entirely (no 1.12.2 equivalent).
 *       {@link #narrate(Element, boolean)} and {@link #narrate(String, boolean)}
 *       are kept as no-ops with Javadoc documenting the drop.</li>
 *   <li>{@code BlockHitResult}/{@code EntityHitResult} unified into {@link RayTraceResult}.</li>
 *   <li>{@code CustomData} and {@code DataComponents.CUSTOM_DATA} replaced with
 *       direct {@link NBTTagCompound} reads.</li>
 *   <li>{@code Identifier} -> {@link ResourceLocation}.</li>
 *   <li>{@code Level} -> {@link World}, {@code BlockEntity} -> {@link TileEntity}.</li>
 * </ul>
 */
public class WailaTickHandler {
	public static final String REMOVE_ELEMENTS = "$jade:remove";

	private String lastNarration = "";
	private long lastNarrationTime = 0;
	public ProgressTracker progressTracker = new ProgressTracker();
	public @Nullable BoxElementImpl rootElement;
	public @Nullable State state;

	/**
	 * 1.12.2: no GameNarrator system. Kept as a no-op for API compatibility.
	 */
	public void narrate(Element element, boolean dedupe) {
		// 1.12.2: narration dropped
	}

	/**
	 * 1.12.2: no GameNarrator system. Kept as a no-op for API compatibility.
	 */
	public void narrate(String message, boolean dedupe) {
		// 1.12.2: narration dropped
	}

	public void clearState() {
		lastNarration = "";
		state = null;
		rootElement = null;
		progressTracker.clear();
	}

	@SuppressWarnings("deprecation")
	public void tickClient() {
		Minecraft mc = Minecraft.getMinecraft();
		World level = mc.world;
		if (level == null) {
			OverlayRenderer.clearLingerTooltip();
			clearState();
			return;
		}

		progressTracker.tick();

		General config = IWailaConfig.get().general();
		if (!config.shouldDisplayTooltip()) {
			clearState();
			return;
		}

		if (JadeUI.isPinned()) {
			return;
		}

		if (ClientProxy.shouldHideWithGui(mc, mc.currentScreen)) {
			return;
		}

		Entity entity = mc.getRenderViewEntity();
		if (entity == null) {
			clearState();
			return;
		}

		RayTracing.INSTANCE.fire();
		RayTraceResult target = RayTracing.INSTANCE.getTarget();
		if (target == null) {
			clearState();
			return;
		}

		Accessor<?> accessor;
		boolean useRayTraceCallback = true;
		outer:
		if (target.typeOfHit == RayTraceResult.Type.BLOCK && target.getBlockPos() != null) {
			IBlockState state = level.getBlockState(target.getBlockPos());
			if (state.getBlock().isAir(state, level, target.getBlockPos())) {
				accessor = createEmpty(target);
				break outer;
			}
			TileEntity tileEntity = level.getTileEntity(target.getBlockPos());
			accessor = WailaClientRegistration.instance().blockAccessor()
					.blockState(state)
					.blockEntity(tileEntity)
					.hit(target)
					.requireVerification()
					.build();
		} else if (target.typeOfHit == RayTraceResult.Type.ENTITY) {
			accessor = WailaClientRegistration.instance().entityAccessor()
					.hit(target)
					.entity(target.entityHit)
					.requireVerification()
					.build();
		} else if (mc.currentScreen instanceof PreviewOptionsScreen) {
			useRayTraceCallback = false;
			accessor = WailaClientRegistration.instance().blockAccessor()
					.blockState(Blocks.GRASS.getDefaultState())
					.hit(new RayTraceResult(entity.getPositionVector(), EnumFacing.UP, entity.getPosition()))
					.build();
		} else {
			accessor = createEmpty(target);
		}

		if (useRayTraceCallback) {
			Accessor<?> originalAccessor = accessor;
			EmptyAccessor originalEmpty = originalAccessor instanceof EmptyAccessor emptyAccessor ? emptyAccessor : null;
			for (JadeRayTraceCallback callback : WailaClientRegistration.instance().rayTraceCallback.callbacks()) {
				accessor = callback.onRayTrace(target, accessor, originalAccessor);
				if (accessor == null) {
					if (originalEmpty == null) {
						originalEmpty = createEmpty(originalAccessor.getHitResult());
					}
					accessor = originalEmpty;
				}
			}
		}

		if (!accessor.verifyData(accessor.getServerData())) {
			accessor.setServerData(null);
		}

		ObjectDataCenter.set(accessor);
		var handler = WailaClientRegistration.instance().getAccessorHandler(accessor.getAccessorType());

		if (!handler.shouldDisplay(accessor)) {
			clearState();
			return;
		}

		state = State.create(state, accessor, handler, state == null ? null : state.data);
		if (accessor.isServerConnected()) {
			NBTTagCompound data = accessor.getServerData();
			accessor.setServerData(null);
			List<IServerDataProvider<Accessor<?>>> providers = handler.shouldRequestData(accessor);
			if (ObjectDataCenter.isTimeElapsed(ObjectDataCenter.rateLimiter)) {
				ObjectDataCenter.resetTimer();
				if (!providers.isEmpty()) {
					handler.requestData(accessor, providers);
				}
			}
			if (!providers.isEmpty() && getData() == null) {
				return;
			}
			accessor.setServerData(data);
		}

		Theme theme = IWailaConfig.get().overlay().getTheme();
		MutableObject<Theme> holder = new MutableObject<>(theme);
		Preconditions.checkNotNull(theme, "Theme cannot be null");
		Accessor<?> accessor0 = accessor;
		for (JadeBeforeTooltipCollectCallback callback : WailaClientRegistration.instance().beforeTooltipCollectCallback.callbacks()) {
			if (!callback.beforeCollecting(holder, accessor0)) {
				return;
			}
		}
		Preconditions.checkNotNull(holder.getValue(), "Theme cannot be null");
		IThemeHelper themes = IThemeHelper.get();
		if (theme != holder.getValue()) {
			theme = holder.getValue();
			themes.setThemeOverride(theme);
		}

		Tooltip tooltip = new Tooltip();
		tooltip.setIcon(state.getIcon());

		if (config.getDisplayMode() == DisplayMode.LITE && !ClientProxy.isShowDetailsPressed()) {
			Tooltip dummyTooltip = new Tooltip();
			handler.gatherComponents(
					accessor, $ -> {
						if (Math.abs(WailaCommonRegistration.instance().priorities.byValue($)) > 5000) {
							return tooltip;
						} else {
							return dummyTooltip;
						}
					});
			if (!dummyTooltip.isEmpty()) {
				tooltip.sneakyDetails = true;
			}
		} else {
			handler.gatherComponents(accessor, $ -> tooltip);
		}

		if (accessor.isServersideContent()) {
			NBTTagCompound serversideTag = accessor.getServersideRep().getTagCompound();
			if (serversideTag != null && serversideTag.hasKey(REMOVE_ELEMENTS)) {
				readRemoveElements(serversideTag, tooltip);
			}
		}

		tooltip.setIcon(themes.theme().modifyIcon(tooltip.getIcon()));
		BoxElementImpl newElement = new BoxElementImpl(tooltip, themes.theme().tooltipStyle);
		newElement.tag(JadeIds.ROOT);
		for (JadeTooltipCollectedCallback callback : WailaClientRegistration.instance().tooltipCollectedCallback.callbacks()) {
			callback.onTooltipCollected(newElement, accessor);
		}
		if (newElement.getTooltip().isDirty) {
			newElement.updateSize();
		}
		if (rootElement == null || rootElement.layout.getX() != newElement.layout.getX() ||
				rootElement.layout.getY() != newElement.layout.getY() ||
				rootElement.layout.getWidth() != newElement.layout.getWidth() ||
				rootElement.layout.getHeight() != newElement.layout.getHeight()) {
			OverlayRenderer.animation.startRect.copy(OverlayRenderer.animation.rect);
			OverlayRenderer.animation.startTime = System.currentTimeMillis();
		}
		rootElement = newElement;
		themes.setThemeOverride(null);
	}

	/**
	 * 1.12.2: reads the remove-elements list from raw NBT instead of
	 * using {@code DataComponents.CUSTOM_DATA}'s codec-based read.
	 */
	private static void readRemoveElements(NBTTagCompound tag, Tooltip tooltip) {
		if (tag.hasKey(REMOVE_ELEMENTS, Constants.NBT.TAG_STRING)) {
			tooltip.remove(new ResourceLocation(tag.getString(REMOVE_ELEMENTS)));
		} else if (tag.hasKey(REMOVE_ELEMENTS, Constants.NBT.TAG_LIST)) {
			NBTTagList list = tag.getTagList(REMOVE_ELEMENTS, Constants.NBT.TAG_STRING);
			for (int i = 0; i < list.tagCount(); i++) {
				tooltip.remove(new ResourceLocation(list.getStringTagAt(i)));
			}
		}
	}

	private static EmptyAccessor createEmpty(RayTraceResult hit) {
		RayTraceResult miss;
		if (hit.typeOfHit == RayTraceResult.Type.MISS && hit.getBlockPos() != null) {
			miss = hit;
		} else {
			Vec3d vec = hit.hitVec;
			miss = new RayTraceResult(
					vec,
					EnumFacing.getFacingFromVector((float) vec.x, (float) vec.y, (float) vec.z),
					new BlockPos(vec));
		}
		return WailaClientRegistration.instance().emptyAccessor().hit(miss).build();
	}

	public void setData(NBTTagCompound tag) {
		if (state == null) {
			return;
		}
		state = state.withData(tag);
	}

	public @Nullable NBTTagCompound getData() {
		return state == null ? null : state.data;
	}

	public record State(Accessor<?> accessor, AccessorClientHandler<Accessor<?>> handler, @Nullable NBTTagCompound data) {
		public static State create(
				@Nullable State prev,
				Accessor<?> accessor,
				AccessorClientHandler<Accessor<?>> handler,
				@Nullable NBTTagCompound data) {
			return new State(accessor, handler, data != null && accessor.verifyData(data) ? data : null);
		}

		@Nullable
		public Element getIcon() {
			if (accessor == null || handler == null) {
				return null;
			}
			Element icon = handler.getIcon(accessor);
			if (JadeUI.isEmptyElement(icon)) {
				return null;
			}
			return icon;
		}

		public State withData(NBTTagCompound data) {
			if (!verifyData(data)) {
				return this;
			}
			return new State(accessor, handler, data);
		}

		public boolean verifyData(NBTTagCompound data) {
			if (data == null) {
				return true;
			}
			return accessor.verifyData(data);
		}
	}
}
