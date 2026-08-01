package snownee.jade.shim.hwyla;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Stand-alone dummy mod declaring the legacy {@code waila} mod id.
 * <p>
 * 1.12.2 backport: third-party HWYLA integrations resolve mods by the {@code waila} mod id;
 * declaring it here makes those integrations load and link against this shim instead of the real
 * HWYLA, while the actual tooltip rendering is delegated to Jade. The Jade plugin entry point
 * (in a non-{@code snownee.jade.*} package) is discovered by Jade's plugin loader via the
 * {@code @snownee.jade.api.WailaPlugin} annotation on the class in this jar.
 */
@Mod(
		modid = HwylaShim.MODID,
		name = HwylaShim.NAME,
		version = HwylaShim.VERSION,
		dependencies = "required-after:jade",
		acceptedMinecraftVersions = "[1.12.2]")
public class HwylaShim {
	public static final String MODID = "waila";
	public static final String NAME = "Jade HWYLA Compatibility";
	public static final String VERSION = "1.0.0";

	/** Shared logger; the shim's stand-in for {@code snownee.jade.Jade.LOGGER}. */
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	/** ASM data captured during pre-init; used by the shim's plugin registration (B6 port). */
	public static ASMDataTable asmData;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		asmData = event.getAsmData();
	}
}
