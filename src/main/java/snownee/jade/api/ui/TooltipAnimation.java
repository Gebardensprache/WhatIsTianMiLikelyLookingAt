package snownee.jade.api.ui;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Mutable animation state used while showing or hiding the tooltip overlay.
 */
public class TooltipAnimation {
	public final Rect2f expectedRect = new Rect2f();
	public final Rect2f rect = new Rect2f();

	public long startTime = -1;
	public final Rect2f startRect = new Rect2f();
	public float scale = 1;
	public float showHideAlpha;
	public float alpha;

	public <R> @Nullable R mapMousePosition(double x, double y, BiFunction<Double, Double, @Nullable R> consumer) {
		x = (x - rect.getX()) / scale;
		y = (y - rect.getY()) / scale;
		return consumer.apply(x, y);
	}

	/**
	 * @deprecated 1.12.2 has no MouseButtonEvent; use {@link #mapMousePosition(double, double, BiFunction)}.
	 */
	@Deprecated
	public <R> @Nullable R mapMousePosition(Object event, Function<Object, @Nullable R> consumer) {
		throw new UnsupportedOperationException("MouseButtonEvent is not available in 1.12.2");
	}
}
