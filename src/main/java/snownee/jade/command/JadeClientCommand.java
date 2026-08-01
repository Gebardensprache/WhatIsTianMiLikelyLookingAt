package snownee.jade.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.Jade;
import snownee.jade.JadeClient;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.gui.HomeConfigScreen;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.DumpGenerator;

/**
 * Legacy client-side command registered only with
 * {@code ClientCommandHandler.instance.registerCommand}:
 * <ul>
 *   <li>{@code /jadec handlers} — writes the handler dump to {@code jade_handlers.md}</li>
 *   <li>{@code /jadec config} — invalidates Jade config and item-storage caches, then
 *       opens the in-game config GUI</li>
 *   <li>{@code /jadec use_profile <0..3>} — switches the active profile</li>
 * </ul>
 * <p>
 * 1.12.2: replaces the modern Brigadier tree; the {@code pin} branch is removed.
 */
public class JadeClientCommand extends CommandBase implements ICommand {

	@Override
	public String getName() {
		return Jade.ID + "c";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "commands.jadec.usage";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 0;
	}

	@Override
	public List<String> getAliases() {
		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 0) {
			throw new WrongUsageException(getUsage(sender));
		}
		switch (args[0]) {
			case "handlers" -> {
				File file = new File("jade_handlers.md");
				try (FileWriter writer = new FileWriter(file)) {
					writer.write(DumpGenerator.generateInfoDump());
					CommonProxy.sendMessage(sender, new TextComponentString(
							JadeClient.formatString("command.jade.dump.success")));
				} catch (IOException e) {
					CommonProxy.sendMessage(sender, new TextComponentString(
							e.getClass().getSimpleName() + ": " + e.getMessage()));
				}
			}
			case "config" -> {
				// Invalidate caches, then open the in-game config GUI on the client thread.
				IWailaConfig.get().invalidate();
				ItemStorageProvider.targetCache.invalidateAll();
				ItemStorageProvider.containerCache.invalidateAll();
				Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft()
						.displayGuiScreen(new HomeConfigScreen(Minecraft.getMinecraft().currentScreen)));
			}
			case "use_profile" -> {
				if (args.length < 2) {
					throw new WrongUsageException(getUsage(sender));
				}
				int index = parseInt(args[1], 0, 3);
				// useProfile touches config files; run on the client thread.
				Minecraft.getMinecraft().addScheduledTask(() -> Jade.useProfile(index));
			}
			default -> throw new WrongUsageException(getUsage(sender));
		}
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "handlers", "config", "use_profile");
		}
		if (args.length == 2 && "use_profile".equals(args[0])) {
			return getListOfStringsMatchingLastWord(args, "0", "1", "2", "3");
		}
		return Collections.emptyList();
	}
}
