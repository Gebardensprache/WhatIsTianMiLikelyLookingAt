package snownee.jade.api.ui;

import java.util.function.Consumer;

/**
 * A {@link LayoutElement} that arranges children.
 * <p>
 * 1.12.2: stands in for {@code net.minecraft.client.gui.layouts.Layout}. {@code visitWidgets} is kept in the shape
 * vanilla uses so Jade's layout code reads the same, but it hands back {@link LayoutElement} rather than
 * {@code AbstractWidget} because 1.12.2 has no widget base class.
 */
public interface Layout extends LayoutElement {
	void visitChildren(Consumer<LayoutElement> consumer);

	/**
	 * Recomputes child positions. Vanilla calls this {@code arrangeElements}.
	 */
	void arrangeElements();

	void removeChildren();
}
