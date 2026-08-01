package snownee.jade.gui;

import snownee.jade.api.ui.LayoutElement;

public interface ResizeableLayout extends LayoutElement {
	void setFreeSpace(int width, int height);

	void setFlexGrow(int flexGrow);

	int getFlexGrow();
}
