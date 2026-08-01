package snownee.jade.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import snownee.jade.util.CommonProxy;

/**
 * Server-side packet handling context.
 *
 * <p>1.12.2: replaces the modern {@code ServerPayloadContext} backed by
 * {@code ClientboundCustomPayloadPacket}; dispatch goes through
 * {@link JadeNetwork} instead.
 */
public interface ServerPayloadContext {
	static ServerPayloadContext of(EntityPlayerMP player) {
		return () -> player;
	}

	default void execute(Runnable runnable) {
		CommonProxy.runWithContext(this, runnable);
	}

	default void sendPacket(IMessage message) {
		JadeNetwork.sendTo(message, player());
	}

	EntityPlayerMP player();
}
