package snownee.jade.compat.top;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mcjty.theoneprobe.api.IElementFactory;
import mcjty.theoneprobe.api.IProbeInfoEntityProvider;
import mcjty.theoneprobe.api.IProbeInfoProvider;

/**
 * Stores registered TOP providers and element factories in registration order.
 */
public class TopProviderStore {

	private final LinkedHashMap<String, IProbeInfoProvider> blockProviders = new LinkedHashMap<>();
	private final LinkedHashMap<String, IProbeInfoEntityProvider> entityProviders = new LinkedHashMap<>();
	private final Map<Integer, IElementFactory> elementFactories = new LinkedHashMap<>();
	private int nextElementFactoryId = 1;

	public void registerProvider(IProbeInfoProvider provider) {
		blockProviders.put(provider.getID(), provider);
	}

	public void registerEntityProvider(IProbeInfoEntityProvider provider) {
		entityProviders.put(provider.getID(), provider);
	}

	public List<IProbeInfoProvider> getBlockProviders() {
		return new ArrayList<>(blockProviders.values());
	}

	public List<IProbeInfoEntityProvider> getEntityProviders() {
		return new ArrayList<>(entityProviders.values());
	}

	public int registerElementFactory(IElementFactory factory) {
		int id = nextElementFactoryId++;
		elementFactories.put(id, factory);
		return id;
	}

	public IElementFactory getElementFactory(int id) {
		return elementFactories.get(id);
	}
}
