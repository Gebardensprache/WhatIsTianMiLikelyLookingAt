package snownee.jade.api;

import java.io.IOException;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/**
 * Minimal codec-like interface for encoding and decoding values via {@link PacketBuffer}.
 * <p>
 * Used by {@link StreamServerDataProvider} for server-payload streaming. Implementations
 * need only provide the pair of read/write operations.
 *
 * @param <D> the data type
 */
public interface DataCodec<D> {

	/**
	 * Decodes a value from the given buffer.
	 *
	 * @param buf the packet buffer to read from
	 * @return the decoded value
	 */
	D decode(PacketBuffer buf);

	/**
	 * Encodes the given value into the given buffer.
	 *
	 * @param buf   the packet buffer to write to
	 * @param value the value to encode
	 */
	void encode(PacketBuffer buf, D value);

	/**
	 * Reads a compound tag, wrapping the 1.12.2 checked {@link IOException}.
	 *
	 * @param buf the packet buffer to read from
	 * @return the compound tag, or {@code null}
	 */
	static NBTTagCompound readTag(PacketBuffer buf) {
		try {
			return buf.readCompoundTag();
		} catch (IOException e) {
			throw new RuntimeException("Failed to read NBT tag from buffer", e);
		}
	}

	/**
	 * Reads an item stack, wrapping the 1.12.2 checked {@link IOException}.
	 *
	 * @param buf the packet buffer to read from
	 * @return the item stack
	 */
	static ItemStack readStack(PacketBuffer buf) {
		try {
			return buf.readItemStack();
		} catch (IOException e) {
			throw new RuntimeException("Failed to read item stack from buffer", e);
		}
	}
}
