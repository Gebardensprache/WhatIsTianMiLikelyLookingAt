package mcjty.theoneprobe.compat;

import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Method;

/**
 * 1.12.2 backport: same class name and public method signatures as The One Probe's
 * {@code mcjty.theoneprobe.compat.TeslaTools}, but implemented via reflection so the shim needs
 * no hard dependency on Tesla's API.
 * <p>
 * Compiled integrations link against this class directly (real TOP compiles it against
 * {@code net.darkhax.tesla.api.ITeslaHolder}); the reflection bodies degrade gracefully to
 * {@code false}/{@code 0} when Tesla is not installed.
 */
public class TeslaTools {

    private static final String CAPABILITY_HOLDER = "net.darkhax.tesla.capability.TeslaCapabilities";

    public static long getEnergy(TileEntity te) {
        try {
            Object handler = getHolder(te);
            if (handler == null) {
                return 0;
            }
            Method m = handler.getClass().getMethod("getStoredPower");
            return (Long) m.invoke(handler);
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getMaxEnergy(TileEntity te) {
        try {
            Object handler = getHolder(te);
            if (handler == null) {
                return 0;
            }
            Method m = handler.getClass().getMethod("getCapacity");
            return (Long) m.invoke(handler);
        } catch (Exception e) {
            return 0;
        }
    }

    public static boolean isEnergyHandler(TileEntity te) {
        return getHolder(te) != null;
    }

    private static Object getHolder(TileEntity te) {
        if (te == null) {
            return null;
        }
        try {
            Class<?> caps = Class.forName(CAPABILITY_HOLDER);
            Object holderCap = caps.getField("CAPABILITY_HOLDER").get(null);
            // te.getCapability((net.minecraftforge.common.capabilities.Capability) holderCap, null)
            Method getCapability = TileEntity.class.getMethod("getCapability", net.minecraftforge.common.capabilities.Capability.class, net.minecraft.util.EnumFacing.class);
            return getCapability.invoke(te, holderCap, null);
        } catch (Exception e) {
            return null;
        }
    }

    private TeslaTools() {
    }
}
