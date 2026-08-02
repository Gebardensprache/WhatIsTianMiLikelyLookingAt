package mcjty.theoneprobe.setup;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.Loader;

import java.io.File;

/**
 * 1.12.2 backport: minimal stand-in for The One Probe's {@code mcjty.theoneprobe.setup.ModSetup},
 * holding only the public fields/methods compiled integrations reference (e.g. {@code TheOneProbe.setup.tesla},
 * {@code TheOneProbe.setup.redstoneflux}, {@code TheOneProbe.setup.getLogger()}). The full pre/init/post
 * registration machinery is owned by the shim's {@code snownee.jade.shim.top.TopShim} instead.
 */
public class ModSetup {

    public static File modConfigDir;

    public static boolean baubles = false;
    public static boolean tesla = false;
    public static boolean redstoneflux = false;

    private static final Logger LOGGER = LogManager.getLogger("theoneprobe");

    static {
        tesla = Loader.isModLoaded("tesla");
        redstoneflux = Loader.isModLoaded("redstoneflux");
        baubles = Loader.isModLoaded("baubles");
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public ModSetup() {
    }
}
