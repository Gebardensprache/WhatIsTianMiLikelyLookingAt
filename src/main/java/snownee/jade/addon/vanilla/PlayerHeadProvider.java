package snownee.jade.addon.vanilla;

import org.apache.commons.lang3.StringUtils;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.resources.I18n;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.util.ClientProxy;

public class PlayerHeadProvider implements IBlockComponentProvider {
	public static final PlayerHeadProvider INSTANCE = new PlayerHeadProvider();

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		if (accessor.getBlockEntity() instanceof TileEntitySkull tile) {
			// 1.12.2: TileEntitySkull has no custom name, so the upstream customName branch is dropped.
			// 1.12.2: no ResolvableProfile, the owner is a plain GameProfile.
			GameProfile profile = tile.getPlayerProfile();
			if (profile == null) {
				return;
			}
			String name = profile.getName();
			if (name == null) {
				name = ClientProxy.lookupPlayerName(profile.getId());
			}
			if (StringUtils.isBlank(name)) {
				return;
			}
			if (!name.contains(" ") && !name.contains("§")) {
				// 1.12.2: there is no Items.PLAYER_HEAD and no ".named" suffix. The skull item is
				// Items.SKULL with subtypes, and the player subtype key already carries the
				// possessive format ("item.skull.player.name=%s's Head"), matching upstream's ".named".
				name = I18n.format("item.skull.player.name", name);
			}
			tooltip.replace(JadeIds.CORE_OBJECT_NAME, IThemeHelper.get().title(name));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_PLAYER_HEAD;
	}

}
