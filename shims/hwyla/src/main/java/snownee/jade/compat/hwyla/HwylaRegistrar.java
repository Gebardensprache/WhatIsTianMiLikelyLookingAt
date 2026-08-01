package snownee.jade.compat.hwyla;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import mcp.mobius.waila.api.IWailaBlockDecorator;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaEntityProvider;
import mcp.mobius.waila.api.IWailaFMPDecorator;
import mcp.mobius.waila.api.IWailaFMPProvider;
import mcp.mobius.waila.api.IWailaTooltipRenderer;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.JadeIds;

/** Bridges HWYLA registrations into Jade's registration APIs. */
public class HwylaRegistrar implements mcp.mobius.waila.api.IWailaRegistrar {

	private static final Logger LOGGER = LogManager.getLogger(HwylaRegistrar.class);

	private final HwylaConfigHandler configHandler;
	private final IWailaClientRegistration clientReg;
	private final IWailaCommonRegistration commonReg;
	private final Set<String> warnedUnsupported = new HashSet<>();
	private final Map<String, Integer> uidOrdinals = new HashMap<>();
	private final Set<ResourceLocation> allocatedUids = new HashSet<>();

	// 1.12.2 shim: the impl singletons (WailaClientRegistration.instance() etc.) were replaced
	// by the PUBLIC registration interfaces handed to IWailaPlugin.register/registerClient.
	public HwylaRegistrar(HwylaConfigHandler configHandler, IWailaClientRegistration clientReg, IWailaCommonRegistration commonReg) {
		this.configHandler = configHandler;
		this.clientReg = clientReg;
		this.commonReg = commonReg;
	}

	// ---- Config methods ----

	@Override
	public void addConfig(String modname, String keyname, String configtext) {
		addConfig(modname, keyname, configtext, true);
	}

	@Override
	public void addConfig(String modname, String keyname, String configtext, boolean defvalue) {
		LegacyKeyMapping.registerDefault(keyname, defvalue, clientReg);
		configHandler.setDefault(modname, keyname, configtext, defvalue);
	}

	@Override
	public void addConfigRemote(String modname, String keyname, String configtext) {
		addConfigRemote(modname, keyname, configtext, true);
	}

	@Override
	public void addConfigRemote(String modname, String keyname, String configtext, boolean defvalue) {
		addConfig(modname, keyname, configtext, defvalue);
	}

	@Override
	public void addConfig(String modname, String keyname) {
		addConfig(modname, keyname, keyname, true);
	}

	@Override
	public void addConfig(String modname, String keyname, boolean defvalue) {
		addConfig(modname, keyname, keyname, defvalue);
	}

	@Override
	public void addConfigRemote(String modname, String keyname) {
		addConfigRemote(modname, keyname, true);
	}

	@Override
	public void addConfigRemote(String modname, String keyname, boolean defvalue) {
		addConfig(modname, keyname, keyname, defvalue);
	}

	// ---- Block provider methods ----

	@Override
	public void registerStackProvider(IWailaDataProvider dataProvider, Class block) {
		HwylaBlockProviderBridge bridge = new HwylaBlockProviderBridge(
				dataProvider, configHandler, HwylaBlockProviderBridge.Role.STACK,
				uid("block", dataProvider, block, "stack"));
		registerBlockComponent(bridge, block);
	}

	@Override
	public void registerHeadProvider(IWailaDataProvider dataProvider, Class block) {
		HwylaBlockProviderBridge bridge = new HwylaBlockProviderBridge(
				dataProvider, configHandler, HwylaBlockProviderBridge.Role.HEAD,
				uid("block", dataProvider, block, "head"));
		registerBlockComponent(bridge, block);
	}

	@Override
	public void registerBodyProvider(IWailaDataProvider dataProvider, Class block) {
		HwylaBlockProviderBridge bridge = new HwylaBlockProviderBridge(
				dataProvider, configHandler, HwylaBlockProviderBridge.Role.BODY,
				uid("block", dataProvider, block, "body"));
		registerBlockComponent(bridge, block);
	}

	@Override
	public void registerTailProvider(IWailaDataProvider dataProvider, Class block) {
		HwylaBlockProviderBridge bridge = new HwylaBlockProviderBridge(
				dataProvider, configHandler, HwylaBlockProviderBridge.Role.TAIL,
				uid("block", dataProvider, block, "tail"));
		registerBlockComponent(bridge, block);
	}

	// 1.12.2 shim: on a dedicated server Jade never calls IWailaPlugin.registerClient, so the
	// client registration handle is absent. The old embedded code registered client bridges into
	// a dead client singleton there; skipping them is equivalent (they would be discarded anyway).
	private void registerBlockComponent(IComponentProvider<BlockAccessor> bridge, Class block) {
		if (clientReg != null) {
			clientReg.registerBlockComponent(bridge, block);
		}
	}

	@Override
	public void registerNBTProvider(IWailaDataProvider dataProvider, Class block) {
		HwylaBlockServerDataBridge bridge = new HwylaBlockServerDataBridge(
				dataProvider, uid("block_data", dataProvider, block, "nbt"));
		commonReg.registerBlockDataProvider(bridge, (Class<? extends Block>) block);
	}

	// ---- Entity provider methods ----

	@Override
	public void registerHeadProvider(IWailaEntityProvider dataProvider, Class entity) {
		HwylaEntityProviderBridge bridge = new HwylaEntityProviderBridge(
				dataProvider, configHandler, HwylaEntityProviderBridge.Role.HEAD,
				uid("entity", dataProvider, entity, "head"));
		registerEntityComponent(bridge, entity);
	}

	@Override
	public void registerBodyProvider(IWailaEntityProvider dataProvider, Class entity) {
		HwylaEntityProviderBridge bridge = new HwylaEntityProviderBridge(
				dataProvider, configHandler, HwylaEntityProviderBridge.Role.BODY,
				uid("entity", dataProvider, entity, "body"));
		registerEntityComponent(bridge, entity);
	}

	@Override
	public void registerTailProvider(IWailaEntityProvider dataProvider, Class entity) {
		HwylaEntityProviderBridge bridge = new HwylaEntityProviderBridge(
				dataProvider, configHandler, HwylaEntityProviderBridge.Role.TAIL,
				uid("entity", dataProvider, entity, "tail"));
		registerEntityComponent(bridge, entity);
	}

	private void registerEntityComponent(IEntityComponentProvider bridge, Class entity) {
		if (clientReg != null) {
			clientReg.registerEntityComponent(bridge, entity);
		}
	}

	@Override
	public void registerOverrideEntityProvider(IWailaEntityProvider dataProvider, Class entity) {
		warnUnsupported("registerOverrideEntityProvider(IWailaEntityProvider, Class)");
	}

	@Override
	public void registerNBTProvider(IWailaEntityProvider dataProvider, Class entity) {
		HwylaEntityServerDataBridge bridge = new HwylaEntityServerDataBridge(
				dataProvider, uid("entity_data", dataProvider, entity, "nbt"));
		commonReg.registerEntityDataProvider(bridge, (Class<? extends net.minecraft.entity.Entity>) entity);
	}

	private ResourceLocation uid(String type, Object provider, Class<?> target, String role) {
		String providerName = provider.getClass().getName();
		String targetName = target.getName();
		String seed = type + "|" + providerName + "|" + targetName + "|" + role;
		int ordinal = uidOrdinals.getOrDefault(seed, 0);
		ResourceLocation result;
		do {
			String suffix = ordinal == 0 ? "" : "_" + ordinal;
			String path = "hwyla_" + type + "_" + role + "_" + slug(providerName) + "_" + slug(targetName) + suffix;
			result = JadeIds.JADE(path);
			ordinal++;
		} while (!allocatedUids.add(result));
		uidOrdinals.put(seed, ordinal);
		return result;
	}

	private static String slug(String value) {
		String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
		return normalized + "_" + Integer.toUnsignedString(value.hashCode(), 36);
	}

	// ---- FMP (unsupported) ----

	@Override
	public void registerHeadProvider(IWailaFMPProvider dataProvider, String name) {
		warnUnsupported("registerHeadProvider(IWailaFMPProvider, String)");
	}

	@Override
	public void registerBodyProvider(IWailaFMPProvider dataProvider, String name) {
		warnUnsupported("registerBodyProvider(IWailaFMPProvider, String)");
	}

	@Override
	public void registerTailProvider(IWailaFMPProvider dataProvider, String name) {
		warnUnsupported("registerTailProvider(IWailaFMPProvider, String)");
	}

	// ---- Decorators (unsupported) ----

	@Override
	public void registerDecorator(IWailaBlockDecorator decorator, Class block) {
		warnUnsupported("registerDecorator(IWailaBlockDecorator, Class)");
	}

	@Override
	public void registerDecorator(IWailaFMPDecorator decorator, String name) {
		warnUnsupported("registerDecorator(IWailaFMPDecorator, String)");
	}

	// ---- Tooltip renderers (unsupported) ----

	@Override
	public void registerTooltipRenderer(String name, IWailaTooltipRenderer renderer) {
		warnUnsupported("registerTooltipRenderer(String, IWailaTooltipRenderer)");
	}

	private void warnUnsupported(String method) {
		if (warnedUnsupported.add(method)) {
			LOGGER.warn("HWYLA compat: {} is not supported by Jade; call ignored", method);
		}
	}
}
