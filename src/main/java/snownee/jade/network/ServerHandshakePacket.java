package snownee.jade.network;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import snownee.jade.Jade;
import snownee.jade.addon.harvest.HarvestToolProvider;
import snownee.jade.impl.ObjectDataCenter;
import snownee.jade.impl.WailaClientRegistration;
import snownee.jade.impl.WailaCommonRegistration;
import snownee.jade.util.JadeCodecs;

public class ServerHandshakePacket implements IMessage {

	private Map<ResourceLocation, Object> serverConfig;
	private List<Block> shearableBlocks;
	private List<ResourceLocation> blockProviderIds;
	private List<ResourceLocation> entityProviderIds;

	public ServerHandshakePacket() {
	}

	public ServerHandshakePacket(
			Map<ResourceLocation, Object> serverConfig,
			List<Block> shearableBlocks,
			List<ResourceLocation> blockProviderIds,
			List<ResourceLocation> entityProviderIds) {
		this.serverConfig = serverConfig;
		this.shearableBlocks = shearableBlocks;
		this.blockProviderIds = blockProviderIds;
		this.entityProviderIds = entityProviderIds;
	}

	public Map<ResourceLocation, Object> serverConfig() {
		return serverConfig;
	}

	public List<Block> shearableBlocks() {
		return shearableBlocks;
	}

	public List<ResourceLocation> blockProviderIds() {
		return blockProviderIds;
	}

	public List<ResourceLocation> entityProviderIds() {
		return entityProviderIds;
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		int configSize = packet.readVarInt();
		serverConfig = Maps.newHashMapWithExpectedSize(configSize);
		for (int i = 0; i < configSize; i++) {
			ResourceLocation key = new ResourceLocation(packet.readString(32767));
			serverConfig.put(key, JadeCodecs.PRIMITIVE_STREAM_CODEC.decode(packet));
		}
		int blockSize = packet.readVarInt();
		shearableBlocks = Lists.newArrayListWithExpectedSize(blockSize);
		for (int i = 0; i < blockSize; i++) {
			Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(packet.readString(32767)));
			if (block != null) {
				shearableBlocks.add(block);
			}
		}
		blockProviderIds = readIds(packet);
		entityProviderIds = readIds(packet);
	}

	@Override
	public void toBytes(ByteBuf buf) {
		PacketBuffer packet = new PacketBuffer(buf);
		packet.writeVarInt(serverConfig.size());
		for (Map.Entry<ResourceLocation, Object> entry : serverConfig.entrySet()) {
			packet.writeString(entry.getKey().toString());
			JadeCodecs.PRIMITIVE_STREAM_CODEC.encode(packet, entry.getValue());
		}
		packet.writeVarInt(shearableBlocks.size());
		for (Block block : shearableBlocks) {
			packet.writeString(String.valueOf(block.getRegistryName()));
		}
		writeIds(packet, blockProviderIds);
		writeIds(packet, entityProviderIds);
	}

	private static List<ResourceLocation> readIds(PacketBuffer packet) {
		int size = packet.readVarInt();
		List<ResourceLocation> ids = Lists.newArrayListWithExpectedSize(size);
		for (int i = 0; i < size; i++) {
			ids.add(new ResourceLocation(packet.readString(32767)));
		}
		return ids;
	}

	private static void writeIds(PacketBuffer packet, List<ResourceLocation> ids) {
		packet.writeVarInt(ids.size());
		for (ResourceLocation id : ids) {
			packet.writeString(id.toString());
		}
	}

	public static void handle(ServerHandshakePacket message, ClientPayloadContext context) {
		context.execute(() -> {
			ObjectDataCenter.serverConnected = true;
			HarvestToolProvider.INSTANCE.setShearableBlocks(message.shearableBlocks);
			WailaClientRegistration.instance().setServerConfig(message.serverConfig);
			WailaCommonRegistration.instance().blockDataProviders.remapIds(message.blockProviderIds);
			WailaCommonRegistration.instance().entityDataProviders.remapIds(message.entityProviderIds);
			Jade.LOGGER.info("Received config from the server: {}", message.serverConfig);
		});
	}

	public static class Handler implements IMessageHandler<ServerHandshakePacket, IMessage> {
		@Override
		public IMessage onMessage(ServerHandshakePacket message, MessageContext ctx) {
			handle(message, ClientPayloadContext.of(Minecraft.getMinecraft()));
			return null;
		}
	}
}
