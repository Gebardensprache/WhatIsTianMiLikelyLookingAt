package snownee.jade.addon.vanilla;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.entity.passive.EntityLlama;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.overlay.DisplayHelper;

public class HorseStatsProvider implements IEntityComponentProvider {
	public static final HorseStatsProvider INSTANCE = new HorseStatsProvider();

	/**
	 * 1.12.2: {@code AbstractHorse.MAX_JUMP_STRENGTH} / {@code MAX_MOVEMENT_SPEED} do not exist. The
	 * ceilings are the maxima of {@code AbstractHorse.getModifiedJumpStrength()} and
	 * {@code getModifiedMovementSpeed()}: {@code 0.4 + 3 * 0.2 = 1.0} and
	 * {@code (0.45 + 3 * 0.3) * 0.25 = 0.3375}.
	 */
	private static final double MAX_JUMP_STRENGTH = 1.0;
	private static final double MAX_MOVEMENT_SPEED_ATTRIBUTE = 0.3375;

	/**
	 * 1.12.2: the jump-strength attribute is {@code AbstractHorse.JUMP_STRENGTH}, a protected
	 * {@code RangedAttribute} rather than a member of {@code Attributes}. It is looked up by its
	 * registered name so no access transformer is needed.
	 */
	private static final String JUMP_STRENGTH_NAME = "horse.jumpStrength";

	private static final double MAX_JUMP_HEIGHT = getJumpHeight(MAX_JUMP_STRENGTH);
	private static final double MAX_MOVEMENT_SPEED = getSpeed(MAX_MOVEMENT_SPEED_ATTRIBUTE);

	private static ITextComponent switchText(String key, boolean showMax, double value, double max) {
		IThemeHelper t = IThemeHelper.get();
		ITextComponent valueText = t.info(DisplayHelper.dfCommas.format(value));
		if (showMax) {
			return new TextComponentTranslation(key, new TextComponentTranslation("jade.fraction", valueText, DisplayHelper.dfCommas.format(max)));
		} else {
			return new TextComponentTranslation(key, valueText);
		}
	}

	private static double getJumpHeight(double jumpStrength) {
		return 4.53680079 * jumpStrength * jumpStrength + 1.61431730 * jumpStrength - 0.22656224;
	}

	private static double getSpeed(double speed) {
		// https://minecraft.wiki/w/Horse#Movement_speed
		// https://github.com/sakura-ryoko/minihud/pull/179
		return speed * 43.171815466666658 - 0.000000339999999;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		AbstractHorse horse = (AbstractHorse) accessor.getEntity();
		boolean showMax = accessor.showDetails();
		if (horse instanceof EntityLlama) {
			tooltip.add(switchText("jade.llamaStrength", showMax, ((EntityLlama) horse).getStrength(), 5));
			return;
		}
		// 1.12.2: no Camel, so the early return for it is dropped.
		IAttributeInstance jumpStrength = horse.getAttributeMap().getAttributeInstanceByName(JUMP_STRENGTH_NAME);
		if (jumpStrength != null) {
			// 1.12.2: use the effective attribute value so equipment and potion modifiers remain visible.
			double jumpHeight = getJumpHeight(jumpStrength.getAttributeValue());
			tooltip.add(switchText("jade.horseStat.jump", showMax, jumpHeight, MAX_JUMP_HEIGHT));
		}
		IAttributeInstance movementSpeed = horse.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED);
		if (movementSpeed != null) {
			// 1.12.2: use the effective attribute value so equipment and potion modifiers remain visible.
			double speed = getSpeed(movementSpeed.getAttributeValue());
			tooltip.add(switchText("jade.horseStat.speed", showMax, speed, MAX_MOVEMENT_SPEED));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_HORSE_STATS;
	}
}
