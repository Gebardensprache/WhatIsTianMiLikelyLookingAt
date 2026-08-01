package snownee.jade.network;

import java.util.List;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import snownee.jade.Jade;
import snownee.jade.addon.harvest.LootTableMineableCollector;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.impl.config.ServerPluginConfig;
import snownee.jade.util.CommonProxy;

// This class of structure should not be changed
public class ClientHandshakePacket implements IMessage {

	private String protocolVersion;

	public ClientHandshakePacket() {
	}

	public ClientHandshakePacket(String protocolVersion) {
		this.protocolVersion = protocolVersion;
	}

	public String protocolVersion() {
		return protocolVersion;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		protocolVersion = packet.readString(32767);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		new PacketBuffer(buf).writeString(protocolVersion);
	}

	public static void handle(ClientHandshakePacket message, ServerPayloadContext context) {
		context.execute(() -> {
			EntityPlayerMP player = context.player();
			if (!Jade.PROTOCOL_VERSION.equals(message.protocolVersion)) {
				String version = CommonProxy.getModVersion(Jade.ID).orElse("UNKNOWN");
				player.sendMessage(new TextComponentTranslation("jade.protocolMismatch", version));
				return;
			}
			CommonProxy.setConnected(player, true);
			Map<ResourceLocation, Object> configs = ServerPluginConfig.instance().values();
			List<Block> shearableBlocks = LootTableMineableCollector.getShearableBlocks();
			if (!configs.isEmpty()) {
				Jade.LOGGER.debug("Syncing config to {} ({})", player.getName(), player.getGameProfile().getId());
			}
			List<ResourceLocation> blockProviderIds = WailaCommonRegistration.instance().blockDataProviders.mappedIds();
			List<ResourceLocation> entityProviderIds = WailaCommonRegistration.instance().entityDataProviders.mappedIds();
			context.sendPacket(new ServerHandshakePacket(configs, shearableBlocks, blockProviderIds, entityProviderIds));
		});
	}

	public static class Handler implements IMessageHandler<ClientHandshakePacket, IMessage> {
		@Override
		public IMessage onMessage(ClientHandshakePacket message, MessageContext ctx) {
			handle(message, ServerPayloadContext.of(ctx.getServerHandler().player));
			return null;
		}
	}
}
