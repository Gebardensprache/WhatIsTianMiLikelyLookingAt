package snownee.jade.gui;

import java.util.BitSet;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.Layout;
import snownee.jade.api.ui.LayoutElement;
import snownee.jade.api.ui.Orientation;

/**
 * 1.12.2: no longer extends {@code net.minecraft.client.gui.layouts.AbstractLayout}, which does not exist before
 * 1.20. {@code x}/{@code y}/{@code width}/{@code height} are tracked directly instead of inherited, and
 * {@code ChildContainer} is a plain wrapper (see below) instead of an {@code AbstractLayout.AbstractChildWrapper}.
 * Declares {@code implements Layout} explicitly (vanilla gets this via {@code AbstractLayout}) because callers such
 * as {@code LayoutWithPadding} and {@code JadeUIInternal}'s recursive visitor rely on {@code instanceof Layout}.
 */
public class JadeLinearLayout implements Layout, ResizeableLayout {
	private Orientation orientation;
	private Align alignItems = Align.START;
	private final List<ChildContainer> children = Lists.newArrayList();
	private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();
	private int defaultHeadMargin;
	private int defaultTailMargin;
	private int minWidth;
	private int minHeight;
	private int flexGrow;
	private boolean arranged;
	private int x;
	private int y;
	private int width;
	private int height;

	public JadeLinearLayout(Orientation orientation) {
		this.orientation = orientation;
	}

	public JadeLinearLayout orientation(Orientation orientation) {
		this.orientation = orientation;
		arranged = false;
		return this;
	}

	public JadeLinearLayout alignItems(Align align) {
		this.alignItems = align;
		arranged = false;
		return this;
	}

	public JadeLinearLayout spacing(int i) {
		defaultHeadMargin = defaultTailMargin = i;
		arranged = false;
		return this;
	}

	public JadeLinearLayout setMinDimensions(int minWidth, int minHeight) {
		return setMinWidth(minWidth).setMinHeight(minHeight);
	}

	public JadeLinearLayout setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		arranged = false;
		return this;
	}

	public JadeLinearLayout setMinWidth(int minWidth) {
		this.minWidth = minWidth;
		arranged = false;
		return this;
	}

	public LayoutSettings newChildLayoutSettings() {
		return defaultChildLayoutSettings.copy();
	}

	public LayoutSettings defaultChildLayoutSetting() {
		return defaultChildLayoutSettings;
	}

	public <T extends LayoutElement> T addChild(T element) {
		return addChild(element, newChildLayoutSettings(element));
	}

	public <T extends LayoutElement> T addChild(T element, Consumer<LayoutSettings> consumer) {
		LayoutSettings settings = newChildLayoutSettings(element);
		consumer.accept(settings);
		return addChild(element, settings);
	}

	public <T extends LayoutElement> T addChild(T element, LayoutSettings layoutSettings) {
		return addChild(element, layoutSettings, null);
	}

	public <T extends LayoutElement> T addChild(T element, LayoutSettings layoutSettings, @Nullable Consumer<ChildContainer> consumer) {
		ChildContainer container = new ChildContainer(element, layoutSettings);
		container.headMargin = defaultHeadMargin;
		container.tailMargin = defaultTailMargin;
		if (consumer != null) {
			consumer.accept(container);
		}
		children.add(container);
		arranged = false;
		return element;
	}

	@Override
	public void visitChildren(Consumer<LayoutElement> consumer) {
		this.children.forEach(childContainer -> consumer.accept(childContainer.child));
	}

	@Override
	public void arrangeElements() {
		if (arranged) {
			return;
		}
		int size = children.size();
		if (size == 0) {
			width = height = 0;
			arranged = true;
			return;
		}
		int axis = 0;
		int crossAxis = 0;
		int sumGrow = 0;
		int[] margins = null;
		if (size == 1) {
			ChildContainer child = children.get(0);
			axis = orientation.getAxisLength(child);
			crossAxis = orientation.getCrossAxisLength(child);
			sumGrow = child.flexGrow;
			arranged = true;
		} else {
			margins = new int[size - 1];
			ChildContainer lastChild = null;
			for (int i = 0; i < size; i++) {
				ChildContainer child = children.get(i);
				if (i != 0) {
					int margin = calculateMargin(lastChild.tailMargin, child.headMargin);
					margins[i - 1] = margin;
					axis += margin;
				}

				axis += orientation.getAxisLength(child);
				crossAxis = Math.max(crossAxis, orientation.getCrossAxisLength(child));
				sumGrow += child.flexGrow;

				lastChild = child;
			}
			arranged = true;
		}

		int minAxis = orientation == Orientation.HORIZONTAL ? minWidth : minHeight;
		int extraAxisSpace = Math.max(0, minAxis - axis);
		axis = Math.max(axis, minAxis);
		int minCrossAxis = orientation == Orientation.HORIZONTAL ? minHeight : minWidth;
		crossAxis = Math.max(crossAxis, minCrossAxis);

		resolveFlexGrow(extraAxisSpace, crossAxis, sumGrow);

		int axisPos = orientation.getAxisPosition(this);
		int crossAxisPos = orientation.getCrossAxisPosition(this);
		for (int i = 0; i < size; i++) {
			ChildContainer child = children.get(i);
			int childAxisLength = orientation.getAxisLength(child);

			if (i != 0) {
				axisPos += margins[i - 1];
			}

			Align align = MoreObjects.firstNonNull(child.alignSelf, alignItems);
			align.align(orientation, child, axisPos, crossAxisPos, crossAxis);

			axisPos += childAxisLength;
		}

		width = orientation == Orientation.HORIZONTAL ? axis : crossAxis;
		height = orientation == Orientation.HORIZONTAL ? crossAxis : axis;
	}

	@Override
	public void removeChildren() {
		children.clear();
		arranged = false;
	}

	private void resolveFlexGrow(int extraAxisSpace, int crossAxis, int sumGrow) {
		if (sumGrow == 0 || extraAxisSpace <= 0) {
			return;
		}

		List<ChildContainer> children = Lists.newArrayList();
		for (ChildContainer child : this.children) {
			if (child.flexGrow > 0) {
				children.add(child);
			}
		}
		int size = children.size();
		if (size == 1) {
			ChildContainer child = children.get(0);
			orientation.setFreeSpace(child, orientation.getAxisLength(child) + extraAxisSpace, crossAxis);
			return;
		}

		int reachLimitAmount = 0;
		BitSet reachLimitFlags = new BitSet(size);
		boolean changed = true;
		outer:
		while (changed && reachLimitAmount < size && extraAxisSpace > 0) {
			changed = false;
			int virtualSumGrow = sumGrow;
			for (int i = 0; i < size; i++) {
				ChildContainer child = children.get(i);
				if (reachLimitFlags.get(i)) {
					continue;
				}
				int childAxisLength = orientation.getAxisLength(child);
				int grow = child.flexGrow;
				int extraChildAxis = extraAxisSpace * grow / virtualSumGrow;
				if (extraChildAxis <= 0) {
					continue;
				}
				int newAxisLength = childAxisLength + extraChildAxis;
				orientation.setFreeSpace(child, newAxisLength, crossAxis);
				int childAxisLengthNow = orientation.getAxisLength(child);
				changed |= childAxisLength != childAxisLengthNow;
				if (newAxisLength > childAxisLengthNow) {
					reachLimitFlags.set(i);
					reachLimitAmount++;
					sumGrow -= grow;
					if (sumGrow <= 0) {
						break outer; // no more children to grow
					}
				}
				extraAxisSpace -= extraChildAxis;
				virtualSumGrow -= grow;
				if (virtualSumGrow <= 0) {
					break;
				}
			}
		}
	}

	private static int calculateMargin(int margin1, int margin2) {
		if (margin1 >= 0 && margin2 >= 0) {
			return Math.max(margin1, margin2);
		} else if (margin1 < 0 && margin2 < 0) {
			return Math.min(margin1, margin2);
		} else {
			return margin1 + margin2;
		}
	}

	public static JadeLinearLayout vertical() {
		return new JadeLinearLayout(Orientation.VERTICAL);
	}

	public static JadeLinearLayout horizontal() {
		return new JadeLinearLayout(Orientation.HORIZONTAL);
	}

	@Override
	public void setFreeSpace(int width, int height) {
		if (this.width >= width && this.height >= height) {
			return; // no need to resize
		}
		int oldMinWidth = minWidth;
		int oldMinHeight = minHeight;
		minWidth = Math.max(minWidth, width);
		minHeight = Math.max(minHeight, height);
		arranged = false;
		arrangeElements();
		minWidth = oldMinWidth;
		minHeight = oldMinHeight;
	}

	@Override
	public void setFlexGrow(int flexGrow) {
		Preconditions.checkArgument(flexGrow >= 0, "flexGrow must be non-negative");
		this.flexGrow = flexGrow;
		arranged = false;
	}

	@Override
	public int getFlexGrow() {
		return flexGrow;
	}

	public LayoutSettings newChildLayoutSettings(LayoutElement layoutElement) {
		LayoutSettings settings = newChildLayoutSettings();
		if (layoutElement instanceof Element element && element.getSettings() != null) {
			settings = (LayoutSettings) element.getSettings().apply(settings);
		}
		return settings;
	}

	@Override
	public int getHeight() {
		if (!arranged) {
			arrangeElements();
		}
		return height;
	}

	@Override
	public int getWidth() {
		if (!arranged) {
			arrangeElements();
		}
		return width;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	/**
	 * 1.12.2: reproduces modern {@code AbstractLayout.setX}/{@code setY}, which propagate the position
	 * delta to every child ({@code child.setX(child.getX() + x - this.getX())}). Without this, a parent
	 * layout positions its child layouts at their cumulative offsets, but the child layouts never move
	 * their own children, so all grandchildren stay parked at their local origin and overlap.
	 */
	@Override
	public void setX(int x) {
		int delta = x - this.x;
		if (delta != 0) {
			for (ChildContainer container : children) {
				container.child.setX(container.child.getX() + delta);
			}
		}
		this.x = x;
	}

	@Override
	public void setY(int y) {
		int delta = y - this.y;
		if (delta != 0) {
			for (ChildContainer container : children) {
				container.child.setY(container.child.getY() + delta);
			}
		}
		this.y = y;
	}

	/**
	 * 1.12.2: plain wrapper holding a child plus its {@link LayoutSettings}, replacing
	 * {@code net.minecraft.client.gui.layouts.AbstractLayout.AbstractChildWrapper} (which does not exist before
	 * 1.20). {@code getWidth()}/{@code getHeight()} report the child's size plus padding; {@code setX}/{@code setY}
	 * position the child inside that padded box. Vanilla's fractional x/y-alignment is not reproduced -- see
	 * {@link LayoutSettings}.
	 */
	public static class ChildContainer implements LayoutElement {
		public final LayoutElement child;
		public final LayoutSettings settings;
		public int headMargin;
		public int tailMargin;
		public int flexGrow;
		public @Nullable Align alignSelf;

		protected ChildContainer(LayoutElement child, LayoutSettings settings) {
			this.child = child;
			this.settings = settings;
		}

		@Override
		public int getWidth() {
			return child.getWidth() + settings.getPaddingLeft() + settings.getPaddingRight();
		}

		@Override
		public int getHeight() {
			return child.getHeight() + settings.getPaddingTop() + settings.getPaddingBottom();
		}

		@Override
		public int getX() {
			return child.getX() - settings.getPaddingLeft();
		}

		@Override
		public int getY() {
			return child.getY() - settings.getPaddingTop();
		}

		@Override
		public void setX(int x) {
			setX(x, getWidth());
		}

		@Override
		public void setY(int y) {
			setY(y, getHeight());
		}

		public void setX(int x, int width) {
			child.setX(x + settings.getPaddingLeft());
		}

		public void setY(int y, int height) {
			child.setY(y + settings.getPaddingTop());
		}
	}

	public enum Align {
		START, CENTER, END, STRETCH;

		private void align(Orientation orientation, ChildContainer child, int axisPos, int crossAxisPos, int crossAxisFreeSpace) {
			int axisLength = orientation.getAxisLength(child);
			int crossAxisLength = orientation.getCrossAxisLength(child);
			switch (this) {
				case START -> {
					// do nothing
				}
				case CENTER -> crossAxisPos += (crossAxisFreeSpace - crossAxisLength) / 2;
				case END -> crossAxisPos += crossAxisFreeSpace - crossAxisLength;
				case STRETCH -> orientation.setFreeSpace(child, axisLength, crossAxisFreeSpace); // stretch to fill the cross axis
			}
			orientation.setPosition(child, axisPos, crossAxisPos);
		}
	}
}
