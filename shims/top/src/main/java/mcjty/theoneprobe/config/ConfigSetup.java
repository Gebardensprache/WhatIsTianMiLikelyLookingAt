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
