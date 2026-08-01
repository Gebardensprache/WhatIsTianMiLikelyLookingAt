package snownee.jade.compat.top;

import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mcjty.theoneprobe.api.IBlockDisplayOverride;
import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.IEntityDisplayOverride;
import mcjty.theoneprobe.api.IOverlayRenderer;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import snownee.jade.shim.top.TopShim;

/**
 * Shim implementation of {@link ITheOneProbe}, delivered to mods via
 * {@code FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", ...)}.
 * <p>
 * 1.12.2 backport: formerly embedded in Jade's own jar as {@code snownee.jade.compat.top};
 * now lives in the standalone TOP shim and uses the shim's own logger instead of
 * {@code snownee.jade.Jade.LOGGER}.
 */
public class TheOneProbeImpl implements ITheOneProbe {

	private static final Logger LOGGER = LogManager.getLogger(TopShim.MODID);

	public static final TheOneProbeImpl INSTANCE = new TheOneProbeImpl();

	private final TopProviderStore store = new TopProviderStore();
	private final Set<String> warnedUnsupported = new HashSet<>();

	private TheOneProbeImpl() {
	}

	public TopProviderStore getStore() {
		return store;
	}

	@Override
	public void registerProvider(IProbeInfoProvider provider) {
		store.registerProvider(provider);
	}

	@Override
	public void registerEntityProvider(IProbeInfoEntityProvider provider) {
		store.registerEntityProvider(provider);
	}

	@Override
	public int registerElementFactory(IElementFactory factory) {
		return store.registerElementFactory(factory);
	}

	@Override
	public IElementFactory getElementFactory(int id) {
		return store.getElementFactory(id);
	}

	@Override
	public IOverlayRenderer getOverlayRenderer() {
		warnUnsupported("getOverlayRenderer()");
		return null;
	}

	@Override
	public IProbeConfig createProbeConfig() {
		warnUnsupported("createProbeConfig()");
		return null;
	}

	@Override
	public void registerProbeConfigProvider(IProbeConfigProvider provider) {
		warnUnsupported("registerProbeConfigProvider(IProbeConfigProvider)");
	}

	@Override
	public void registerBlockDisplayOverride(IBlockDisplayOverride override) {
		warnUnsupported("registerBlockDisplayOverride(IBlockDisplayOverride)");
	}

	@Override
	public void registerEntityDisplayOverride(IEntityDisplayOverride override) {
		warnUnsupported("registerEntityDisplayOverride(IEntityDisplayOverride)");
	}

	private void warnUnsupported(String operation) {
		if (warnedUnsupported.add(operation)) {
			LOGGER.warn("TOP compat: {} is not supported by Jade; call ignored", operation);
		}
	}
}
