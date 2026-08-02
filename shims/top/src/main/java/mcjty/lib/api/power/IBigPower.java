package mcjty.lib.api.power;

/**
 * 1.12.2 backport: bundled into the shim exactly as The One Probe bundles it.
 * <p>
 * The real TOP ships this interface inside its own jar (source lives at
 * {@code mcjty/theoneprobe/src/main/java/mcjty/lib/api/power/IBigPower.java}); integrations
 * compiled against TOP reference it directly (e.g. NomiLabs' {@code LabsRFInfoProvider} checks
 * {@code instanceof IBigPower}). When the shim replaces TOP, this class must still be present or
 * those integrations crash with {@code NoClassDefFoundError}.
 */
public interface IBigPower {

    long getStoredPower();

    long getCapacity();
}
