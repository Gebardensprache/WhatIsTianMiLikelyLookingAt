package snownee.jade.network;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.impl.BlockAccessorImpl;
import snownee.jade.impl.WailaCommonRegistration;

public class RequestBlockPacket implements IMessage {

	private BlockAccessorImpl.SyncData data;
	private List<IServerDataProvider<BlockAccessor>> dataProviders;

	public RequestBlockPacket() {
	}

	public RequestBlockPacket(BlockAccessorImpl.SyncData data, List<IServerDataProvider<BlockAccessor>> dataProviders) {
		this.data = data;
		this.dataProviders = dataProviders;
	}

	public BlockAccessorImpl.SyncData data() {
		return data;
	}

	public List<IServerDataProvider<BlockAccessor>> dataProviders() {
		return dataProviders;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		data = BlockAccessorImpl.SyncData.STREAM_CODEC.decode(packet);
		int size = packet.readVarInt();
		dataProviders = Lists.newArrayListWithExpectedSize(size);
		for (int i = 0; i < size; i++) {
			dataProviders.add(Objects.requireNonNull(
					WailaCommonRegistration.instance().blockDataProviders.idMapper().byId(packet.readVarInt())));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		BlockAccessorImpl.SyncData.STREAM_CODEC.encode(packet, data);
		packet.writeVarInt(dataProviders.size());
		for (IServerDataProvider<BlockAccessor> provider : dataProviders) {
			packet.writeVarInt(WailaCommonRegistration.instance().blockDataProviders.idMapper().getIdOrThrow(provider));
		}
	}

	public static void handle(RequestBlockPacket message, ServerPayloadContext context) {
		BlockAccessorImpl.handleRequest(message, context, tag -> ReceiveDataPacket.send(tag, context));
	}

	public static class Handler implements IMessageHandler<RequestBlockPacket, IMessage> {
		@Override
		public IMessage onMessage(RequestBlockPacket message, MessageContext ctx) {
			handle(message, ServerPayloadContext.of(ctx.getServerHandler().player));
			return null;
		}
	}
}
