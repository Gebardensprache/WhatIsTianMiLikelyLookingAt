package snownee.jade.mixin.key_extension;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import snownee.jade.key_extension.KeyMappingEx;

/**
 * Filters inactive profile bindings out of the key-bindings list, before the
 * constructor's clone/sort and category-count allocation, so no null trailing entries
 * appear and inactive bindings are neither listed nor counted.
 * <p>
 * 1.12.2: targets {@link net.minecraft.client.gui.GuiKeyBindingList}, whose constructor
 * reads {@code mcIn.gameSettings.keyBindings} (clone + {@code Arrays.sort} + per-binding
 * category/entry loop) and allocates {@code listEntries} from that array length plus
 * {@code KeyBinding.getKeybinds().size()}.
 */
@Mixin(GuiKeyBindingList.class)
public class KeyBindsListMixin {
	@WrapOperation(
			method = "<init>",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;",
					opcode = Opcodes.GETFIELD))
	private KeyBinding[] keyEx$filterDisabled(GameSettings gameSettings, Operation<KeyBinding[]> original) {
		return Arrays.stream(original.call(gameSettings))
				.filter($ -> ((KeyMappingEx) $).keyEx$isActive())
				.toArray(KeyBinding[]::new);
	}

	/**
	 * The category-count call ({@code KeyBinding.getKeybinds().size()}) must reflect the
	 * filtered bindings so the entry allocation has no trailing nulls. Return the set of
	 * categories actually present in the filtered binding array (the constructor emits
	 * exactly one {@code CategoryEntry} per distinct category of that array).
	 */
	@WrapOperation(
			method = "<init>",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/client/settings/KeyBinding;getKeybinds()Ljava/util/Set;"))
	private Set<String> keyEx$categoryCount(Operation<Set<String>> original) {
		Set<String> filtered = new HashSet<>();
		for (KeyBinding keyBinding : Minecraft.getMinecraft().gameSettings.keyBindings) {
			if (((KeyMappingEx) keyBinding).keyEx$isActive()) {
				filtered.add(keyBinding.getKeyCategory());
			}
		}
		return filtered;
	}
}
