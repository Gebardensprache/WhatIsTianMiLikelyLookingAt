package mcjty.theoneprobe.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.util.text.TextFormatting;
import mcjty.theoneprobe.api.TextStyleClass;

/**
 * 1.12.2 backport: minimal stand-in for The One Probe's {@code ConfigSetup}, holding only the
 * {@link TextStyleClass} -> style-name mapping and the style-string -> {@link TextFormatting}
 * conversion that {@code mcjty.theoneprobe.apiimpl.client.ElementTextRender} needs. The full
 * Forge-config loading/gui machinery is intentionally omitted — the shim delegates rendering
 * to Jade and has no user-facing config of its own.
 */
public class ConfigSetup {

    public static int loggingThrowableTimeout = 20000;

    public static final int PROBE_NOTNEEDED = 0;
    public static final int PROBE_NEEDED = 1;
    public static final int PROBE_NEEDEDHARD = 2;
    public static final int PROBE_NEEDEDFOREXTENDED = 3;
    public static int needsProbe = PROBE_NEEDEDFOREXTENDED;

    public static boolean extendedInMain = false;
    public static int timeout = 300;
    public static int waitingForServerTimeout = 2000;
    public static int maxPacketToServer = 20000;

    public static boolean supportBaubles = true;
    public static boolean spawnNote = true;

    public static int showSmallChestContentsWithoutSneaking = 0;
    // NOTE: "Thresshold" is a typo in the original TOP and integrations reference it as-is.
    public static int showItemDetailThresshold = 4;
    public static String[] showContentsWithoutSneaking = {"storagedrawers:basicDrawers", "storagedrawersextra:extra_drawers"};
    public static String[] dontShowContentsUnlessSneaking = {};
    public static String[] dontSendNBT = {};

    public static float probeDistance = 6;
    public static boolean showLiquids = false;
    public static boolean isVisible = true;
    public static boolean compactEqualStacks = true;
    public static boolean holdKeyToMakeVisible = false;

    public static boolean showDebugInfo = true;

    public static int showBreakProgress = 1;    // 0 == off, 1 == bar, 2 == text
    public static boolean harvestStyleVanilla = true;

    public static int chestContentsBorderColor = 0xff006699;

    public static float tooltipScale = 1.0f;

    // RF bar colors (values from real TOP's ConfigSetup)
    public static int rfbarFilledColor = 0xffdd0000;
    public static int rfbarAlternateFilledColor = 0xff430000;
    public static int rfbarBorderColor = 0xff555555;
    public static mcjty.theoneprobe.api.NumberFormat rfFormat = mcjty.theoneprobe.api.NumberFormat.COMPACT;

    // Tank bar colors (values from real TOP's ConfigSetup)
    public static int tankbarFilledColor = 0xff0000dd;
    public static int tankbarAlternateFilledColor = 0xff000043;
    public static int tankbarBorderColor = 0xff555555;
    public static mcjty.theoneprobe.api.NumberFormat tankFormat = mcjty.theoneprobe.api.NumberFormat.COMPACT;

    public static Map<TextStyleClass, String> defaultTextStyleClasses = new HashMap<>();
    public static Map<TextStyleClass, String> textStyleClasses = new HashMap<>();

    static {
        defaultTextStyleClasses.put(TextStyleClass.NAME, "white");
        defaultTextStyleClasses.put(TextStyleClass.MODNAME, "blue,italic");
        defaultTextStyleClasses.put(TextStyleClass.ERROR, "red,bold");
        defaultTextStyleClasses.put(TextStyleClass.WARNING, "yellow");
        defaultTextStyleClasses.put(TextStyleClass.OK, "green");
        defaultTextStyleClasses.put(TextStyleClass.INFO, "white");
        defaultTextStyleClasses.put(TextStyleClass.INFOIMP, "blue");
        defaultTextStyleClasses.put(TextStyleClass.OBSOLETE, "gray,strikethrough");
        defaultTextStyleClasses.put(TextStyleClass.LABEL, "gray");
        defaultTextStyleClasses.put(TextStyleClass.PROGRESS, "white");
        textStyleClasses = new HashMap<>(defaultTextStyleClasses);
    }

    public static String getTextStyle(TextStyleClass styleClass) {
        if (textStyleClasses.containsKey(styleClass)) {
            return configToTextFormat(textStyleClasses.get(styleClass));
        }
        return "";
    }

    public static String configToTextFormat(String input) {
        if ("context".equals(input)) {
            return "context";
        }
        StringBuilder builder = new StringBuilder();
        String[] splitted = StringUtils.split(input, ',');
        for (String s : splitted) {
            TextFormatting format = TextFormatting.getValueByName(s);
            if (format != null) {
                builder.append(format.toString());
            }
        }
        return builder.toString();
    }

    private ConfigSetup() {
    }
}
