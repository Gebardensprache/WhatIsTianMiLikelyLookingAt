package mcjty.theoneprobe.config;

import mcjty.theoneprobe.api.NumberFormat;

/**
 * 1.12.2 backport: legacy class name for The One Probe's config. Newer TOP renamed this to
 * {@link ConfigSetup}, but some compiled integrations (e.g. NomiLabs' {@code RecipeOutputsProvider}
 * and {@code LabsRFInfoProvider}) still reference {@code mcjty.theoneprobe.config.Config} static
 * fields directly. The shim must ship this class with those fields or the providers throw
 * {@code NoClassDefFoundError} / {@code NoSuchFieldError}. The fields forward to
 * {@link ConfigSetup} so both names always agree.
 */
public class Config {

    public static int needsProbe = ConfigSetup.needsProbe;

    public static boolean extendedInMain = ConfigSetup.extendedInMain;
    public static NumberFormat rfFormat = ConfigSetup.rfFormat;
    public static NumberFormat tankFormat = ConfigSetup.tankFormat;
    public static int timeout = ConfigSetup.timeout;
    public static int waitingForServerTimeout = ConfigSetup.waitingForServerTimeout;
    public static int maxPacketToServer = ConfigSetup.maxPacketToServer;

    public static boolean supportBaubles = ConfigSetup.supportBaubles;
    public static boolean spawnNote = ConfigSetup.spawnNote;

    public static int showSmallChestContentsWithoutSneaking = ConfigSetup.showSmallChestContentsWithoutSneaking;
    // NOTE: "Thresshold" is a typo in the original TOP and integrations reference it as-is.
    public static int showItemDetailThresshold = ConfigSetup.showItemDetailThresshold;
    public static String[] showContentsWithoutSneaking = ConfigSetup.showContentsWithoutSneaking;
    public static String[] dontShowContentsUnlessSneaking = ConfigSetup.dontShowContentsUnlessSneaking;
    public static String[] dontSendNBT = ConfigSetup.dontSendNBT;

    public static float probeDistance = ConfigSetup.probeDistance;
    public static boolean showLiquids = ConfigSetup.showLiquids;
    public static boolean isVisible = ConfigSetup.isVisible;
    public static boolean compactEqualStacks = ConfigSetup.compactEqualStacks;
    public static boolean holdKeyToMakeVisible = ConfigSetup.holdKeyToMakeVisible;

    public static boolean showDebugInfo = ConfigSetup.showDebugInfo;

    public static int showBreakProgress = ConfigSetup.showBreakProgress;    // 0 == off, 1 == bar, 2 == text
    public static boolean harvestStyleVanilla = ConfigSetup.harvestStyleVanilla;

    public static int chestContentsBorderColor = ConfigSetup.chestContentsBorderColor;

    public static float tooltipScale = ConfigSetup.tooltipScale;

    // RF bar
    public static int rfbarFilledColor = ConfigSetup.rfbarFilledColor;
    public static int rfbarAlternateFilledColor = ConfigSetup.rfbarAlternateFilledColor;
    public static int rfbarBorderColor = ConfigSetup.rfbarBorderColor;

    // Tank bar
    public static int tankbarFilledColor = ConfigSetup.tankbarFilledColor;
    public static int tankbarAlternateFilledColor = ConfigSetup.tankbarAlternateFilledColor;
    public static int tankbarBorderColor = ConfigSetup.tankbarBorderColor;

    private Config() {
    }
}
