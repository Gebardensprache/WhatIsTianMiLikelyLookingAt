package snownee.jade.compat.hwyla;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.shim.hwyla.HwylaShim;

/**
 * Entry point for the HWYLA compatibility layer.
 * <p>
 * Scans the ASM data table for classes annotated with HWYLA's {@link WailaPlugin},
 * instantiates them, and calls {@link IWailaPlugin#register(IWailaRegistrar)} with
 * an {@link HwylaRegistrar} that bridges registrations into Jade's registries through
 * the PUBLIC {@link IWailaCommonRegistration}/{@link IWailaClientRegistration} interfaces.
 * <p>
 * 1.12.2 shim: this class moved out of Jade's own jar into the standalone HWYLA shim. The
 * impl singletons were replaced by the public registration interfaces handed to
 * {@code snownee.jade.api.IWailaPlugin.register(...)}/{@code registerClient(...)}, and the
 * {@code Loader.isModLoaded("waila")} self-check was dropped because this shim IS waila.
 */
public final class HwylaCompat {

	private static final Logger LOGGER = HwylaShim.LOGGER;

	/** Set from {@link #registerCommon}; null on a dedicated server until the client side connects. */
	private static IWailaClientRegistration clientReg;
	private static IWailaCommonRegistration commonReg;

	private HwylaCompat() {
	}

	/**
	 * Registers the server-side registration handle. Invoked by Jade's plugin loader
	 * (which hands every plugin the same {@link IWailaCommonRegistration} instance it resets
	 * per load pass), so no impl singleton is reachable from here.
	 */
	public static void registerCommon(IWailaCommonRegistration registration) {
		commonReg = registration;
		// On a dedicated server Jade never calls registerClient, so the scan must run now or
		// the server-side NBT bridges would never register (HWYLA plugins' register() registers
		// server data providers too). On a physical client the scan runs once from registerClient.
		if (FMLLaunchHandler.side().isServer()) {
			scanPlugins();
		}
	}

	/**
	 * Registers the client-side registration handle and runs the HWYLA plugin scan. Invoked
	 * by Jade's plugin loader after {@link #registerCommon} on physical clients.
	 */
	public static void registerClient(IWailaClientRegistration registration) {
		clientReg = registration;
		HwylaConfigHandler.INSTANCE.setClientRegistration(clientReg);
		scanPlugins();
	}

	/**
	 * Scans the ASM data table for {@link WailaPlugin}-annotated classes and bridges them into
	 * Jade. Runs once per load pass (Jade calls register/registerClient exactly once per pass).
	 */
	private static void scanPlugins() {
		if (commonReg == null) {
			return;
		}
		ASMDataTable asmData = HwylaShim.asmData;
		if (asmData == null) {
			return;
		}

		HwylaConfigHandler configHandler = HwylaConfigHandler.INSTANCE;
		HwylaRegistrar registrar = new HwylaRegistrar(configHandler, clientReg, commonReg);

		String annotationName = WailaPlugin.class.getCanonicalName();
		if (annotationName == null) {
			return;
		}

		for (ASMDataTable.ASMData data : asmData.getAll(annotationName)) {
			try {
				String requiredMod = "";
				Map<String, Object> info = data.getAnnotationInfo();
				if (info != null && info.get("value") instanceof String) {
					requiredMod = (String) info.get("value");
				}
				if (!requiredMod.isEmpty() && !Loader.isModLoaded(requiredMod)) {
					LOGGER.debug("Skipping HWYLA plugin {}: required mod {} not loaded",
							data.getClassName(), requiredMod);
					continue;
				}

				Class<?> clazz = Class.forName(data.getClassName());
				if (!IWailaPlugin.class.isAssignableFrom(clazz)) {
					LOGGER.error("HWYLA plugin class {} does not implement IWailaPlugin",
							data.getClassName());
					continue;
				}

				IWailaPlugin plugin = (IWailaPlugin) clazz.newInstance();
				plugin.register(registrar);
				LOGGER.info("Loaded HWYLA compat plugin: {}", data.getClassName());
			} catch (Exception e) {
				LOGGER.error("Failed to load HWYLA compat plugin {}", data.getClassName(), e);
			}
		}
	}
}
