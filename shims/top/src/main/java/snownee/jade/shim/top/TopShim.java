package snownee.jade.shim.top;

import java.util.function.Function;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.apiimpl.TheOneProbeImp;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import snownee.jade.compat.top.TheOneProbeImpl;

/**
 * Stand-alone dummy mod declaring the legacy {@code theoneprobe} mod id.
 * <p>
 * 1.12.2 backport: third-party TOP integrations resolve mods by the {@code theoneprobe} mod id;
 * declaring it here makes those integrations load and link against this shim instead of the real
 * TOP, while the actual tooltip rendering is delegated to Jade. The Jade plugin entry point
 * (in a non-{@code snownee.jade.*} package) is discovered by Jade's plugin loader via the
 * {@code @snownee.jade.api.WailaPlugin} annotation on the class in this jar.
 * <p>
 * TOP's integration seam is function IMC (not ASM scanning): mods call
 * {@code FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe", ...)}. Each such
 * message carries a {@link Function}<{@link ITheOneProbe}, {@link Void}> whose parameter is the
 * shim's {@link TheOneProbeImpl#INSTANCE}, exactly as the Jade-embedded
 * {@code TopCompat.onInterModComms} handled it.
 * <p>
 * <b>Delivery context (load-bearing).</b> Forge routes IMC by target modid and drops any message
 * whose target is not a loaded mod ({@code Loader.isModLoaded}). The embedded design registered
 * {@code onIMC} on the {@code jade} container while integrations send to {@code theoneprobe}, so
 * when real TOP was absent (the only case the embedded compat was active) every
 * {@code getTheOneProbe} message was discarded and the handler never saw a provider. This shim
 * declares {@code @Mod(modid = "theoneprobe")}, making the target loaded and delivering the
 * messages to {@link #onIMC}. Do not "simplify" this back toward the embedded shape: without the
 * {@code theoneprobe} container, all third-party TOP integrations silently register nothing.
 * {@link #asmData} is captured during pre-init for completeness/parity with the HWYLA shim, but
 * the TOP path does not use it.
 */
@Mod(
		modid = TopShim.MODID,
		name = TopShim.NAME,
		version = TopShim.VERSION,
		dependencies = "required-after:jade",
		acceptedMinecraftVersions = "[1.12.2]")
public class TopShim {
	public static final String MODID = "theoneprobe";
	public static final String NAME = "Jade TOP Compatibility";
	public static final String VERSION = "1.0.0";

	private static final String GET_THE_ONE_PROBE = "getTheOneProbe";

	/** ASM data captured during pre-init; not used by the TOP path (IMC-based). */
	public static ASMDataTable asmData;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		asmData = event.getAsmData();
		// Mirrors real TOP's ModSetup.preInit: the built-in element factories must be registered
		// before any integration (or our own server bridge) serializes/deserializes elements.
		// Integrations that reach the shim via getTheOneProbe IMC register their own factories
		// during their preInit, after this call.
		TheOneProbeImp.registerElements();
	}

	/**
	 * Delivers the shim's {@link ITheOneProbe} implementation to legacy function IMC messages.
	 * <p>
	 * 1.12.2 backport: handler body is the direct equivalent of the Jade-embedded
	 * {@code TopCompat.onInterModComms} (minus the {@code Loader.isModLoaded("theoneprobe")}
	 * guard, which is meaningless for the shim since it IS theoneprobe). What changed is the
	 * <em>delivery</em>: the embedded handler lived on the {@code jade} container and Forge dropped
	 * {@code theoneprobe}-targeted messages, so it never fired for real integrations; this handler
	 * is reached because the shim owns the {@code theoneprobe} mod id (see class Javadoc). Forge
	 * applies each returned function to the previous message's function, so the impl instance is
	 * threaded through every getTheOneProbe message in arrival order.
	 *
	 * @param event Forge's IMC event
	 */
	@Mod.EventHandler
	public void onIMC(FMLInterModComms.IMCEvent event) {
		for (FMLInterModComms.IMCMessage message : event.getMessages()) {
			if (!GET_THE_ONE_PROBE.equals(message.key)) {
				continue;
			}
			message.getFunctionValue(ITheOneProbe.class, Void.class)
					.ifPresent(function -> function.apply(TheOneProbeImpl.INSTANCE));
		}
	}
}
