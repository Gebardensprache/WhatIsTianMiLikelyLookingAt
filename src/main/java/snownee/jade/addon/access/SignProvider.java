package snownee.jade.addon.access;

import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;

public class SignProvider implements IBlockComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		if (!(accessor.getBlockEntity() instanceof TileEntitySign be)) {
			return;
		}
		// 1.12.2: signs are single-sided -- there is no front/back distinction (modern
		// "isFacingFrontText" / front-back text arrays do not exist), so only the "front"
		// title is used. The "jade.access.sign.back" language key is unused here.
		tooltip.add(new TextComponentTranslation("jade.access.sign.front"));
		int i = 0;
		for (ITextComponent message : be.signText) {
			++i;
			if (accessor.showDetails()) {
				tooltip.add(new TextComponentTranslation("jade.access.sign.line" + i, message));
			} else {
				tooltip.add(message);
			}
			if (i >= 4) {
				break;
			}
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_SIGN;
	}
}
