package snownee.jade.compat.hwyla;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.ResourceLocation;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.JadeIds;

/**
 * Maps legacy HWYLA config keys to Jade {@link ResourceLocation} uids.
 * Unknown keys return {@code null}, and callers use the registered default.
 */
public final class LegacyKeyMapping {

	private static final Map<String, ResourceLocation> LEGACY_TO_JADE = new HashMap<>();

	static {
		LEGACY_TO_JADE.put("capability.inventoryinfo", JadeIds.UNIVERSAL_ITEM_STORAGE);
		LEGACY_TO_JADE.put("capability.furnace", JadeIds.MC_FURNACE);
		LEGACY_TO_JADE.put("capability.brewing", JadeIds.MC_BREWING_STAND);
		LEGACY_TO_JADE.put("capability.horse", JadeIds.MC_HORSE_STATS);
		LEGACY_TO_JADE.put("capability.chesthorse", JadeIds.MC_HORSE_STATS);
		LEGACY_TO_JADE.put("capability.ageable", JadeIds.MC_MOB_GROWTH);
		LEGACY_TO_JADE.put("capability.breeding", JadeIds.MC_MOB_BREEDING);
		LEGACY_TO_JADE.put("capability.crop", JadeIds.MC_CROP_PROGRESS);
		LEGACY_TO_JADE.put("capability.itemframe", JadeIds.MC_ITEM_FRAME);
		LEGACY_TO_JADE.put("capability.potion", JadeIds.MC_POTION_EFFECTS);
		LEGACY_TO_JADE.put("capability.harvest", JadeIds.MC_HARVEST_TOOL);
	}

	private LegacyKeyMapping() {
	}

	/**
	 * Maps a legacy HWYLA config key to a Jade {@link ResourceLocation}, or {@code null}
	 * when no equivalent exists.
	 */
	@Nullable
	public static ResourceLocation map(String legacyKey) {
		return LEGACY_TO_JADE.get(legacyKey);
	}

	/**
	 * Ensures the Jade config has a default entry for the given key.
	 * Called when HWYLA plugins register config options.
	 * <p>
	 * 1.12.2 shim: the impl singleton was replaced by the public
	 * {@link IWailaClientRegistration} handed to {@code IWailaPlugin.registerClient}.
	 */
	public static void registerDefault(String key, boolean defaultValue, @Nullable IWailaClientRegistration clientReg) {
		ResourceLocation uid = map(key);
		if (uid != null && clientReg != null && !clientReg.hasConfig(uid)) {
			clientReg.addConfig(uid, defaultValue);
		}
	}
}
