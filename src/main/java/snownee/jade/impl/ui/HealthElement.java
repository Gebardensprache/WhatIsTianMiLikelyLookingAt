package snownee.jade.impl.ui;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.JadeClient;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.config.IWailaConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.IDisplayHelper;
import snownee.jade.overlay.DisplayHelper;
import snownee.jade.track.HealthTrackInfo;

/**
 * 1.12.2: the upstream {@code Hud.HeartType} enum (and its
 * {@code getSprite(hardcore, half, blinking)} lookup) does not exist here -- 1.12.2's vanilla
 * heart icons are plain fixed regions of {@code textures/gui/icons.png} (see
 * {@code net.minecraftforge.client.GuiIngameForge#renderHealth}), with no per-status-effect
 * "frozen" heart variant at all (that was added well after 1.12.2). The upstream
 * {@code heartType} constructor parameter is therefore dropped entirely: this element only
 * ever distinguishes plain hearts from golden absorption hearts, driven purely by the
 * {@code absorption} amount already passed in, exactly mirroring how upstream's own
 * {@code Hud.HeartType.ABSORBING} override was selected. This is a documented feature drop --
 * the out-of-scope {@code addon.vanilla.EntityHealthAndArmorProvider} call site that passed a
 * frozen/normal heart type will no longer compile (left alone, that's B5's problem).
 * <p>
 * There is also no distinct "blinking" heart texture in 1.12.2 (vanilla instead flashes the
 * empty-heart background). We reuse the plain full/half heart texture for the "last known
 * health" overlay drawn during {@link HealthTrackInfo#isBlinking()}, and flash the empty-heart
 * background the same way vanilla does, which keeps the overall blink behaviour intact.
 */
public class HealthElement extends Element {

	private static final ResourceLocation ICONS = new ResourceLocation("textures/gui/icons.png");
	private static final int SHEET_SIZE = 256;
	private static final int EMPTY_U = 16;
	private static final int EMPTY_BLINK_U = 25;
	private static final int FULL_U = 52;
	private static final int HALF_U = 61;
	private static final int ABSORB_FULL_U = 160;
	private static final int ABSORB_HALF_U = 169;

	private final float maxHealth;
	private final float health;
	private final float absorption;
	private @Nullable String text;
	private int iconsPerLine = 1;
	private int lineCount = 1;
	private int iconCount = 1;
	private @Nullable HealthTrackInfo track;

	public HealthElement(float maxHealth, float health, float absorption) {
		this.maxHealth = maxHealth;
		this.health = health;
		this.absorption = absorption;
		IPluginConfig config = IWailaConfig.get().plugin();
		int iconCount = MathHelper.ceil(maxHealth) + MathHelper.ceil(absorption);
		if (iconCount > config.getInt(JadeIds.MC_ENTITY_HEALTH_MAX_FOR_RENDER)) {
			health += absorption;
			if (!config.get(JadeIds.MC_ENTITY_HEALTH_SHOW_FRACTIONS)) {
				maxHealth = MathHelper.ceil(maxHealth);
				health = MathHelper.ceil(health);
			}
			text = String.format("%s/%s", DisplayHelper.dfCommas.format(health), DisplayHelper.dfCommas.format(maxHealth));
		} else {
			int maxHeartsPerLine = config.getInt(JadeIds.MC_ENTITY_HEALTH_ICONS_PER_LINE);
			iconCount = MathHelper.ceil(iconCount * 0.5F);
			this.iconCount = iconCount;
			iconsPerLine = Math.min(maxHeartsPerLine, iconCount);
			lineCount = MathHelper.ceil((float) iconCount / maxHeartsPerLine);
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
		float health = this.health * 0.5F;
		float lastHealth = health;
		float lastAbsorption = absorption;
		boolean blink = false;
		if (track == null && getTag() != null) {
			track = JadeClient.tickHandler().progressTracker.getOrCreate(
					getTag(),
					HealthTrackInfo.class,
					() -> new HealthTrackInfo(this.health, absorption));
		}
		if (track != null) {
			track.setHealth(this.health, absorption);
			track.update(Minecraft.getMinecraft().timer.renderPartialTicks);
			lastHealth = track.getLastHealth() * 0.5F;
			lastAbsorption = track.getLastAbsorption();
			blink = track.isBlinking();
		}

		IDisplayHelper helper = IDisplayHelper.get();
		int xOffset = (iconCount - 1) % iconsPerLine * 8;
		int yOffset = lineCount * 4 - 4;
		int backgroundU = blink ? EMPTY_BLINK_U : EMPTY_U;
		for (int i = iconCount; i > 0; --i) {
			int xPos = getX() + xOffset;
			int yPos = getY() + yOffset;
			helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, backgroundU, 0, xPos, yPos, 9, 9);

			boolean renderAbsorb = i > MathHelper.ceil(maxHealth * 0.5F);
			float curHealth = health;
			float curLastHealth = lastHealth;
			int fullU = FULL_U;
			int halfU = HALF_U;
			if (renderAbsorb) {
				curHealth = (MathHelper.ceil(maxHealth) + absorption) * 0.5F;
				curLastHealth = (MathHelper.ceil(maxHealth) + lastAbsorption) * 0.5F;
				fullU = ABSORB_FULL_U;
				halfU = ABSORB_HALF_U;
			}
			if (i <= MathHelper.floor(curHealth)) { // Full heart
				helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, fullU, 0, xPos, yPos, 9, 9);
			}

			if (i > curHealth) {
				if (i <= MathHelper.floor(curLastHealth)) { // Full heart (last known health, blink)
					helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, fullU, 0, xPos, yPos, 9, 9);
				} else if ((i > curLastHealth) && (i < curLastHealth + 1)) { // Half heart (blink)
					helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, halfU, 0, xPos, yPos, 9, 9);
				}
				if (i < curHealth + 1) { // Half heart
					helper.blitSprite(ICONS, SHEET_SIZE, SHEET_SIZE, halfU, 0, xPos, yPos, 9, 9);
				}
			}

			xOffset -= 8;
			if (xOffset < 0) {
				xOffset = iconsPerLine * 8 - 8;
				yOffset -= 4;
			}
		}

		if (showText()) {
			helper.drawText(Objects.requireNonNull(text), getX() + 10, getY() + 1, IThemeHelper.get().getNormalColor());
		}
	}

	@Override
	public ITextComponent getNarration() {
		return new TextComponentTranslation("narration.jade.health", MathHelper.ceil(health));
	}

	public boolean showText() {
		return text != null;
	}
}
