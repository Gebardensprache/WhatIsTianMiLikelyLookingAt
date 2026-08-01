package snownee.jade.api.ui;

import org.jetbrains.annotations.Contract;

/**
 * Base element for text-rendering components.
 */
public abstract class TextElement extends ResizeableElement {
	@Contract("_ -> this")
	public abstract TextElement scale(float scale);

	@Contract("_ -> this")
	public abstract TextElement alpha(float alpha);

	/**
	 * Returns the plain text rendered by this element.
	 * <p>
	 * 1.12.2: upstream inherited this from {@code Message} (via the modern text
	 * component hierarchy); with that hierarchy gone the method lives here as a
	 * default instead.
	 */
	public String getString() {
		return "";
	}
}
