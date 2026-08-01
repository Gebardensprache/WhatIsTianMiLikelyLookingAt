package snownee.jade.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.entity.player.EntityPlayerMP;
import snownee.jade.util.JadeServerPlayer;

@Mixin(EntityPlayerMP.class)
public abstract class ServerPlayerMixin implements JadeServerPlayer {

	@Unique
	private boolean jade$isConnected;

	@Override
	public boolean jade$isConnected() {
		return jade$isConnected;
	}

	@Override
	public void jade$setConnected(boolean connected) {
		this.jade$isConnected = connected;
	}
}
