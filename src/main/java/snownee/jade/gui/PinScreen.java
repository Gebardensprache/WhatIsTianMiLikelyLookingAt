package snownee.jade.gui;

import java.io.IOException;
import java.util.Objects;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.init.SoundEvents;
import snownee.jade.JadeClient;
import snownee.jade.api.ui.CopyBehavior;
import snownee.jade.impl.ui.BoxElementImpl;
import snownee.jade.overlay.OverlayRenderer;

/**
 * 1.12.2: the pinned overlay screen. There is no background to render and no pause. The modern mouse handlers
 * routed to the box element, but 1.12.2's {@code BoxElementImpl} exposes no mouse API (and the pinned overlay is
 * rendered by the overlay renderer, not the screen), so only the copy keybinding remains interactive here.
 */
public class PinScreen extends GuiScreen {

	public PinScreen() {
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		// No background rendering needed
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		BoxElementImpl root = JadeClient.tickHandler().rootElement;
		if (root != null && keyCode == 46 && hasControlDown()) { // KEY_C with Ctrl
			Minecraft mc = Minecraft.getMinecraft();
			ScaledResolution resolution = new ScaledResolution(mc);
			double mouseX = Mouse.getX() * resolution.getScaledWidth() / (double) mc.displayWidth;
			double mouseY = resolution.getScaledHeight() - Mouse.getY() * resolution.getScaledHeight() / (double) mc.displayHeight - 1;
			boolean copied = Boolean.TRUE.equals(OverlayRenderer.animation.mapMousePosition(
					mouseX, mouseY, (x, y) -> {
						if (root.getChildAt(x, y).orElse(root) instanceof CopyBehavior behavior) {
							return behavior.copyToClipboard();
						}
						return false;
					}));
			if (copied) {
				mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
						SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
			}
		}
		super.keyTyped(typedChar, keyCode);
	}

	private static boolean hasControlDown() {
		return Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
	}
}
