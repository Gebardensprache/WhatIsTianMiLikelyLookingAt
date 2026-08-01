package snownee.jade.util;

import java.util.function.Consumer;

/**
 * 1.12.2: fastutil 8.2+ adds {@code BooleanConsumer}; the 1.12.2-bundled
 * fastutil (7.1.0) does not ship it. Mirrors the fastutil contract exactly:
 * the primitive {@link #accept(boolean)} plus the boxed
 * {@link Consumer<Boolean>} supertype, so both primitive call sites and boxed
 * consumers compile -- same treatment as {@link ToFloatFunction}.
 */
@FunctionalInterface
public interface BooleanConsumer extends Consumer<Boolean> {
	void accept(boolean value);

	@Override
	default void accept(Boolean value) {
		accept(value.booleanValue());
	}
}
