package snownee.jade.api.ui;

import java.util.Arrays;
import java.util.List;

/**
 * Cardinal screen directions used for tooltip layout.
 */
public enum ScreenDirection {
	UP, DOWN, LEFT, RIGHT;

	public static final List<ScreenDirection> VALUES = Arrays.asList(values());

	public static ScreenDirection fromIndex(int index) {
		return VALUES.get(index);
	}

	public boolean isHorizontal() {
		return this == LEFT || this == RIGHT;
	}

	public boolean isVertical() {
		return this == UP || this == DOWN;
	}
}
