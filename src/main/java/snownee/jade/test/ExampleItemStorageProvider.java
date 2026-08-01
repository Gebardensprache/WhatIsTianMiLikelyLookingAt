package snownee.jade.test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityBrewingStand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.Accessor;
import snownee.jade.api.ui.MessageType;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

public enum ExampleItemStorageProvider implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
	INSTANCE;

	@Override
	public ResourceLocation getUid() {
		return ExamplePlugin.UID_TEST_BREWING;
	}

	@Override
	public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
		return ClientViewGroup.map(
				groups, ItemView::new, (group, clientGroup) -> {
					clientGroup.title = new TextComponentString(Objects.requireNonNull(group.id));
					clientGroup.messageType = MessageType.WARNING;
				});
	}

	@Override
	public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
		TileEntityBrewingStand target = (TileEntityBrewingStand) Objects.requireNonNull(accessor.getTarget());
		var potions = new ViewGroup<>(IntStream.of(0, 1, 2).mapToObj(target::getStackInSlot).filter($ -> !$.isEmpty()).collect(Collectors.toList()));
		potions.id = "Potions";
		var ingredient = new ViewGroup<>(IntStream.of(3).mapToObj(target::getStackInSlot).filter($ -> !$.isEmpty()).collect(Collectors.toList()));
		ingredient.id = "Ingredient";
		return Arrays.asList(ingredient, potions);
	}
}
