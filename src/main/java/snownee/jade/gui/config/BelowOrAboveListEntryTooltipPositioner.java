package snownee.jade.gui.config;

/**
 * 1.12.2: plain-class replacement of the modern {@code ClientTooltipPositioner} machinery. Produces the
 * screen-space position at which a tooltip should be drawn. {@code m}/{@code n} are the tooltip width/height,
 * {@code i}/{@code j} the screen bounds.
 */
public class BelowOrAboveListEntryTooltipPositioner {

	private final OptionsList list;
	private final OptionsList.Entry entry;

	public BelowOrAboveListEntryTooltipPositioner(OptionsList list, OptionsList.Entry entry) {
		this.list = list;
		this.entry = entry;
	}

	public int[] positionTooltip(int i, int j, int mouseX, int mouseY, int m, int n) {
		int x;
		int y;
		int index = list.children().indexOf(entry);
		if (index == -1) {
			x = mouseX + 3;
			y = mouseY + 3;
			return new int[]{x, y};
		}
		x = entry.getContentX() + entry.getTextX();
		y = list.getRowBottom(index) + 1;
		if (y + n > j) {
			y = list.getRowTop(index) - n - 1;
		}
		if (x + m > i) {
			x = Math.max(list.getRowLeft() + list.getRowWidth() - m, 4);
		}
		return new int[]{x, y};
	}
}
