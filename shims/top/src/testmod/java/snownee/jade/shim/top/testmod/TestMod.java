package snownee.jade.shim.top.testmod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

/**
 * Test-mod fixture for the TOP shim (lives in the shim's test source set, mirroring the
 * Jade root's {@code snownee.jade.testmod.TestMod}).
 * <p>
 * 1.12.2 backport: proves the shim's {@code getTheOneProbe} function-IMC seam end to end —
 * {@code TopTestIntegration.register()} sends the function message that the shim's
 * {@code TopShim.onIMC} answers with {@code TheOneProbeImpl.INSTANCE}.
 */
@Mod(modid = TestMod.MODID, name = "Jade TOP Shim Test Mod", version = "1.0")
public class TestMod {

	public static final String MODID = "jadetopshimtestmod";

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		TopTestIntegration.register();
	}
}
