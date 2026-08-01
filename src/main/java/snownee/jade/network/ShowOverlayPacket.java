package snownee.jade.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import snownee.jade.Jade;
import snownee.jade.api.config.IWailaConfig;

public class ShowOverlayPacket implements IMessage {

	private boolean show;

	public ShowOverlayPacket() {
	}

	public ShowOverlayPacket(boolean show) {
		this.show = show;
	}

	public boolean show() {
		return show;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		show = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(show);
	}

	public static void handle(ShowOverlayPacket message, ClientPayloadContext context) {
		Jade.LOGGER.info("Received request from the server to {} overlay", message.show ? "show" : "hide");
		context.execute(() -> {
			IWailaConfig.get().general().setDisplayTooltip(message.show);
			IWailaConfig.get().save();
		});
	}

	public static class Handler implements IMessageHandler<ShowOverlayPacket, IMessage> {
		@Override
		public IMessage onMessage(ShowOverlayPacket message, MessageContext ctx) {
			handle(message, ClientPayloadContext.of(Minecraft.getMinecraft()));
			return null;
		}
	}
}
