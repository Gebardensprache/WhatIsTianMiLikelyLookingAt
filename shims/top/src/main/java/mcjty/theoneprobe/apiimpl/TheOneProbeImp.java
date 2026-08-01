package mcjty.theoneprobe.apiimpl;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.IEntityDisplayOverride;
import mcjty.theoneprobe.api.IOverlayRenderer;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.apiimpl.elements.ElementEntity;
import mcjty.theoneprobe.apiimpl.elements.ElementHorizontal;
import mcjty.theoneprobe.apiimpl.elements.ElementIcon;
import mcjty.theoneprobe.apiimpl.elements.ElementItemLabel;
import mcjty.theoneprobe.apiimpl.elements.ElementItemStack;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import mcjty.theoneprobe.apiimpl.elements.ElementText;
import mcjty.theoneprobe.apiimpl.elements.ElementVertical;
import snownee.jade.compat.top.TheOneProbeImpl;

/**
 * Shim stand-in for the real TOP's {@code mcjty.theoneprobe.apiimpl.TheOneProbeImp}, which is
 * exposed to integrations via the static {@code mcjty.theoneprobe.TheOneProbe.theOneProbeImp}
 * field. Some integrations (e.g. GregTech's {@code TheOneProbeModule}) read that field directly
 * instead of going through {@code FMLInterModComms}, so this class must exist with the exact
 * {@code mcjty.theoneprobe.apiimpl.TheOneProbeImp} name or those integrations crash with
 * {@code NoClassDefFoundError}. Every call delegates to the shim's {@link TheOneProbeImpl} so
 * direct-field registrations land in the same provider store as IMC registrations.
 * <p>
 * The {@code ELEMENT_*} constants and {@link #registerElements()} mirror the real TOP exactly:
 * the built-in element classes ({@code ElementItemStack} and friends) return these IDs from
 * {@code getID()} and must be registered so server->client round-trips of those elements resolve.
 * {@link #create()} is the same factory seam real TOP exposes for its {@code ProbeInfo}.
 */
public class TheOneProbeImp implements ITheOneProbe {

	public static int ELEMENT_TEXT;
	public static int ELEMENT_ITEM;
	public static int ELEMENT_PROGRESS;
	public static int ELEMENT_HORIZONTAL;
	public static int ELEMENT_VERTICAL;
	public static int ELEMENT_ENTITY;
	public static int ELEMENT_ICON;
	public static int ELEMENT_ITEMLABEL;

	/** Same seam as real TOP: registers the built-in elements with the delegate's store. */
	public static void registerElements() {
		TheOneProbeImpl store = TheOneProbeImpl.INSTANCE;
		ELEMENT_TEXT = store.registerElementFactory(ElementText::new);
		ELEMENT_ITEM = store.registerElementFactory(ElementItemStack::new);
		ELEMENT_PROGRESS = store.registerElementFactory(ElementProgress::new);
		ELEMENT_HORIZONTAL = store.registerElementFactory(ElementHorizontal::new);
		ELEMENT_VERTICAL = store.registerElementFactory(ElementVertical::new);
		ELEMENT_ENTITY = store.registerElementFactory(ElementEntity::new);
		ELEMENT_ICON = store.registerElementFactory(ElementIcon::new);
		ELEMENT_ITEMLABEL = store.registerElementFactory(ElementItemLabel::new);
	}

	public static final TheOneProbeImp INSTANCE = new TheOneProbeImp();

	private final TheOneProbeImpl delegate = TheOneProbeImpl.INSTANCE;

	private TheOneProbeImp() {
	}

	@Override
	public void registerProvider(IProbeInfoProvider provider) {
		delegate.registerProvider(provider);
	}

	@Override
	public void registerEntityProvider(IProbeInfoEntityProvider provider) {
		delegate.registerEntityProvider(provider);
	}

	@Override
	public int registerElementFactory(IElementFactory factory) {
		return delegate.registerElementFactory(factory);
	}

	@Override
	public IElementFactory getElementFactory(int id) {
		return delegate.getElementFactory(id);
	}

	@Override
	public IOverlayRenderer getOverlayRenderer() {
		return delegate.getOverlayRenderer();
	}

	@Override
	public IProbeConfig createProbeConfig() {
		return delegate.createProbeConfig();
	}

	@Override
	public void registerProbeConfigProvider(IProbeConfigProvider provider) {
		delegate.registerProbeConfigProvider(provider);
	}

	@Override
	public void registerBlockDisplayOverride(IBlockDisplayOverride override) {
		delegate.registerBlockDisplayOverride(override);
	}

	@Override
	public void registerEntityDisplayOverride(IEntityDisplayOverride override) {
		delegate.registerEntityDisplayOverride(override);
	}
}
