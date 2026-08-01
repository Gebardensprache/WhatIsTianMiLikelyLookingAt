package snownee.jade.impl.ui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.overlay.DisplayHelper;

/**
 * 1.12.2: the upstream {@code ARMOR}/{@code HALF_ARMOR}/{@code EMPTY_ARMOR}
 * constants reference {@code minecraft:hud/armor_*} — modern Minecraft ships those
 * as separate GUI-sprites under {@code textures/gui/sprites/hud/armor_*.png}, but
 * 1.12.2 has no such files (its atlas {@code textures/gui/icons.png} is a single
 * sheet of fixed UV regions; see {@code GuiIngame.renderPlayerStats}). Those
 * constants would resolve through {@code DisplayHelper.resolveSprite} to a
 * nonexistent texture and render as the missing-texture checkerboard. Mirror
 * {@link HealthElement}: bind {@code icons.png} and draw the vanilla armor regions
 * directly — full {@code (34, 9)}, half {@code (25, 9)}, empty {@code (16, 9)},
 * each 9x9. The render logic itself is unchanged from upstream.
 */
public class ArmorElement extends Element {

	private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
	private static final int SHEET_SIZE = 256;
	private static final int EMPTY_U = 16;
	private static final int HALF_U = 25;
	private static final int FULL_U = 34;
	private static final int V = 9;

	private final float armor;
	private @Nullable String text;
	private int iconsPerLine = 1;
	private int lineCount = 1;
	private int iconCount = 1;

	public ArmorElement(float armor) {
		this.armor = armor;
		IPluginConfig config = IWailaConfig.get().plugin();
		if (armor > config.getInt(JadeIds.MC_ENTITY_ARMOR_MAX_FOR_RENDER)) {
			if (!config.get(JadeIds.MC_ENTITY_HEALTH_SHOW_FRACTIONS)) {
				armor = MathHelper.ceil(armor);
			}
			text = DisplayHelper.dfCommas.format(armor);
		} else {
			armor *= 0.5F;
			int maxHeartsPerLine = config.getInt(JadeIds.MC_ENTITY_HEALTH_ICONS_PER_LINE);
			iconCount = MathHelper.ceil(armor);
			iconsPerLine = Math.min(maxHeartsPerLine, iconCount);
			lineCount = MathHelper.ceil(armor / maxHeartsPerLine);
		}
		if (showText()) {
			width = DisplayHelper.font().width(text) + 10;
			height = 9;
		} else {
			width = 8 * iconsPerLine + 1;
			height = 5 + 4 * lineCount;
		}
	}

	@Override
	public void extractRenderState(int mouseX, int mouseY, float partialTicks) {
		IDisplayHelper helper = IDisplayHelper.get();
		int x = getX();
		int y = getY();
		int xOffset = (iconCount - 1) % iconsPerLine * 8;
		int yOffset = lineCount * 4 - 4;
		for (int i = iconCount; i > 0; --i) {
			helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, EMPTY_U, V, x + xOffset, y + yOffset, 9, 9);

			if (i <= MathHelper.floor(armor)) {
				helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, FULL_U, V, x + xOffset, y + yOffset, 9, 9);
			}

			if ((i > armor) && (i < armor + 1)) {
				helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, HALF_U, V, x + xOffset, y + yOffset, 9, 9);
			}

			xOffset -= 8;
			if (xOffset < 0) {
				xOffset = iconsPerLine * 8 - 8;
				yOffset -= 4;
			}
		}

		if (showText()) {
			helper.drawText(Objects.requireNonNull(text), x + 10, y + 1, IThemeHelper.get().getNormalColor());
		}
	}

	@Override
	public ITextComponent getNarration() {
		return new TextComponentTranslation("narration.jade.armor", MathHelper.ceil(armor));
	}

	public boolean showText() {
		return text != null;
	}
}
