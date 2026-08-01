package snownee.jade.key_extension;

import java.util.List;
import java.util.Map;

import net.minecraft.client.settings.KeyBinding;

/**
 * Tracks whether the profile-key active set changed and defers the legacy key-hash
 * rebuild ({@link KeyBinding#resetKeyBindingArrayAndHash()}) to the next dispatch
 * surface that reads the hash ({@link KeyBinding#onTick}/{@link KeyBinding#setKeyBindState}).
 * <p>
 * 1.12.2: the modern {@code KeyMapping.resetMapping()} has no counterpart; the static
 * {@code KEYBIND_ARRAY}/{@code HASH} state of {@link KeyBinding} is rebuilt instead.
 */
public final class KeyExManager {
	private static boolean dirty;

	public static void markDirty() {
		dirty = true;
	}

	/**
	 * Rebuilds the dispatch hash from {@code KEYBIND_ARRAY}, including only active
	 * bindings, and clears the dirty flag. Wrapped from inside
	 * {@code KeyBinding.resetKeyBindingArrayAndHash()} by {@code KeyMappingMixin}.
	 */
	public static void resetMapping(Map<String, KeyBinding> all) {
		dirty = false;
	}

	/**
	 * Called from {@code KeyBinding.onTick} / {@code setKeyBindState} before they look
	 * up bindings in the hash, so a profile activation/deactivation is effective
	 * immediately.
	 */
	public static void checkDirty() {
		if (dirty) {
			KeyBinding.resetKeyBindingArrayAndHash();
		}
	}
}
