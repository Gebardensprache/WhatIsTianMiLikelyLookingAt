package snownee.jade.api.callback;

import snownee.jade.api.Accessor;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.TooltipAnimation;

/**
 * Runs before Jade renders the tooltip overlay.
 */
@FunctionalInterface
public interface JadeBeforeRenderCallback {

	/**
	 * Called before rendering starts.
	 *
	 * @param root     root tooltip element
	 * @param animation tooltip animation state
	 * @param accessor current accessor
	 * @return {@code true} to continue rendering, {@code false} to cancel
	 */
	boolean beforeRender(BoxElement root, TooltipAnimation animation, Accessor<?> accessor);

}
