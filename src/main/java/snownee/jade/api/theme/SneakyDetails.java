package snownee.jade.api.theme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.JadeIds;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.overlay.OverlayRenderer;

/**
 * Renders the little "sneaky details" indicator used by themed tooltips.
 * <p>
 * 1.12.2: {@code GuiGraphicsExtractor} parameter dropped from
 * {@link #render(float, BoxElement)}. Rendering is done directly via
 * {@link DisplayHelper#blitSprite(ResourceLocation, int, int, int, int, int)}
 * following the same pattern as {@code BoxStyle.render(...)}.
 * {@code RenderPipelines} and {@code ARGB} are not available in 1.12.2;
 * color packing is done by hand. {@code ExtraCodecs.POSITIVE_FLOAT} replaced
 * with {@code Codec.floatRange} (available through shadowed DFU).
 */
public interface SneakyDetails {
	SneakyDetails DEFAULT = new Simple(JadeIds.JADE("details_arrow"), 7, 5, 0, 0, "breath", 1F, 12F);
	Codec<SneakyDetails> CODEC = Codec.STRING.dispatch(SneakyDetails::type, SneakyDetails::codec);

	static MapCodec<? extends SneakyDetails> codec(String s) {
		if ("simple".equals(s)) {
			return Simple.CODEC;
		}
		throw new UnsupportedOperationException();
	}

	void render(float partialTicks, BoxElement element);

	String type();

	record Simple(
			ResourceLocation sprite,
			int width,
			int height,
			float offsetX,
			float offsetY,
			String animation,
			float animationDistance,
			float animationPeriod) implements SneakyDetails {
		private static final Codec<ResourceLocation> RESOURCE_LOCATION_CODEC = Codec.STRING.xmap(ResourceLocation::new, ResourceLocation::toString);
		public static final MapCodec<Simple> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
				RESOURCE_LOCATION_CODEC.fieldOf("sprite").forGetter(Simple::sprite),
				Codec.INT.fieldOf("width").forGetter(Simple::width),
				Codec.INT.fieldOf("height").forGetter(Simple::height),
				Codec.FLOAT.optionalFieldOf("offsetX", 0F).forGetter(Simple::offsetX),
				Codec.FLOAT.optionalFieldOf("offsetY", 0F).forGetter(Simple::offsetY),
				Codec.STRING.optionalFieldOf("animation", "").forGetter(Simple::animation),
				Codec.floatRange(Float.MIN_VALUE, Float.MAX_VALUE).optionalFieldOf("animationDistance", 1F).forGetter(Simple::animationDistance),
				Codec.floatRange(Float.MIN_VALUE, Float.MAX_VALUE).optionalFieldOf("animationPeriod", 12F).forGetter(Simple::animationPeriod)
		).apply(i, Simple::new));

		@Override
		public void render(float partialTicks, BoxElement element) {
			float x = element.getX() + element.getWidth() / 2f - width / 2f + offsetX;
			float y = element.getY() + element.getHeight() - height / 2f + offsetY;
			float alpha = 1f;
			if ("breath".equals(animation)) {
				float breath = (OverlayRenderer.ticks / 5) % animationPeriod - 2; //range: -2 to 6
				if (breath > 4) {
					return;
				}
				alpha = 1 - Math.abs(breath) / 2;
				y += animationDistance * breath;
				if (alpha < 0.016f) {
					return; // too transparent
				}
			}
			int col = ((int) (alpha * 255) << 24) | 0xFFFFFF;
			DisplayHelper.INSTANCE.blitSprite(sprite, Math.round(x), Math.round(y), width, height, col);
		}

		@Override
		public String type() {
			return "simple";
		}
	}
}
