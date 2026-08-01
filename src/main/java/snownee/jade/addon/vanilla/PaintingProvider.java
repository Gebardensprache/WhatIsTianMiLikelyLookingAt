package snownee.jade.addon.vanilla;

import net.minecraft.entity.item.EntityPainting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class PaintingProvider implements IEntityComponentProvider {
	public static final PaintingProvider INSTANCE = new PaintingProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		EntityPainting painting = (EntityPainting) accessor.getEntity();
		if (painting.art != null) {
			// 1.12.2: paintings are a hardcoded EnumArt, not a registry variant. EnumArt only
			// carries a title, so there is no author line and no registry lookup to perform.
			String title = painting.art.title;
			tooltip.add(IThemeHelper.get().warning(new TextComponentString(title)));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_PAINTING;
	}
}
