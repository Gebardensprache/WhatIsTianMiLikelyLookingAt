package snownee.jade.mixin.key_extension;

import java.util.Arrays;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import snownee.jade.key_extension.KeyMappingEx;

/**
 * Filters inactive bindings out of the conflict-scan loop inside the nested
 * {@code GuiKeyBindingList.KeyEntry.drawEntry}:
 * <pre>{@code
 * for (KeyBinding keybinding : GuiKeyBindingList.this.mc.gameSettings.keyBindings) {
 *     if (keybinding != this.keybinding && keybinding.conflicts(this.keybinding)) { ... }
 * }
 * }</pre>
 * A mixin on the outer {@code GuiKeyBindingList} class cannot touch this nested-class
 * method, so this mixin targets {@code GuiKeyBindingList$KeyEntry} directly. The wrap
 * replaces the array the for-each iterates with the active bindings only, so an
 * inactive profile slot never lights up as a false conflict on its duplicate-key
 * neighbour. (Wrapping the {@code conflicts} call itself would not work: for a plain
 * {@code INVOKEVIRTUAL} the handler receives only the method's arguments -- the entry's
 * own binding -- not the loop variable.)
 */
@Mixin(targets = "net.minecraft.client.gui.GuiKeyBindingList$KeyEntry")
public class KeyBindsListEntryMixin {
	@WrapOperation(
			method = "drawEntry",
			at = @At(
					value = "FIELD",
					target = "Lnet/minecraft/client/settings/GameSettings;keyBindings:[Lnet/minecraft/client/settings/KeyBinding;",
					opcode = Opcodes.GETFIELD))
	private KeyBinding[] keyEx$filterDisabled(GameSettings gameSettings, Operation<KeyBinding[]> original) {
		return Arrays.stream(original.call(gameSettings))
				.filter($ -> ((KeyMappingEx) $).keyEx$isActive())
				.toArray(KeyBinding[]::new);
	}
}
