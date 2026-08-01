package snownee.jade.network;

import net.minecraft.client.Minecraft;
import snownee.jade.util.ClientProxy;

/**
 * Client-side packet handling context.
 *
 * <p>1.12.2: {@code client.execute} becomes
 * {@link Minecraft#addScheduledTask(Runnable)}.
 */
public interface ClientPayloadContext {
	static ClientPayloadContext of(Minecraft client) {
		return client.player != null
				? runnable -> client.addScheduledTask(() -> ClientProxy.runWithContext(client, runnable))
				: runnable -> client.addScheduledTask(runnable);
	}

	void execute(Runnable runnable);
}
