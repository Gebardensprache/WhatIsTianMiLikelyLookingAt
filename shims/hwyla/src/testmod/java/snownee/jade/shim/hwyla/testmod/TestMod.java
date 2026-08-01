package snownee.jade.shim.hwyla.testmod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Test-mod fixture for the HWYLA shim (lives in the shim's test source set, mirroring the
 * Jade root's {@code snownee.jade.testmod.TestMod}).
 * <p>
 * 1.12.2 backport: proves the shim's {@code @mcp.mobius.waila.api.WailaPlugin} ASM-scan seam
 * end to end — {@link HwylaTestPlugin} is annotated with {@code @WailaPlugin(TestMod.MODID)}
 * and is discovered by the shim's scan ({@code HwylaCompat.scanPlugins}) because the mod
 * declared by {@code @WailaPlugin}'s value is loaded.
 */
@Mod(modid = TestMod.MODID, name = "Jade HWYLA Shim Test Mod", version = "1.0")
public class TestMod {

	public static final String MODID = "jadehwylashimtestmod";

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		// Registration happens via ASM scanning at Jade's plugin-load time; nothing to do here
		// beyond loading this mod so the @WailaPlugin(TestMod.MODID) requirement is satisfied.
	}
}
