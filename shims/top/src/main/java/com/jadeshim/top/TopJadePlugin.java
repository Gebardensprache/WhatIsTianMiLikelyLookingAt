package com.jadeshim.top;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.compat.top.TopCompat;

/**
 * Jade plugin entry point for the TOP compatibility shim.
 * <p>
 * 1.12.2 backport: this class MUST live outside the {@code snownee.jade.*} package — Jade's
 * plugin loader throws "Built-in plugin registered by non-Jade mod" for any class whose package
 * starts with {@code snownee.jade.} but whose owning mod is not Jade itself. The owning mod
 * container is resolved by package scanning ({@code ModContainer.getOwnedPackages()}), so a class
 * in {@code com.jadeshim.top} is owned by the shim mod (modid {@code theoneprobe}).
 * <p>
 * The bridges are registered through the PUBLIC {@link IWailaCommonRegistration} /
 * {@link IWailaClientRegistration} interfaces handed to {@link #register} / {@link #registerClient}
 * (Jade's impl singletons are intentionally not reachable from the shim).
 */
@WailaPlugin
public class TopJadePlugin implements IWailaPlugin {

	@Override
	public void register(IWailaCommonRegistration registration) {
		TopCompat.registerCommon(registration);
	}

	@Override
	public void registerClient(IWailaClientRegistration registration) {
		TopCompat.registerClient(registration);
	}
}
