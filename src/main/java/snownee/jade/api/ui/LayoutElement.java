package snownee.jade.api.ui;

/**
 * Positionable, measurable UI element.
 * <p>
 * 1.12.2: stands in for {@code net.minecraft.client.gui.layouts.LayoutElement}, which does not exist before 1.20.
 * Only the geometry contract Jade actually relies on is kept; the vanilla {@code visitWidgets} narration plumbing
 * is not reproduced because 1.12.2 has no {@code AbstractWidget} hierarchy to visit.
 */
public interface LayoutElement {
	int getX();

	int getY();

	void setX(int x);

	void setY(int y);

	int getWidth();

	int getHeight();

	default void setPosition(int x, int y) {
		setX(x);
		setY(y);
	}

	default int getRight() {
		return getX() + getWidth();
	}

	default int getBottom() {
		return getY() + getHeight();
	}

	default Rect2f getRectangle() {
		return Rect2f.of(this);
	}
}
