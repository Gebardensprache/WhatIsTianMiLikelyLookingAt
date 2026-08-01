package snownee.jade.api;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

/**
 * Minimal 1.12.2 backport of modern {@code net.minecraft.util.StringRepresentable}.
 */
public interface StringRepresentable {
	String getSerializedName();

	/**
	 * Creates a codec for a StringRepresentable enum.
	 */
	static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnum(E[] values) {
		return Codec.STRING.flatXmap(
				name -> {
					for (E value : values) {
						if (value.getSerializedName().equals(name)) {
							return DataResult.success(value);
						}
					}
					return DataResult.error(() -> "Unknown enum value: " + name);
				},
				value -> DataResult.success(value.getSerializedName()));
	}

	/**
	 * Creates a codec for a StringRepresentable enum, optionally allowing legacy int IDs.
	 */
	static <E extends Enum<E> & StringRepresentable> Codec<E> fromEnumWithAlias(E[] values, java.util.function.Function<Integer, E> byId) {
		Codec<E> stringCodec = fromEnum(values);
		Codec<E> intCodec = Codec.INT.flatXmap(
				id -> {
					E value = byId.apply(id);
					if (value == null) {
						return DataResult.error(() -> "Unknown enum id: " + id);
					}
					return DataResult.success(value);
				},
				value -> DataResult.success(value.ordinal()));
		return Codec.either(stringCodec, intCodec).xmap(
				either -> either.map(l -> l, r -> r),
				value -> Either.left(value));
	}
}
