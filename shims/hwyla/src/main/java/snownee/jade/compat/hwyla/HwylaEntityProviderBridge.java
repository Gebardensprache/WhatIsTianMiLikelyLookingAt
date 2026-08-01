package snownee.jade.compat.hwyla;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Bridges one tooltip role of an HWYLA entity provider to Jade. */
public class HwylaEntityProviderBridge implements IEntityComponentProvider {

	public enum Role {
		HEAD, BODY, TAIL
	}

	private final mcp.mobius.waila.api.IWailaEntityProvider hwylaProvider;
	private final HwylaConfigHandler config;
	private final Role role;
	private final ResourceLocation uid;

	public HwylaEntityProviderBridge(mcp.mobius.waila.api.IWailaEntityProvider hwylaProvider,
			HwylaConfigHandler config, Role role, ResourceLocation uid) {
		this.hwylaProvider = hwylaProvider;
		this.config = config;
		this.role = role;
		this.uid = uid;
	}

	public mcp.mobius.waila.api.IWailaEntityProvider getHwylaProvider() {
		return hwylaProvider;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig pluginConfig) {
		mcp.mobius.waila.api.IWailaEntityAccessor hwylaAccessor = new HwylaEntityAccessor(accessor);
		Entity entity = accessor.getEntity();
		switch (role) {
			case HEAD:
				appendLines(tooltip, hwylaProvider.getWailaHead(entity, new java.util.ArrayList<>(), hwylaAccessor, config));
				break;
			case BODY:
				appendLines(tooltip, hwylaProvider.getWailaBody(entity, new java.util.ArrayList<>(), hwylaAccessor, config));
				break;
			case TAIL:
				appendLines(tooltip, hwylaProvider.getWailaTail(entity, new java.util.ArrayList<>(), hwylaAccessor, config));
				break;
		}
	}

	private static void appendLines(ITooltip tooltip, java.util.List<String> lines) {
		if (lines == null) {
			return;
		}
		for (String line : lines) {
			tooltip.add((net.minecraft.util.text.ITextComponent) new TextComponentString(line));
		}
	}

	@Override
	public ResourceLocation getUid() {
		return uid;
	}

	@Override
	public boolean isRequired() {
		return true;
	}
}
