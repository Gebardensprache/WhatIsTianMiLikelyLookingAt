package snownee.jade.util;

import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.Style;
import snownee.jade.api.theme.SneakyDetails;
import snownee.jade.api.theme.TextSetting;
import snownee.jade.api.theme.Theme;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.Color;
import snownee.jade.api.ui.ColorPalette;

public class JadeClientCodecs {
	public static final Codec<TextSetting> TEXT_SETTING = RecordCodecBuilder.create(i -> i.group(
			ColorPalette.CODEC.optionalFieldOf("colors", ColorPalette.DEFAULT).forGetter(TextSetting::colors),
			Codec.BOOL.optionalFieldOf("shadow", true).forGetter(TextSetting::shadow),
			// 1.12.2: no Style.Serializer codec; use a placeholder
			Codec.STRING.optionalFieldOf("modNameStyle").xmap(
					$ -> Optional.<Object>empty(),
					$ -> Optional.<String>empty())
					.forGetter($ -> Optional.of($.modNameStyle())),
			Color.CODEC.optionalFieldOf("itemAmountColor", 0xFFFFFFFF).forGetter(TextSetting::itemAmountColor)
	).apply(i, (colors, shadow, modNameStyle, itemAmountColor) -> {
		// 1.12.2: Style codec not available; construct TextSetting with empty style
		@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
		Style emptyStyle = new Style();
		return new TextSetting(colors, shadow, Optional.of(emptyStyle), itemAmountColor);
	}));

	public static final MapCodec<Theme> THEME = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.optionalFieldOf("styleName", "jade.default").forGetter($ -> $.styleName),
			BoxStyle.CODEC.fieldOf("tooltipStyle").forGetter($ -> $.tooltipStyle),
			BoxStyle.CODEC.optionalFieldOf("nestedBoxStyle", BoxStyle.DEFAULT_NESTED_BOX).forGetter($ -> $.nestedBoxStyle),
			BoxStyle.CODEC.optionalFieldOf("viewGroupStyle", BoxStyle.DEFAULT_VIEW_GROUP).forGetter($ -> $.viewGroupStyle),
			TEXT_SETTING.optionalFieldOf("text", TextSetting.DEFAULT).forGetter($ -> $.text),
			Codec.floatRange(0, 1).optionalFieldOf("changeOpacity", 0F).forGetter($ -> $.changeOpacity),
			Codec.BOOL.optionalFieldOf("lightColorScheme", false).forGetter($ -> $.lightColorScheme),
			JadeCodecs.RESOURCE_LOCATION.optionalFieldOf("iconSlotSprite").forGetter($ -> Optional.ofNullable($.iconSlotSprite)),
			Codec.INT.optionalFieldOf("iconSlotInflation", 0).forGetter($ -> $.iconSlotInflation),
			SneakyDetails.CODEC.optionalFieldOf("sneakyDetails", SneakyDetails.DEFAULT).forGetter($ -> $.sneakyDetails),
			ColorPalette.CODEC.optionalFieldOf("progressColors", ColorPalette.DEFAULT).forGetter($ -> $.progressColors),
			Codec.unboundedMap(JadeCodecs.RESOURCE_LOCATION, JadeCodecs.RESOURCE_LOCATION)
					.optionalFieldOf("spriteMapping", Map.of())
					.forGetter($ -> $.spriteMapping)
	).apply(i, Theme::new));

	public record ThemeHolder(int version, boolean autoEnable, Theme theme) {}
}
