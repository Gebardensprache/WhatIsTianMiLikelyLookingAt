package snownee.jade.api.config;

import net.minecraft.util.ResourceLocation;

/**
 * Tracks explicit hide/pick overrides for registry-backed targets.
 *
 * @param <T> registry value type
 * @param <U> resolved runtime object type
 */
public interface TargetOperationRepository<T, U> {
	/**
	 * Reloads repository state from registries.
	 */
	void reload();

	/**
	 * Returns whether the given registry entry should be hidden.
	 *
	 * @param key registry key
	 * @return {@code true} if the target should be hidden
	 */
	boolean shouldHide(ResourceLocation key);

	/**
	 * Returns whether the given object should be hidden.
	 *
	 * @param obj target object
	 * @return {@code true} if the target should be hidden
	 */
	default boolean shouldHide(U obj) {
		return shouldHide(map(obj));
	}

	/**
	 * Returns whether the given registry entry should be selected.
	 *
	 * @param key registry key
	 * @return {@code true} if the target should be picked
	 */
	boolean shouldPick(ResourceLocation key);

	/**
	 * Returns whether the given object should be selected.
	 *
	 * @param obj target object
	 * @return {@code true} if the target should be picked
	 */
	default boolean shouldPick(U obj) {
		return shouldPick(map(obj));
	}

	/**
	 * Marks a registry entry as hidden.
	 *
	 * @param key registry key
	 */
	void hide(ResourceLocation key);

	/**
	 * Marks a registry entry as preferred.
	 *
	 * @param key registry key
	 */
	void pick(ResourceLocation key);

	/**
	 * Maps a runtime object back to its registry key.
	 *
	 * @param obj runtime object
	 * @return the corresponding registry key
	 */
	ResourceLocation map(U obj);
}
