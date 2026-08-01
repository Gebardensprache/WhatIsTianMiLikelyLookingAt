package snownee.jade.api;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;

/**
 * A server data provider that serializes its payload with a {@link DataCodec}.
 *
 * @param <T> accessor type
 * @param <D> streamed data type
 */
public interface StreamServerDataProvider<T extends Accessor<?>, D> extends IServerDataProvider<T> {

	/**
	 * Appends the encoded streamed data to the server payload.
	 *
	 * @param data server payload
	 * @param accessor accessor providing the data
	 */
	@Override
	default void appendServerData(NBTTagCompound data, T accessor) {
		D value = streamData(accessor);
		if (value != null) {
			data.setTag(getUid().toString(), accessor.encodeAsNbt(streamCodec(), value));
		}
	}

	/**
	 * Decodes streamed data from the accessor's server payload.
	 *
	 * @param accessor accessor providing the server payload
	 * @return decoded data, if present
	 */
	default Optional<D> decodeFromData(T accessor) {
		NBTBase tag = accessor.getServerData().getTag(getUid().toString());
		if (tag == null) {
			return Optional.empty();
		}
		return accessor.decodeFromNbt(streamCodec(), tag);
	}

	/**
	 * Returns the streamed data to encode.
	 *
	 * @param accessor accessor providing the data
	 * @return streamed data, or {@code null} if nothing should be sent
	 */
	@Nullable
	D streamData(T accessor);

	/**
	 * Returns the codec used to encode and decode the payload.
	 *
	 * @return data codec
	 */
	DataCodec<D> streamCodec();
}
