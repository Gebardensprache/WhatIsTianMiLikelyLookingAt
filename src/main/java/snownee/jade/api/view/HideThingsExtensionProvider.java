package snownee.jade.api.view;

import java.util.Collections;
import java.util.List;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.Accessor;
import snownee.jade.api.JadeIds;

/**
 * Empty extension provider used when a target should intentionally hide all storage detail.
 */
public class HideThingsExtensionProvider<IN, OUT> implements IServerExtensionProvider<IN>, IClientExtensionProvider<IN, OUT> {
	private static final HideThingsExtensionProvider<?, ?> INSTANCE = new HideThingsExtensionProvider<>();

	@Override
	public List<ViewGroup<IN>> getGroups(Accessor<?> accessor) {
		return Collections.emptyList();
	}

	@Override
	public List<ClientViewGroup<OUT>> getClientGroups(Accessor<?> accessor, List<ViewGroup<IN>> viewGroups) {
		return Collections.emptyList();
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.UNIVERSAL_HIDE_THINGS;
	}

	/**
	 * Returns the singleton instance.
	 *
	 * @param <IN> server-side data type
	 * @param <OUT> client-side data type
	 * @return shared instance
	 */
	public static <IN, OUT> HideThingsExtensionProvider<IN, OUT> instance() {
		//noinspection unchecked
		return (HideThingsExtensionProvider<IN, OUT>) INSTANCE;
	}
}
