package snownee.jade.test;

import java.util.List;
import java.util.Optional;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.Style;
import snownee.jade.Jade;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;

public class ExampleComponentProvider implements IBlockComponentProvider {
	public static final ExampleComponentProvider INSTANCE = new ExampleComponentProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		// 1.12.2: api/ui has no Button element (GuiButton is a screen widget, not a tooltip element),
		// so the modern Button.builder(...) demo is rendered as a plain text element.
		tooltip.add(JadeUI.text(new TextComponentString("Done")));
		// 1.12.2: translated ITooltip has no add(LayoutElement) overload, so the modern
		// LayoutWithPadding-wrapped item is added as a plain item element instead.
		tooltip.add(JadeUI.item(new ItemStack(Items.DIAMOND)));
		Optional<Integer> fuel = ExampleDataProvider.INSTANCE.decodeFromData(accessor);
		if (fuel.isPresent()) {
			Element icon = JadeUI.smallItem(new ItemStack(Items.CLOCK));
			tooltip.add(icon);
			tooltip.append(new TextComponentTranslation("mymod.fuel", fuel.orElse(0)));
		}

		Style test1Style = new Style().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("1")));
		Element test1 = JadeUI.text(new TextComponentString("1").setStyle(test1Style)).flexGrow(1);
		Style test2Style = new Style().setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_ITEM,
				new TextComponentString(new ItemStack(Items.DIAMOND).writeToNBT(new NBTTagCompound()).toString())));
		Element test2 = JadeUI.text(new TextComponentString("2").setStyle(test2Style)).flexGrow(1);
		Style test3Style = new Style().setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "test"));
		Element test3 = JadeUI.text(new TextComponentTranslation("container.dropper").setStyle(test3Style)).flexGrow(2);
		tooltip.add(test1);
		tooltip.append(test2);
		tooltip.append(test3);

		tooltip.add(JadeUI.text(new TextComponentString("1").setStyle(test1Style)).flexGrow(1));
		tooltip.append(JadeUI.text(new TextComponentString("2").setStyle(test2Style)).flexGrow(0));
		tooltip.append(JadeUI.text(new TextComponentTranslation("container.dropper").setStyle(test3Style)).flexGrow(2));

		Element text = JadeUI.text(new TextComponentString("test"));
		tooltip.replace(JadeIds.CORE_OBJECT_NAME, $ -> List.of(List.of(text)));
	}

	@Override
	public ResourceLocation getUid() {
		return ExamplePlugin.UID_TEST_FUEL;
	}

	@Override
	public int getDefaultPriority() {
		return 999999;
	}
}
