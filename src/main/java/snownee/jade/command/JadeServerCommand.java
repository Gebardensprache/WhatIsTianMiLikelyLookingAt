package snownee.jade.command;

import java.util.Collections;
import java.util.List;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.Jade;
import snownee.jade.network.ShowOverlayPacket;
import snownee.jade.util.CommonProxy;

/**
 * Legacy server-side command: {@code /jade show <targets>} and {@code /jade hide <targets>}.
 * <p>
 * 1.12.2: replaces the modern Brigadier tree. Packets are only sent to players whose Jade
 * client completed the handshake ({@link CommonProxy#isConnected}).
 */
public class JadeServerCommand extends CommandBase {

	@Override
	public String getName() {
		return Jade.ID;
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "commands.jade.usage";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public List<String> getAliases() {
		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 2) {
			throw new WrongUsageException(getUsage(sender));
		}
		boolean show = "show".equals(args[0]);
		if (!show && !"hide".equals(args[0])) {
			throw new WrongUsageException(getUsage(sender));
		}
		List<EntityPlayerMP> players = getPlayers(server, sender, args[1]);
		ShowOverlayPacket msg = new ShowOverlayPacket(show);
		int count = 0;
		for (EntityPlayerMP player : players) {
			if (CommonProxy.isConnected(player)) {
				CommonProxy.sendPacket(player, msg);
				count++;
			}
		}
		CommonProxy.sendMessage(sender, new TextComponentString(
				count + (show ? " shown" : " hidden")));
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "show", "hide");
		}
		if (args.length == 2) {
			return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
		}
		return Collections.emptyList();
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return index == 1;
	}
}
