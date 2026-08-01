package snownee.jade.util;

import java.util.function.Consumer;

/**
 * 1.12.2: fastutil 8.2+ adds {@code FloatConsumer}; the 1.12.2-bundled
 * fastutil (7.1.0) does not ship it. Mirrors the fastutil contract exactly:
 * the primitive {@link #accept(float)} plus the boxed
 * {@link Consumer<Float>} supertype, so both primitive call sites and boxed
 * consumers compile -- same treatment as {@link ToFloatFunction}.
 */
@FunctionalInterface
public interface FloatConsumer extends Consumer<Float> {
	void accept(float value);

	@Override
	default void accept(Float value) {
		accept(value.floatValue());
	}
}
