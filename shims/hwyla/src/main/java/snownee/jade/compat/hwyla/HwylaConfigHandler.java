package snownee.jade.compat.hwyla;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.shim.hwyla.HwylaShim;

/** Implements HWYLA's read-only config handler on top of Jade config. */
public class HwylaConfigHandler implements mcp.mobius.waila.api.IWailaConfigHandler {

	public static final HwylaConfigHandler INSTANCE = new HwylaConfigHandler();

	private final Map<String, Boolean> registeredDefaults = new HashMap<>();
	private final Map<String, Map<String, String>> moduleKeys = new LinkedHashMap<>();
	private final Set<String> warnedKeys = new java.util.HashSet<>();

	/** Set from {@link HwylaCompat#registerClient}; null on a dedicated server. */
	private IWailaClientRegistration clientReg;

	private HwylaConfigHandler() {
	}

	/**
	 * 1.12.2 shim: the impl singleton was replaced by the public
	 * {@link IWailaClientRegistration} handed to {@code IWailaPlugin.registerClient}.
	 */
	public void setClientRegistration(IWailaClientRegistration clientReg) {
		this.clientReg = clientReg;
	}

	/** Records a default for callers that only have a legacy key. */
	public void setDefault(String key, boolean defaultValue) {
		setDefault(key, key, key, defaultValue);
	}

	/** Records a default and preserves HWYLA's display module/key metadata. */
	public void setDefault(String modName, String key, String displayName, boolean defaultValue) {
		registeredDefaults.putIfAbsent(key, defaultValue);
		moduleKeys.computeIfAbsent(modName, ignored -> new LinkedHashMap<>()).putIfAbsent(key, displayName);
	}

	@Override
	public Set<String> getModuleNames() {
		return java.util.Collections.unmodifiableSet(moduleKeys.keySet());
	}

	@Override
	public HashMap<String, String> getConfigKeys(String modName) {
		return new HashMap<>(moduleKeys.getOrDefault(modName, java.util.Collections.emptyMap()));
	}

	@Override
	public boolean getConfig(String key, boolean defvalue) {
		ResourceLocation uid = LegacyKeyMapping.map(key);
		if (uid != null && clientReg != null && clientReg.hasConfig(uid)) {
			return IWailaConfig.get().plugin().get(uid);
		}
		return getRegisteredDefault(key, defvalue);
	}

	@Override
	public boolean getConfig(String key) {
		return getConfig(key, true);
	}

	private boolean getRegisteredDefault(String key, boolean fallback) {
		if (registeredDefaults.containsKey(key)) {
			return registeredDefaults.get(key);
		}
		if (warnedKeys.add(key)) {
			HwylaShim.LOGGER.debug("HWYLA config key '{}' has no Jade equivalent; using fallback {}", key, fallback);
		}
		return fallback;
	}
}
