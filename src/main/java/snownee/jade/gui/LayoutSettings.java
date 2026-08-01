package snownee.jade.gui;

/**
 * Per-child padding settings for {@link JadeLinearLayout}.
 * <p>
 * 1.12.2: stands in for {@code net.minecraft.client.gui.layouts.LayoutSettings}, which does not exist before 1.20.
 * Only the padding fields Jade actually uses are kept; vanilla's fractional x/y-alignment is dropped because every
 * call site in this codebase always passes a child's own width/height as the available space (see
 * {@code JadeLinearLayout.ChildContainer.setX(int, int)}), which makes that offset always resolve to zero.
 */
public class LayoutSettings {
	private int paddingLeft;
	private int paddingTop;
	private int paddingRight;
	private int paddingBottom;

	public static LayoutSettings defaults() {
		return new LayoutSettings();
	}

	public LayoutSettings copy() {
		LayoutSettings copy = new LayoutSettings();
		copy.paddingLeft = paddingLeft;
		copy.paddingTop = paddingTop;
		copy.paddingRight = paddingRight;
		copy.paddingBottom = paddingBottom;
		return copy;
	}

	public LayoutSettings paddingLeft(int padding) {
		paddingLeft = padding;
		return this;
	}

	public LayoutSettings paddingTop(int padding) {
		paddingTop = padding;
		return this;
	}

	public LayoutSettings paddingRight(int padding) {
		paddingRight = padding;
		return this;
	}

	public LayoutSettings paddingBottom(int padding) {
		paddingBottom = padding;
		return this;
	}

	public LayoutSettings paddingHorizontal(int padding) {
		return paddingLeft(padding).paddingRight(padding);
	}

	public LayoutSettings paddingVertical(int padding) {
		return paddingTop(padding).paddingBottom(padding);
	}

	public LayoutSettings padding(int padding) {
		return paddingHorizontal(padding).paddingVertical(padding);
	}

	public int getPaddingLeft() {
		return paddingLeft;
	}

	public int getPaddingTop() {
		return paddingTop;
	}

	public int getPaddingRight() {
		return paddingRight;
	}

	public int getPaddingBottom() {
		return paddingBottom;
	}
}
