package snownee.jade.gui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.Jade;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.Rect2f;
import snownee.jade.gui.config.OptionsList;
import snownee.jade.overlay.OverlayRenderer;

public abstract class PreviewOptionsScreen extends BaseOptionsScreen {

	private boolean adjustingPosition;
	private boolean adjustDragging;
	private double dragOffsetX;
	private double dragOffsetY;

	public PreviewOptionsScreen(@Nullable GuiScreen parent, ITextComponent title) {
		super(parent, title);
	}

	public static boolean isAdjustingPosition() {
		return Minecraft.getMinecraft().currentScreen instanceof PreviewOptionsScreen screen && screen.adjustingPosition;
	}

	private static float calculateAnchor(float center, float size, float rectSize) {
		float anchor = center / size;
		if (anchor < 0.25F) {
			return 0;
		}
		if (anchor > 0.75F) {
			return 1;
		}
		float halfRectSize = rectSize / 2F;
		float tolerance = Math.min(15, halfRectSize / 2F - 3);
		if (Math.abs(center + halfRectSize - size / 2F) < tolerance) {
			return 1;
		}
		if (Math.abs(center - halfRectSize - size / 2F) < tolerance) {
			return 0;
		}
		return 0.5F;
	}

	private static float maybeSnap(float value, boolean snap) {
		if (snap && value > 0.475f && value < 0.525f) {
			return 0.5f;
		}
		return value;
	}

	@Override
	public void initGui() {
		super.initGui();
		if (mc.world != null) {
			// preview overlay toggle; 1.12.2 CycleButton -> plain toggling GuiButton
			OptionsList.OptionButtonLike preview = new OptionsList.OptionButtonLike(
					Jade.history().previewOverlay ? OptionsList.OPTION_ON : OptionsList.OPTION_OFF,
					new TextComponentTranslation("gui.jade.preview"),
					10, Objects.requireNonNull(saveButton).y, 85, 20);
			preview.setOnPress(w -> {
				Jade.history().previewOverlay = !Jade.history().previewOverlay;
				w.setMessage(Jade.history().previewOverlay ? OptionsList.OPTION_ON : OptionsList.OPTION_OFF);
				Objects.requireNonNull(saver).run();
			});
			buttonList.add(preview);
		}
	}

	public boolean forcePreviewOverlay() {
		Objects.requireNonNull(mc);
		if (adjustingPosition) {
			return true;
		}
		if (!options.isDragging() || options == null) {
			return false;
		}
		OptionsList.Entry entry = options.getSelected();
		if (entry == null || entry.mainWidget() == null) {
			return false;
		}
		return options.forcePreview.contains(entry);
	}

	@Override
	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws java.io.IOException {
		if (!adjustingPosition) {
			super.mouseClicked(mouseX, mouseY, mouseButton);
			return;
		}
		Objects.requireNonNull(mc);
		Rect2f rect = OverlayRenderer.animation.expectedRect;
		if (rect.contains(mouseX, mouseY)) {
			adjustDragging = true;
			float centerX = rect.getX() + rect.getWidth() / 2F;
			float centerY = rect.getY() + rect.getHeight() / 2F;
			dragOffsetX = mouseX - centerX;
			dragOffsetY = mouseY - centerY;
		} else {
			mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
					SoundEvents.UI_BUTTON_CLICK, 1.0f));
			int xIndex = Math.max(0, Math.min((int) (mouseX / (width / 3D)), 2));
			int yIndex = Math.max(0, Math.min((int) (mouseY / (height / 3D)), 2));
			if (xIndex == 1 && yIndex == 1) {
				adjustingPosition = false;
				adjustDragging = false;
			} else {
				IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
				overlay.setOverlayPosX(IWailaConfig.get().accessibility().tryFlip(xIndex / 2F));
				overlay.setOverlayPosY(1 - yIndex / 2F);
				overlay.setAnchorX(IWailaConfig.get().accessibility().tryFlip(xIndex / 2F));
				overlay.setAnchorY(yIndex / 2F);
			}
		}
	}

	@Override
	public void mouseReleased(int mouseX, int mouseY, int state) {
		if (adjustingPosition) {
			adjustDragging = false;
			return;
		}
		super.mouseReleased(mouseX, mouseY, state);
	}

	@Override
	public void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		if (adjustingPosition && adjustDragging) {
			float centerX = (float) mouseX - (float) dragOffsetX;
			float centerY = (float) mouseY - (float) dragOffsetY;
			moveOverlay(centerX, centerY, !JadeUI.hasControlDown());
			return;
		}
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
	}

	@Override
	public void handleMouseInput() throws java.io.IOException {
		if (adjustingPosition) {
			// consume wheel while adjusting; super would dispatch mouse events for clicks
			return;
		}
		super.handleMouseInput();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
		if (adjustingPosition) {
			switch (keyCode) {
				case 200: // up
					moveOverlayRelatively(0, -1);
					break;
				case 208: // down
					moveOverlayRelatively(0, 1);
					break;
				case 203: // left
					moveOverlayRelatively(-1, 0);
					break;
				case 205: // right
					moveOverlayRelatively(1, 0);
					break;
				case 1: // ESC
					adjustingPosition = false;
					adjustDragging = false;
					mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(
							SoundEvents.UI_BUTTON_CLICK, 1.0f));
					break;
				default:
					break;
			}
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}

	private void moveOverlayRelatively(float x, float y) {
		Rect2f rect = OverlayRenderer.animation.expectedRect;
		float centerX = rect.getX() + rect.getWidth() / 2F;
		float centerY = rect.getY() + rect.getHeight() / 2F;
		float step = JadeUI.hasShiftDown() ? 20 : 2;
		centerX += x * step;
		centerY += y * step;
		moveOverlay(centerX, centerY, false);
	}

	public void moveOverlay(float centerX, float centerY, boolean snap) {
		Rect2f rect = OverlayRenderer.animation.expectedRect;
		float rectWidth = rect.getWidth();
		float rectHeight = rect.getHeight();
		float anchorX = calculateAnchor(centerX, width, rectWidth);
		float anchorY = calculateAnchor(centerY, height, rectHeight);
		float posX = (centerX + rectWidth * (anchorX - 0.5F)) / width;
		float posY = 1 - (centerY + rectHeight * (anchorY - 0.5F)) / height;
		IWailaConfig.Overlay overlay = IWailaConfig.get().overlay();
		IWailaConfig.Accessibility accessibility = IWailaConfig.get().accessibility();
		overlay.setOverlayPosX(accessibility.tryFlip(maybeSnap(posX, snap)));
		overlay.setOverlayPosY(maybeSnap(posY, snap));
		overlay.setAnchorX(accessibility.tryFlip(anchorX));
		overlay.setAnchorY(anchorY);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (adjustingPosition) {
			super.drawScreen(Integer.MAX_VALUE, Integer.MAX_VALUE, partialTicks);
			Gui.drawRect(0, 0, width, height, 0x80808080);

			ITextComponent text = new TextComponentTranslation("config.jade.overlay_pos.exit");
			int textWidth = mc.fontRenderer.getStringWidth(text.getFormattedText());
			int x = (width - textWidth) / 2;
			int y = height / 2 - 7;
			Gui.drawRect(x - 4, y - 4, x + textWidth + 4, y + mc.fontRenderer.FONT_HEIGHT + 4, 0x88000000);
			mc.fontRenderer.drawString(text.getFormattedText(), x, y, 0xFFFFFFFF);

			IWailaConfig.Overlay config = IWailaConfig.get().overlay();
			Rect2f rect = OverlayRenderer.animation.expectedRect;
			if (IWailaConfig.get().general().isDebug()) {
				int anchorX = (int) (rect.getX() + rect.getWidth() * config.getAnchorX());
				int anchorY = (int) (rect.getY() + rect.getHeight() * config.getAnchorY());
				Gui.drawRect(anchorX - 2, anchorY - 2, anchorX + 1, anchorY + 1, 0xFFFF0000);
			}
			if (config.getOverlayPosX() == 0.5f) {
				Gui.drawRect(width / 2, (int) (rect.getY() - 5), width / 2 + 1, (int) (rect.getY() + rect.getHeight() + 4), 0xFF0000FF);
			}
			if (config.getOverlayPosY() == 0.5f) {
				Gui.drawRect(
						(int) (rect.getX() - 5),
						height / 2,
						(int) (rect.getX() + rect.getWidth() + 4),
						height / 2 + 1,
						0xFF0000FF);
			}
		} else {
			super.drawScreen(mouseX, mouseY, partialTicks);
		}
	}

	public void startAdjustingPosition() {
		adjustingPosition = true;
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
