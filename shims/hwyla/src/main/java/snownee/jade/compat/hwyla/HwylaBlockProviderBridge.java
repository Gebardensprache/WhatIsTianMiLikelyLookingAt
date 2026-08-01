package snownee.jade.compat.hwyla;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** Bridges one role of an HWYLA block data provider to Jade. */
public class HwylaBlockProviderBridge implements IBlockComponentProvider {

	public enum Role {
		HEAD, BODY, TAIL, STACK
	}

	private final mcp.mobius.waila.api.IWailaDataProvider hwylaProvider;
	private final HwylaConfigHandler config;
	private final Role role;
	private final ResourceLocation uid;

	public HwylaBlockProviderBridge(mcp.mobius.waila.api.IWailaDataProvider hwylaProvider,
			HwylaConfigHandler config, Role role, ResourceLocation uid) {
		this.hwylaProvider = hwylaProvider;
		this.config = config;
		this.role = role;
		this.uid = uid;
	}

	public mcp.mobius.waila.api.IWailaDataProvider getHwylaProvider() {
		return hwylaProvider;
	}

	public Role getRole() {
		return role;
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig pluginConfig) {
		mcp.mobius.waila.api.IWailaDataAccessor hwylaAccessor = new HwylaDataAccessor(accessor);
		ItemStack stack = accessor.getPickedResult();
		switch (role) {
			case STACK:
				ItemStack overrideStack = hwylaProvider.getWailaStack(hwylaAccessor, config);
				if (overrideStack != null && !overrideStack.isEmpty()) {
					storeStackOverride(accessor, overrideStack);
				}
				break;
			case HEAD:
				appendLines(tooltip, hwylaProvider.getWailaHead(stack, new java.util.ArrayList<>(), hwylaAccessor, config));
				break;
			case BODY:
				appendLines(tooltip, hwylaProvider.getWailaBody(stack, new java.util.ArrayList<>(), hwylaAccessor, config));
				break;
			case TAIL:
				appendLines(tooltip, hwylaProvider.getWailaTail(stack, new java.util.ArrayList<>(), hwylaAccessor, config));
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

	@SuppressWarnings("deprecation")
	private void storeStackOverride(BlockAccessor accessor, ItemStack overrideStack) {
		if (accessor instanceof snownee.jade.api.AccessorImpl) {
			((snownee.jade.api.AccessorImpl<?>) accessor).setServersideRep(overrideStack);
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
