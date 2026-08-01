package snownee.jade.test;

import java.util.Arrays;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import snownee.jade.api.Accessor;
import snownee.jade.api.ui.MessageType;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ProgressView;
import snownee.jade.api.view.ViewGroup;

public enum ExampleProgressProvider implements IServerExtensionProvider<ProgressView.Data>,
		IClientExtensionProvider<ProgressView.Data, ProgressView> {
	INSTANCE;

	@Override
	public ResourceLocation getUid() {
		return ExamplePlugin.UID_TEST_PROGRESS;
	}

	@Override
	public List<ClientViewGroup<ProgressView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ProgressView.Data>> groups) {
		return ClientViewGroup.map(
				groups, ProgressView::read, (group, clientGroup) -> {
					var view = clientGroup.views.get(0);
//			view.style.color(0xFFCC0000);
					view.text = new TextComponentString("Testtttttttttttttttttttttttttttttttt");

					view = clientGroup.views.get(1);
//			view.style.color(0xFF00CC00);
					view.text = new TextComponentString("Test");
				});
	}

	@Override
	public List<ViewGroup<ProgressView.Data>> getGroups(Accessor<?> accessor) {
		World world = accessor.getLevel();
		float period = 40;
		var progress1 = new ProgressView.Data(((world.getWorldTime() % period) + 1) / period);

		var progress2 = new ProgressView.Data(((world.getWorldTime() % period) + 1) / period, 1 / period, 1, MessageType.DANGER);
		var group = new ViewGroup<>(List.of(progress1, progress2));
		return Arrays.asList(group);
	}
}
