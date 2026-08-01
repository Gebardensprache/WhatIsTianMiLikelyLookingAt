package snownee.jade.api.callback;

import snownee.jade.api.Accessor;
import snownee.jade.api.ui.BoxElement;
import snownee.jade.api.ui.TooltipAnimation;

/**
 * Runs after Jade renders the tooltip overlay.
 */
@FunctionalInterface
public interface JadeAfterRenderCallback {

	/**
	 * Called after rendering finishes.
	 *
	 * @param root     root tooltip element
	 * @param animation tooltip animation state
	 * @param accessor current accessor
	 */
	void afterRender(BoxElement root, TooltipAnimation animation, Accessor<?> accessor);

}
