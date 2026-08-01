package snownee.jade.api.ui;

/**
 * Optional clipboard export for a UI element.
 */
public interface CopyBehavior {
	/**
	 * Copies this element to the clipboard.
	 * <p>
	 * 1.12.2: upstream takes a {@code KeyboardHandler} to route the copy through. That class does not exist here, and
	 * 1.12.2's clipboard access is the static {@code GuiScreen.setClipboardString}, so the parameter is dropped.
	 *
	 * @return {@code true} if something was copied
	 */
	boolean copyToClipboard();
}
