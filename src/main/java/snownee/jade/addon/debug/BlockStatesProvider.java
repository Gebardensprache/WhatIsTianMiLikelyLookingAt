package snownee.jade.addon.debug;

import java.util.Collection;

import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.JadeUI;

public class BlockStatesProvider implements IBlockComponentProvider {
	public static final BlockStatesProvider INSTANCE = new BlockStatesProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		IBlockState state = accessor.getBlockState();
		Collection<IProperty<?>> properties = state.getPropertyKeys();
		if (properties.isEmpty()) {
			return;
		}
		IThemeHelper t = IThemeHelper.get();
		ITooltip box = JadeUI.tooltip();
		for (IProperty<?> p : properties) {
			Comparable<?> value = state.getValue(p);
			// 1.12.2: PropertyBool replaces modern BooleanProperty.
			ITextComponent valueText = new TextComponentString(" " + value);
			if (p instanceof PropertyBool) {
				valueText = Boolean.TRUE.equals(value) ? t.success(valueText) : t.danger(valueText);
			}
			box.add(new TextComponentString(p.getName() + ":").appendSibling(valueText));
		}
		tooltip.add(JadeUI.box(box, BoxStyle.nestedBox()).flexGrow(1));
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.DEBUG_BLOCK_STATES;
	}

	@Override
	public int getDefaultPriority() {
		return -4500;
	}

	@Override
	public boolean enabledByDefault() {
		return false;
	}
}
