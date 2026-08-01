package snownee.jade.addon.core;

import java.text.DecimalFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.JadeClient;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IToggleableProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.TextElement;
import snownee.jade.impl.theme.ThemeHelper;
import snownee.jade.util.NarrationHelper;

public abstract class DistanceProvider implements IToggleableProvider {

	public static class ForBlock extends DistanceProvider implements IBlockComponentProvider {
		public static final ForBlock INSTANCE = new ForBlock();

		@Override
		public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
			append(tooltip, accessor, accessor.getPosition(), config);
		}
	}

	public static class ForEntity extends DistanceProvider implements IEntityComponentProvider {
		public static final ForEntity INSTANCE = new ForEntity();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			// 1.12.2: Entity#blockPosition() is Entity#getPosition()
			append(tooltip, accessor, accessor.getEntity().getPosition(), config);
		}
	}

	public static final DecimalFormat fmt = new DecimalFormat("#.#");
	private static final int[] colors = {0xef9a9a, 0xa5d6a7, 0x90caf9, 0xb02a37, 0x198754, 0x0a58ca};

	public static String distance(Accessor<?> accessor) {
		// 1.12.2: no DeltaTracker; Minecraft#getRenderPartialTicks() is the render partial tick
		float partialTick = Minecraft.getMinecraft().getRenderPartialTicks();
		return fmt.format(accessor.getPlayer().getPositionEyes(partialTick).distanceTo(accessor.getHitResult().hitVec));
	}

	public static TextElement xyz(Vec3i pos) {
		ITextComponent display = new TextComponentTranslation(
				"jade.blockpos",
				display(pos.getX(), 0),
				display(pos.getY(), 1),
				display(pos.getZ(), 2));
		String narration = JadeClient.formatString(
				"narration.jade.blockpos",
				NarrationHelper.number(pos.getX()),
				NarrationHelper.number(pos.getY()),
				NarrationHelper.number(pos.getZ()));
		TextElement text = JadeUI.text(display);
		text.narration(narration);
		return text;
	}

	public static ITextComponent display(int i, int colorIndex) {
		if (IThemeHelper.get().isLightColorScheme()) {
			colorIndex += 3;
		}
		return new TextComponentString(Integer.toString(i)).setStyle(ThemeHelper.colorStyle(colors[colorIndex]));
	}

	public void append(ITooltip tooltip, Accessor<?> accessor, BlockPos pos, IPluginConfig config) {
		boolean distance = config.get(JadeIds.CORE_DISTANCE);
		String distanceVal = distance ? distance(accessor) : null;
		String distanceMsg = distance ? JadeClient.formatString("narration.jade.distance", distanceVal) : null;
		if (config.get(JadeIds.CORE_COORDINATES)) {
			if (config.get(JadeIds.CORE_REL_COORDINATES) && JadeUI.hasControlDown()) {
				// 1.12.2: no BlockPos.containing(Vec3); BlockPos has a Vec3d constructor that floors
				tooltip.add(xyz(pos.subtract(new BlockPos(accessor.getPlayer().getPositionEyes(1.0F)))));
			} else {
				tooltip.add(xyz(pos));
			}
			if (distance) {
				tooltip.append(JadeUI
						.text(new TextComponentTranslation("jade.distance1", distanceVal))
						.narration(distanceMsg));
			}
		} else if (distance) {
			tooltip.add(JadeUI
					.text(new TextComponentTranslation("jade.distance2", distanceVal))
					.narration(distanceMsg));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.CORE_DISTANCE;
	}

	@Override
	public boolean isRequired() {
		return true;
	}

	@Override
	public int getDefaultPriority() {
		return -4600;
	}

}
