package snownee.jade.util;

import java.util.function.UnaryOperator;

/**
 * 1.12.2: fastutil 8.2+ adds {@code FloatUnaryOperator}; the 1.12.2-bundled
 * fastutil (7.1.0) does not ship it. Mirrors the fastutil contract exactly:
 * the primitive {@link #applyAsFloat(float)} plus the boxed
 * {@link UnaryOperator<Float>} supertype, so both primitive call sites and
 * boxed consumers compile -- same treatment as {@link ToFloatFunction}.
 */
@FunctionalInterface
public interface FloatUnaryOperator extends UnaryOperator<Float> {
	float applyAsFloat(float value);

	@Override
	default Float apply(Float value) {
		return applyAsFloat(value);
	}

	static FloatUnaryOperator identity() {
		return value -> value;
	}
}
