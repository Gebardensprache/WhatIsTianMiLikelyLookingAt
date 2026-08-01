package snownee.jade.gui.config;

/**
 * 1.12.2: plain-class replacement of the modern {@code ClientTooltipPositioner} machinery. Always positions
 * the tooltip at a fixed screen position.
 */
public class FixedTooltipPositioner {

	private final int x;
	private final int y;

	public FixedTooltipPositioner(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int[] positionTooltip(int i, int j, int mouseX, int mouseY, int m, int n) {
		return new int[]{x, y};
	}
}
