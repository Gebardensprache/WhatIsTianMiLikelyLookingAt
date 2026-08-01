package snownee.jade.gui;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/**
 * 1.12.2: the modern {@code Tooltip} type does not exist, so the static factory is replaced by a plain
 * {@code ITextComponent} composer that joins the lines with linebreak components. Screens that need the tooltip
 * rendered call {@code BaseOptionsScreen.setTooltipForNextFrame}/{@code drawHoveringText} instead.
 */
public class MultilineTooltip {

	public static ITextComponent create(List<ITextComponent> components) {
		return create(components, components);
	}

	public static ITextComponent create(List<ITextComponent> components, @Nullable List<ITextComponent> narration) {
		return compose(components);
	}

	private static ITextComponent compose(List<ITextComponent> components) {
		if (components.isEmpty()) {
			return new TextComponentString("");
		}
		if (components.size() == 1) {
			return components.get(0);
		}
		ITextComponent linebreak = new TextComponentString("\n");
		ITextComponent result = components.get(0).createCopy();
		for (int i = 1; i < components.size(); i++) {
			result.appendSibling(linebreak).appendSibling(components.get(i));
		}
		return result;
	}
}
