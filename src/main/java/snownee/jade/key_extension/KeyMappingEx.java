package snownee.jade.key_extension;

import net.minecraft.client.settings.KeyBinding;

/**
 * Duck interface for {@link KeyBinding}, implemented by {@code KeyMappingMixin}.
 * <p>
 * 1.12.2: the modern {@code KeyMapping} carries a {@code Key} object; the legacy
 * binding uses a plain {@code int} key code, hence {@link #keyEx$key()} returns an int.
 */
public interface KeyMappingEx {
	boolean keyEx$isActive();

	void keyEx$setActive(boolean enabled);

	int keyEx$key();

	static void setActive(KeyBinding keyBinding, boolean active) {
		((KeyMappingEx) keyBinding).keyEx$setActive(active);
	}
}
