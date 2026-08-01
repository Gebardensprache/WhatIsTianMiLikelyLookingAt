package snownee.jade.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Objects;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.api.DataCodec;

public class ReceiveDataPacket implements IMessage {
	public static final int MAX_SIZE = 16 * 1024;
	private static int spamCount;

	private NBTTagCompound tag;

	public ReceiveDataPacket() {
	}

	public ReceiveDataPacket(NBTTagCompound tag) {
		this.tag = tag;
	}

	public NBTTagCompound tag() {
		return tag;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		tag = DataCodec.readTag(new PacketBuffer(buf));
	}

	@Override
	public void toBytes(ByteBuf buf) {
		new PacketBuffer(buf).writeCompoundTag(tag);
	}

	public static void handle(ReceiveDataPacket message, ClientPayloadContext context) {
		context.execute(() -> {
			JadeClient.tickHandler().setData(message.tag);
		});
	}

	public static void send(NBTTagCompound tag, ServerPayloadContext context) {
		int size = sizeInBytes(tag);
		if (size > MAX_SIZE) {
			if (spamCount++ < 1) {
				Jade.LOGGER.debug("Data size is too large: {}, max: {}, data: {}", size, MAX_SIZE, tag);
			}
			int c = 0;
			do {
				if (++c > 10) {
					return;
				}
				removeLargest(tag, 0, 1);
			} while (sizeInBytes(tag) > MAX_SIZE);
		}
		context.sendPacket(new ReceiveDataPacket(tag));
	}

	/**
	 * 1.12.2 replacement for the modern {@code Tag.sizeInBytes()}: serializes the tag
	 * and measures the result.
	 *
	 * @param tag the tag to measure
	 * @return the serialized size in bytes, or {@link Integer#MAX_VALUE} on failure
	 */
	private static int sizeInBytes(NBTTagCompound tag) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			CompressedStreamTools.write(tag, new DataOutputStream(out));
			return out.size();
		} catch (Exception e) {
			// Treat an unmeasurable tag as oversized so the caller trims it.
			return Integer.MAX_VALUE;
		}
	}

	/**
	 * Measures a child tag by wrapping it in a compound and subtracting the wrapper cost.
	 * Only relative ordering matters here, so an approximate size is sufficient.
	 *
	 * @param child the child tag to measure
	 * @return the approximate serialized size in bytes
	 */
	private static int sizeInBytes(NBTBase child) {
		NBTTagCompound wrapper = new NBTTagCompound();
		wrapper.setTag("", child);
		return sizeInBytes(wrapper);
	}

	private static boolean removeLargest(NBTTagCompound tag, int depth, int maxDepth) {
		int largestSize = 0;
		String largestKey = null;
		NBTBase largestValue = null;
		for (String key : tag.getKeySet()) {
			NBTBase childTag = Objects.requireNonNull(tag.getTag(key));
			int size = sizeInBytes(childTag);
			if (size > largestSize) {
				largestSize = size;
				largestKey = key;
				largestValue = childTag;
			}
		}
		if (largestKey == null) {
			return false;
		}
		if (depth < maxDepth && largestValue instanceof NBTTagCompound) {
			if (!removeLargest((NBTTagCompound) largestValue, depth + 1, maxDepth)) {
				tag.removeTag(largestKey);
			}
		} else {
			tag.removeTag(largestKey);
		}
		return true;
	}

	public static class Handler implements IMessageHandler<ReceiveDataPacket, IMessage> {
		@Override
		public IMessage onMessage(ReceiveDataPacket message, MessageContext ctx) {
			handle(message, ClientPayloadContext.of(Minecraft.getMinecraft()));
			return null;
		}
	}
}
