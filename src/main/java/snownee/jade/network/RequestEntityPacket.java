package snownee.jade.network;

import java.util.List;
import java.util.Objects;

import com.google.common.collect.Lists;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.impl.EntityAccessorImpl;
import snownee.jade.impl.WailaCommonRegistration;

public class RequestEntityPacket implements IMessage {

	private EntityAccessorImpl.SyncData data;
	private List<IServerDataProvider<EntityAccessor>> dataProviders;

	public RequestEntityPacket() {
	}

	public RequestEntityPacket(EntityAccessorImpl.SyncData data, List<IServerDataProvider<EntityAccessor>> dataProviders) {
		this.data = data;
		this.dataProviders = dataProviders;
	}

	public EntityAccessorImpl.SyncData data() {
		return data;
	}

	public List<IServerDataProvider<EntityAccessor>> dataProviders() {
		return dataProviders;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		data = EntityAccessorImpl.SyncData.STREAM_CODEC.decode(packet);
		int size = packet.readVarInt();
		dataProviders = Lists.newArrayListWithExpectedSize(size);
		for (int i = 0; i < size; i++) {
			dataProviders.add(Objects.requireNonNull(
					WailaCommonRegistration.instance().entityDataProviders.idMapper().byId(packet.readVarInt())));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		EntityAccessorImpl.SyncData.STREAM_CODEC.encode(packet, data);
		packet.writeVarInt(dataProviders.size());
		for (IServerDataProvider<EntityAccessor> provider : dataProviders) {
			packet.writeVarInt(WailaCommonRegistration.instance().entityDataProviders.idMapper().getIdOrThrow(provider));
		}
	}

	public static void handle(RequestEntityPacket message, ServerPayloadContext context) {
		EntityAccessorImpl.handleRequest(message, context, tag -> ReceiveDataPacket.send(tag, context));
	}

	public static class Handler implements IMessageHandler<RequestEntityPacket, IMessage> {
		@Override
		public IMessage onMessage(RequestEntityPacket message, MessageContext ctx) {
			handle(message, ServerPayloadContext.of(ctx.getServerHandler().player));
			return null;
		}
	}
}
