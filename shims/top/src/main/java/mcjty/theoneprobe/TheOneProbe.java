package mcjty.theoneprobe;

import mcjty.theoneprobe.apiimpl.TheOneProbeImp;

/**
 * Shim stand-in for the real TOP's main class. The real TOP holds its {@code ITheOneProbe}
 * implementation in the public static field {@link #theOneProbeImp}, and some integrations
 * (e.g. GregTech's {@code TheOneProbeModule}) read that field directly instead of going through
 * {@code FMLInterModComms}. The shim must ship this class with the exact
 * {@code mcjty.theoneprobe.TheOneProbe} name and field or those integrations crash with
 * {@code NoClassDefFoundError}.
 * <p>
 * This is deliberately NOT the {@code @Mod}: the shim's actual mod identity is owned by
 * {@code snownee.jade.shim.top.TopShim} (modid {@code theoneprobe}). Shipping a second class
 * named {@code TheOneProbe} without {@code @Mod} keeps the mod-id unique while providing the
 * static field integrations link against.
 */
public class TheOneProbe {

	/** The shim's TOP implementation, in the exact shape the real TOP exposes. */
	public static final TheOneProbeImp theOneProbeImp = TheOneProbeImp.INSTANCE;

	private TheOneProbe() {
	}
}
