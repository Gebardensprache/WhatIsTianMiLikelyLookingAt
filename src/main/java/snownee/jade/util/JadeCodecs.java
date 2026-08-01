package snownee.jade.util;

import java.util.Optional;
import java.util.OptionalInt;

import org.jspecify.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.DataCodec;
import snownee.jade.api.config.IgnoreList;

public class JadeCodecs {

	public static final Codec<ResourceLocation> RESOURCE_LOCATION = Codec.STRING.xmap(ResourceLocation::new, ResourceLocation::toString);

	public static final Codec<OptionalInt> OPTIONAL_INT = new Codec<>() {
		@Override
		public <T> DataResult<Pair<OptionalInt, T>> decode(DynamicOps<T> ops, T input) {
			return DataResult.success(ops.getNumberValue(input)
					.mapOrElse(
							number -> Pair.of(OptionalInt.of(number.intValue()), ops.empty()),
							ignored -> Pair.of(OptionalInt.empty(), ops.empty())));
		}

		@Override
		public <T> DataResult<T> encode(OptionalInt input, DynamicOps<T> ops, T prefix) {
			if (input.isPresent()) {
				return DataResult.success(ops.createInt(input.getAsInt()));
			} else {
				return DataResult.success(ops.empty());
			}
		}
	};

	public static final PrimitiveCodec<Object> PRIMITIVE = new PrimitiveCodec<>() {
		@Override
		public <T> DataResult<Object> read(DynamicOps<T> ops, T input) {
			{
				DataResult<Boolean> result = ops.getBooleanValue(input);
				if (result.isSuccess()) {
					return result.map($ -> $);
				}
			}
			{
				DataResult<Number> result = ops.getNumberValue(input);
				if (result.isSuccess()) {
					return result.map($ -> $);
				}
			}
			{
				DataResult<String> result = ops.getStringValue(input);
				if (result.isSuccess()) {
					return result.map($ -> $);
				}
			}
			return DataResult.error(() -> "Not a primitive value: " + input);
		}

		@Override
		public <T> T write(DynamicOps<T> ops, Object value) {
			return switch (value) {
				case Boolean b -> ops.createBoolean(b);
				case Number number -> ops.createNumeric(number);
				case String s -> ops.createString(s);
				case null, default -> throw new IllegalArgumentException("Not a primitive value: " + value);
			};
		}
	};
	public static final DataCodec<Object> PRIMITIVE_STREAM_CODEC = new DataCodec<>() {
		@Override
		public Object decode(PacketBuffer buf) {
			byte b = buf.readByte();
			if (b == 0) {
				return false;
			} else if (b == 1) {
				return true;
			} else if (b == 2) {
				return buf.readVarInt();
			} else if (b == 3) {
				return buf.readFloat();
			} else if (b == 4) {
				return buf.readString(32767);
			} else if (b >= 20) {
				// 1.12.2: compact ints encode as i + 20 for i in [0, 107], so byte 20
				// means 0. Upstream's `> 20` guard made the value 0 undecodable.
				return b - 20;
			}
			throw new IllegalArgumentException("Unknown primitive type: " + b);
		}

		@Override
		public void encode(PacketBuffer buf, Object o) {
			switch (o) {
				case Boolean b -> buf.writeByte(b ? 1 : 0);
				case Number n -> {
					float f = n.floatValue();
					if (f != (int) f) {
						buf.writeByte(3);
						buf.writeFloat(f);
					} else {
						// 1.12.2: upstream lacked this else, writing both a float and an
						// int for non-integral values and desyncing the reader.
						int i = n.intValue();
						if (i <= Byte.MAX_VALUE - 20 && i >= 0) {
							buf.writeByte(i + 20);
						} else {
							buf.writeByte(2);
							buf.writeVarInt(i);
						}
					}
				}
				case String s -> {
					buf.writeByte(4);
					buf.writeString(s);
				}
				case Enum<?> anEnum -> {
					buf.writeByte(4);
					buf.writeString(anEnum.name());
				}
				case null -> throw new NullPointerException();
				default -> throw new IllegalArgumentException("Unknown primitive type: %s (%s)".formatted(o, o.getClass()));
			}
		}
	};

	public static Codec<IgnoreList> ignoreList() {
		return RecordCodecBuilder.create(i -> i.group(
				Codec.STRING.optionalFieldOf("__comment", "").forGetter($ -> {
					// 1.12.2: no Language.getOrDefault; use a static English default
					return "This is an ignore list for the target of Jade. You can add registry ids to the \"values\" list.";
				}),
				Codec.STRING.listOf().fieldOf("values").forGetter($ -> $.values),
				Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("version", 1).forGetter($ -> $.version)
		).apply(
				i, (comment, values, version) -> {
					IgnoreList ignoreList = new IgnoreList();
					ignoreList.values = values;
					ignoreList.version = version;
					return ignoreList;
				}));
	}

	public static Codec<int[]> intArrayCodec(int size, Codec<Integer> codec) {
		return Codec.list(codec).flatXmap(
				$ -> {
					if (size != 0 && size != $.size()) {
						return DataResult.error(() -> "Expected array of length " + size + ", got " + $.size());
					}
					int[] array = new int[size == 0 ? $.size() : size];
					for (int i = 0; i < array.length; i++) {
						array[i] = $.get(i);
					}
					return DataResult.success(array);
				}, $ -> {
					if (size != 0 && size != $.length) {
						return DataResult.error(() -> "Expected array of length " + size + ", got " + $.length);
					}
					IntList list = new IntArrayList($.length);
					for (int i : $) {
						list.add(i);
					}
					return DataResult.success(list);
				});
	}

	public static Codec<float[]> floatArrayCodec(int size, Codec<Float> codec) {
		return Codec.list(codec).flatXmap(
				$ -> {
					if (size != 0 && size != $.size()) {
						return DataResult.error(() -> "Expected array of length " + size + ", got " + $.size());
					}
					float[] array = new float[size == 0 ? $.size() : size];
					for (int i = 0; i < array.length; i++) {
						array[i] = $.get(i);
					}
					return DataResult.success(array);
				}, $ -> {
					if (size != 0 && size != $.length) {
						return DataResult.error(() -> "Expected array of length " + size + ", got " + $.length);
					}
					FloatList list = new FloatArrayList($.length);
					for (float f : $) {
						list.add(f);
					}
					return DataResult.success(list);
				});
	}

	public static Optional<int[]> nullableClone(int @Nullable [] array) {
		if (array == null) {
			return Optional.empty();
		}
		return Optional.of(array.clone());
	}

	public static Optional<float[]> nullableClone(float @Nullable [] array) {
		if (array == null) {
			return Optional.empty();
		}
		return Optional.of(array.clone());
	}

	public static <T> T createFromEmptyMap(Codec<T> codec) {
		return codec.parse(JsonOps.INSTANCE, JsonOps.INSTANCE.emptyMap()).getOrThrow();
	}
}
