package snownee.jade.util;

import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.gson.reflect.TypeToken;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import snownee.jade.Jade;

public class ForgeJadeMetadata extends JadeMetadata {

	public ForgeJadeMetadata() {
		ImmutableList.Builder<String> disableModNameBuilder = ImmutableList.builder();
		for (ModContainer container : Loader.instance().getActiveModList()) {
			String modId = container.getModId();
			try {
				// 1.12.2: ModMetadata has no modproperties field (1.13+ only). Custom mod properties are exposed
				// as a flat Map<String, String> via @Mod(customProperties = @Mod.CustomProperty(k, v)); the jade
				// value is expected to be a JSON object string, parsed back into a map here.
				Object raw = JsonConfig.GSON.fromJson((String) container.getCustomModProperties().get(Jade.ID), new TypeToken<Map<String, Object>>() {}.getType());
				if (raw == null) {
					continue;
				}
				Map<String, Object> obj;
				if (raw instanceof Map) {
					//noinspection unchecked
					obj = (Map<String, Object>) raw;
				} else {
					continue;
				}
				if (obj.isEmpty()) {
					continue;
				}
				accessibilityMod |= getBool(obj, "accessibilityMod");
				fastScroll |= getBool(obj, "fastScroll");
				smoothScroll |= getBool(obj, "smoothScroll");
				if (getBool(obj, "disableItemModNameTooltip")) {
					disableModNameBuilder.add(modId);
				}
			} catch (Exception e) {
				Jade.LOGGER.warn("Failed to read jade metadata for mod %s".formatted(modId), e);
			}
		}
		disableItemModNameTooltip = disableModNameBuilder.build();
	}

	private static boolean getBool(Map<String, Object> obj, String key) {
		if (!obj.containsKey(key)) {
			return false;
		}
		Object value = obj.get(key);
		if (value.getClass() != Boolean.class) {
			return false;
		}
		return (Boolean) value;
	}

}
