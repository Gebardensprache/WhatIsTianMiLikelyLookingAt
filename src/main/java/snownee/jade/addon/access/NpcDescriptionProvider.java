package snownee.jade.addon.access;

import com.google.common.base.Strings;

import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

public class NpcDescriptionProvider implements IEntityComponentProvider {
	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
		// 1.12.2: Mannequin does not exist (added in a much later Minecraft version), so the
		// description source is substituted with the vanilla villager's display name (which is
		// career-based, e.g. "Librarian"). The description is never null for villagers, so the
		// DEFAULT_DESCRIPTION fallback of the modern code has no counterpart here.
		EntityVillager entity = (EntityVillager) accessor.getEntity();
		ITextComponent description = entity.getDisplayName();
		String message = tooltip.getString(JadeIds.CORE_OBJECT_NAME);
		if (!Strings.isNullOrEmpty(message)) {
			tooltip.replace(
					JadeIds.CORE_OBJECT_NAME,
					IThemeHelper.get().title(new TextComponentTranslation("jade.access.npc_description", message, description)));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.ACCESS_NPC_DESCRIPTION;
	}
}
