package mcjty.theoneprobe.compat;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.lang.reflect.Method;

/**
 * 1.12.2 backport: same class name and public method signatures as The One Probe's
 * {@code mcjty.theoneprobe.compat.RedstoneFluxTools}, but implemented via reflection so the shim
 * needs no hard dependency on CoFH's RedstoneFlux API.
 * <p>
 * Compiled integrations (e.g. NomiLabs' {@code LabsRFInfoProvider}) link against this class's
 * static methods directly; without it they crash with {@code NoClassDefFoundError}. The real TOP
 * compiles this against {@code cofh.redstoneflux.api.IEnergyHandler}; here the method bodies probe
 * that interface reflectively and degrade gracefully (0 / false) when RedstoneFlux is absent.
 */
public class RedstoneFluxTools {

    private static final String I_ENERGY_HANDLER = "cofh.redstoneflux.api.IEnergyHandler";

    public static int getEnergy(TileEntity te) {
        try {
            Class<?> handlerClass = Class.forName(I_ENERGY_HANDLER);
            if (!handlerClass.isInstance(te)) {
                return 0;
            }
            Method m = handlerClass.getMethod("getEnergyStored", EnumFacing.class);
            return (Integer) m.invoke(te, EnumFacing.DOWN);
        } catch (Exception e) {
            return 0;
        }
    }

    public static int getMaxEnergy(TileEntity te) {
        try {
            Class<?> handlerClass = Class.forName(I_ENERGY_HANDLER);
            if (!handlerClass.isInstance(te)) {
                return 0;
            }
            Method m = handlerClass.getMethod("getMaxEnergyStored", EnumFacing.class);
            return (Integer) m.invoke(te, EnumFacing.DOWN);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isEnergyHandler(TileEntity te) {
        try {
            Class<?> handlerClass = Class.forName(I_ENERGY_HANDLER);
            return handlerClass.isInstance(te);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private RedstoneFluxTools() {
    }
}
