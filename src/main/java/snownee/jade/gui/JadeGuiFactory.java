package snownee.jade.gui;

import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;

/**
 * 1.12.2 Forge convention that plugs Jade's config screen into the mod list's
 * "Config" button (the {@code guiFactory} attribute of {@code @Mod}). Jade keeps
 * its modern in-game config GUI tree; this factory is the single 1.12.2-specific
 * bridge that makes it reachable from the vanilla mod list.
 */
public class JadeGuiFactory implements IModGuiFactory {

	@Override
	public void initialize(Minecraft minecraftInstance) {
	}

	@Override
	public boolean hasConfigGui() {
		return true;
	}

	@Override
	public GuiScreen createConfigGui(GuiScreen parentScreen) {
		return new HomeConfigScreen(parentScreen);
	}

	@Override
	public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
		return java.util.Collections.emptySet();
	}
}
