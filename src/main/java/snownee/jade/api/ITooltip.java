package snownee.jade.api;

import java.util.List;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jspecify.annotations.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.ScreenDirection;

/**
 * Mutable tooltip container used by Jade to assemble text and layout elements.
 * <p>
 * Addons can append text, icons, and custom layout elements, then tag and replace sections later in the pipeline.
 */
@NonExtendable
public interface ITooltip {

	/**
	 * Removes every element from this tooltip.
	 */
	void clear();

	/**
	 * Returns the number of lines currently in the tooltip.
	 *
	 * @return the tooltip line count
	 */
	int size();

	default boolean isEmpty() {
		return size() == 0;
	}

	/**
	 * Appends a text component on a new line.
	 *
	 * @param component the text to add
	 */
	default void add(ITextComponent component) {
		add(component, null);
	}

	/**
	 * Appends a tagged text component on a new line.
	 *
	 * @param component the text to add
	 * @param tag optional identifier used for later replacement or removal
	 */
	default void add(ITextComponent component, @Nullable ResourceLocation tag) {
		add(size(), component, tag);
	}

	default void add(int index, ITextComponent component) {
		add(index, component, null);
	}

	default void add(int index, ITextComponent component, @Nullable ResourceLocation tag) {
		add(index, JadeUI.text(component).tag(tag));
	}

	default void addAll(List<ITextComponent> components) {
		components.forEach(this::add);
	}

	/**
	 * Appends a renderable element on a new line.
	 *
	 * @param element the element to add
	 */
	default void add(Element element) {
		add(size(), element);
	}

	default void add(int index, List<? extends Element> elements) {
		boolean first = true;
		for (Element element : elements) {
			if (first) {
				add(index, element);
			} else {
				append(index, element);
			}
			first = false;
		}
	}

	default void add(List<? extends Element> elements) {
		add(size(), elements);
	}

	void add(int index, Element element);

	/**
	 * Appends a text component to the last line.
	 * <p>
	 * Use this only to combine separate tooltip elements on the same line, not to build a single long string.
	 *
	 * @param component the text to append
	 */
	default void append(ITextComponent component) {
		append(component, null);
	}

	/**
	 * Appends a tagged text component to the last line.
	 *
	 * @param component the text to append
	 * @param tag optional identifier used for later replacement or removal
	 */
	default void append(ITextComponent component, @Nullable ResourceLocation tag) {
		append(JadeUI.text(component).tag(tag));
	}

	/**
	 * Appends a renderable element to the last line.
	 *
	 * @param element the element to append
	 */
	default void append(Element element) {
		append(size() - 1, element);
	}

	/**
	 * Append render-able elements to the last line
	 */
	default void append(int index, List<? extends Element> elements) {
		for (Element element : elements) {
			append(index, element);
		}
	}

	void append(int index, Element element);

	/**
	 * Removes every element tagged with the given identifier.
	 *
	 * @param tag the tag to remove
	 * @return {@code true} if at least one element was removed
	 *
	 */
	boolean remove(ResourceLocation tag);

	/**
	 * Replaces every element tagged with the given identifier at the position of the first matching element.
	 *
	 * @param tag the tag to replace
	 * @param elements replacement lines
	 * @return {@code true} if at least one element was replaced
	 *
	 */
	boolean replace(ResourceLocation tag, UnaryOperator<List<List<Element>>> elements);

	/**
	 * Replaces every element tagged with the given identifier using a single text component.
	 *
	 * @param tag the tag to replace
	 * @param component replacement text
	 * @return {@code true} if at least one element was replaced
	 */
	boolean replace(ResourceLocation tag, ITextComponent component);

	/**
	 * Returns all elements tagged with the given identifier.
	 *
	 * @param tag the tag to query
	 * @return the tagged elements, in tooltip order
	 */
	List<Element> get(ResourceLocation tag);

	/**
	 * Sets the margin for one side of a line.
	 *
	 * @param index the line index
	 * @param side the side to adjust
	 * @param margin the margin in pixels
	 */
	void setLineMargin(int index, ScreenDirection side, int margin);

	/**
	 * Applies additional layout settings to a line.
	 *
	 * @param index the line index
	 * @param settings settings transformer
	 */
	void setLineSettings(int index, UnaryOperator<Object> settings);

	/**
	 * Returns the narration text for this tooltip.
	 *
	 * @return the narration string
	 */
	String getNarration();

	/**
	 * Returns the rendered string for the elements tagged with the given identifier.
	 *
	 * @param tag the tag to query
	 * @return the rendered text for the tagged elements
	 */
	String getString(ResourceLocation tag);

	/**
	 * Returns the current icon element, if any.
	 *
	 * @return the tooltip icon or {@code null}
	 */
	@Nullable Element getIcon();
}
