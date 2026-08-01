package snownee.jade.mixin.key_extension;

import java.util.Collection;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.settings.KeyBinding;
import snownee.jade.key_extension.KeyExManager;
import snownee.jade.key_extension.KeyMappingEx;

/**
 * Implements the {@link KeyMappingEx} duck interface on
 * {@link net.minecraft.client.settings.KeyBinding} and keeps the legacy static dispatch
 * hash in sync with the active state.
 * <p>
 * 1.12.2: the modern {@code KeyMapping.click/set/setAll/resetMapping} surfaces do not
 * exist; the dispatch surfaces are {@code KeyBinding.onTick}, {@code setKeyBindState}
 * and {@code resetKeyBindingArrayAndHash}.
 */
@Mixin(value = KeyBinding.class, priority = 900)
public abstract class KeyMappingMixin implements KeyMappingEx {

	@Shadow
	@Final
	private static Map<String, KeyBinding> KEYBIND_ARRAY;
	@Shadow
	private int keyCode;

	@Unique
	private boolean active = true;

	@Override
	public boolean keyEx$isActive() {
		return active;
	}

	@Override
	public void keyEx$setActive(boolean active) {
		boolean changed = this.active != active;
		this.active = active;
		// An unbound binding (keyCode 0) is never dispatched anyway; only an actually
		// bound binding's state change requires the hash rebuild.
		if (changed && keyCode != 0) {
			KeyExManager.markDirty();
		}
	}

	@Override
	public int keyEx$key() {
		return keyCode;
	}

	/**
	 * Before any dispatch lookup, rebuild the hash if profile activation changed it.
	 */
	@Inject(method = {"onTick", "setKeyBindState"}, at = @At("HEAD"), order = 800)
	private static void keyEx$checkDirty(CallbackInfo ci) {
		KeyExManager.checkDirty();
	}

	/**
	 * Clears the dirty flag for the duration of the rebuild so the wrap below cannot
	 * trigger a re-entrant rebuild.
	 */
	@Inject(method = "resetKeyBindingArrayAndHash", at = @At("HEAD"))
	private static void keyEx$resetMapping(CallbackInfo ci) {
		KeyExManager.resetMapping(KEYBIND_ARRAY);
	}

	/**
	 * Only active bindings may enter the dispatch hash.
	 */
	@WrapOperation(method = "resetKeyBindingArrayAndHash", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
	private static Collection<KeyBinding> keyEx$resetMapping(Map<String, KeyBinding> map, Operation<Collection<KeyBinding>> original) {
		return original.call(map).stream()
				.filter($ -> ((KeyMappingEx) $).keyEx$isActive())
				.toList();
	}
}
