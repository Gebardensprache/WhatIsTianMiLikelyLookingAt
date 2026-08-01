package snownee.jade.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 1.12.2 network channel for Jade.
 *
 * <p>Replaces the modern payload-type registration: in 1.12.2 packets
 * are identified by their discriminator byte, assigned here by registration order.
 * Discriminators must stay stable across client and server.
 */
public final class JadeNetwork {

	/**
	 * Jade's packet channel.
	 */
	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel("jade");

	private static boolean registered;

	private JadeNetwork() {
	}

	/**
	 * Registers all Jade messages. Idempotent.
	 */
	public static void init() {
		if (registered) {
			return;
		}
		registered = true;
		NETWORK.registerMessage(ClientHandshakePacket.Handler.class, ClientHandshakePacket.class, 0, Side.SERVER);
		NETWORK.registerMessage(ServerHandshakePacket.Handler.class, ServerHandshakePacket.class, 1, Side.CLIENT);
		NETWORK.registerMessage(RequestBlockPacket.Handler.class, RequestBlockPacket.class, 2, Side.SERVER);
		NETWORK.registerMessage(RequestEntityPacket.Handler.class, RequestEntityPacket.class, 3, Side.SERVER);
		NETWORK.registerMessage(ReceiveDataPacket.Handler.class, ReceiveDataPacket.class, 4, Side.CLIENT);
		NETWORK.registerMessage(ShowOverlayPacket.Handler.class, ShowOverlayPacket.class, 5, Side.CLIENT);
	}

	/**
	 * Sends a message to a specific player.
	 *
	 * @param message the message to send
	 * @param player  the recipient
	 */
	public static void sendTo(IMessage message, EntityPlayerMP player) {
		NETWORK.sendTo(message, player);
	}

	/**
	 * Sends a message to the server.
	 *
	 * @param message the message to send
	 */
	public static void sendToServer(IMessage message) {
		NETWORK.sendToServer(message);
	}
}
