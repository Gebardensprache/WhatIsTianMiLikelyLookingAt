package snownee.jade.api.ui;

import java.util.function.IntFunction;

import net.minecraft.network.PacketBuffer;
import snownee.jade.api.DataCodec;

/**
 * Severity or presentation mode for a tooltip section.
 */
public enum MessageType {
	NORMAL, INFO, TITLE, SUCCESS, WARNING, DANGER, FAILURE;

	public static final IntFunction<MessageType> BY_ID = i -> {
		if (i < 0 || i >= values().length) {
			return NORMAL;
		}
		return values()[i];
	};
	public static final DataCodec<MessageType> STREAM_CODEC = new DataCodec<>() {
		@Override
		public MessageType decode(PacketBuffer buf) {
			return BY_ID.apply(buf.readVarInt());
		}

		@Override
		public void encode(PacketBuffer buf, MessageType value) {
			buf.writeVarInt(value.ordinal());
		}
	};

	public static MessageType parse(String s) {
		try {
			return valueOf(s);
		} catch (Exception ignored) {
			return NORMAL;
		}
	}
}
