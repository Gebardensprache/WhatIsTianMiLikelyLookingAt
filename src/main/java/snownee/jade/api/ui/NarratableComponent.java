package snownee.jade.api.ui;

import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

/**
 * ITextComponent wrapper that provides a dedicated narration string.
 * <p>
 * 1.12.2: {@code ITextComponent} is a larger interface here than modern's {@code Component} (it declares
 * {@code setStyle}, {@code appendText}, {@code appendSibling}, {@code createCopy}, {@code getFormattedText},
 * {@code getUnformattedText}, and {@code iterator()} via {@code Iterable}), so every mutating/copying method is
 * delegated straight to the wrapped component; only narration lookup is special-cased.
 */
public class NarratableComponent implements ITextComponent {
	private final ITextComponent component;
	private final @Nullable Supplier<String> narration;

	public NarratableComponent(ITextComponent component) {
		this(component, null);
	}

	public NarratableComponent(ITextComponent component, @Nullable Supplier<String> narration) {
		this.component = component;
		this.narration = narration;
	}

	@Override
	public ITextComponent setStyle(Style style) {
		component.setStyle(style);
		return this;
	}

	@Override
	public Style getStyle() {
		return component.getStyle();
	}

	@Override
	public ITextComponent appendText(String text) {
		component.appendText(text);
		return this;
	}

	@Override
	public ITextComponent appendSibling(ITextComponent sibling) {
		component.appendSibling(sibling);
		return this;
	}

	@Override
	public String getUnformattedComponentText() {
		return component.getUnformattedComponentText();
	}

	@Override
	public String getUnformattedText() {
		return component.getUnformattedText();
	}

	@Override
	public String getFormattedText() {
		return component.getFormattedText();
	}

	@Override
	public List<ITextComponent> getSiblings() {
		return component.getSiblings();
	}

	@Override
	public ITextComponent createCopy() {
		return new NarratableComponent(component.createCopy(), narration);
	}

	@Override
	public Iterator<ITextComponent> iterator() {
		return component.iterator();
	}

	@Override
	public String toString() {
		return component.getUnformattedComponentText();
	}

	public String getNarration() {
		return narration != null ? narration.get() : component.getUnformattedComponentText();
	}

	public static ITextComponent getNarration(ITextComponent text) {
		if (text instanceof NarratableComponent narratable) {
			return new TextComponentString(narratable.getNarration());
		}
		return text;
	}

	public static ITextComponent attach(ITextComponent component, ITextComponent narratable) {
		if (narratable instanceof NarratableComponent narratableComponent) {
			return new NarratableComponent(component, narratableComponent.narration);
		}
		return new NarratableComponent(component, narratable::getUnformattedComponentText);
	}

	public static ITextComponent translatable(String key, Object... objects) {
		boolean hasNarratable = false;
		for (Object object : objects) {
			if (object instanceof NarratableComponent) {
				hasNarratable = true;
				break;
			}
		}
		ITextComponent component = new TextComponentTranslation(key, objects);
		if (!hasNarratable) {
			return component;
		}
		Object[] narrationArgs = new Object[objects.length];
		for (int i = 0; i < objects.length; i++) {
			if (objects[i] instanceof NarratableComponent narratable) {
				narrationArgs[i] = narratable.getNarration();
			} else {
				narrationArgs[i] = objects[i];
			}
		}
		return new NarratableComponent(component, () -> new TextComponentTranslation(key, narrationArgs).getUnformattedComponentText());
	}
}
