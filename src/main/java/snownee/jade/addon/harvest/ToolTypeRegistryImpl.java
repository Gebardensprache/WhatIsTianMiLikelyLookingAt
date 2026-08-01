package snownee.jade.addon.harvest;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.util.ResourceLocation;
import snownee.jade.Jade;
import snownee.jade.api.callback.CallbackContainer;
import snownee.jade.api.harvest.ToolTier;
import snownee.jade.api.harvest.ToolTierAddedCallback;
import snownee.jade.api.harvest.ToolType;
import snownee.jade.api.harvest.ToolTypeRegistry;
import snownee.jade.impl.WailaClientRegistration;

public final class ToolTypeRegistryImpl implements ToolTypeRegistry {

	private static ImmutableMap<ResourceLocation, ToolType> TOOL_TYPES = ImmutableMap.of();
	public static final Supplier<ToolTier> DEFAULT_SHEARS_TIER = Suppliers.memoize(() -> ToolTier.item(Items.SHEARS));
	private final Map<ResourceLocation, ToolType> pendingMap = Maps.newLinkedHashMap();
	private final Map<ResourceLocation, CallbackContainer<ToolTierAddedCallback>> pendingCallbacks = Maps.newHashMap();

	private ToolTypeRegistryImpl() {
	}

	@Override
	public ToolType type(ToolType type) {
		Objects.requireNonNull(type);
		ResourceLocation id = type.getUid();
		Objects.requireNonNull(id);
		if (pendingMap.containsKey(id)) {
			Jade.LOGGER.warn("Skipped duplicate harvest tool type registration: {}", id);
			return pendingMap.get(id);
		}
		pendingMap.put(id, type);
		CallbackContainer<ToolTierAddedCallback> callbacks = pendingCallbacks.remove(id);
		if (callbacks != null) {
			for (ToolTierAddedCallback callback : callbacks.callbacks()) {
				type.tierAddedCallbacks().add(callback);
			}
		}
		return type;
	}

	@Override
	public ToolType type(ResourceLocation id) {
		return type(id, true);
	}

	@Override
	public ToolType type(ResourceLocation id, boolean skipInstaBreakingBlock) {
		return type(ToolType.of(id, skipInstaBreakingBlock));
	}

	@Override
	public @Nullable ToolType get(ResourceLocation typeId) {
		return pendingMap.get(typeId);
	}

	@Override
	public void insertTierAfter(ResourceLocation typeId, ResourceLocation targetTier, ToolTier tier) {
		ToolType type = get(typeId);
		if (type == null || !type.insertTierAfter(targetTier, tier)) {
			CallbackContainer<ToolTierAddedCallback> callbacks = tierAddedCallback(typeId);
			callbacks.add((t, tierId, t2) -> {
				if (tierId.equals(targetTier)) {
					t.insertTierAfter(targetTier, tier);
				}
			});
		}
	}

	@Override
	public void insertTierBefore(ResourceLocation typeId, ResourceLocation targetTier, ToolTier tier) {
		ToolType type = get(typeId);
		if (type == null || !type.insertTierBefore(targetTier, tier)) {
			CallbackContainer<ToolTierAddedCallback> callbacks = tierAddedCallback(typeId);
			callbacks.add((t, tierId, t2) -> {
				if (tierId.equals(targetTier)) {
					t.insertTierBefore(targetTier, tier);
				}
			});
		}
	}

	@Override
	public CallbackContainer<ToolTierAddedCallback> tierAddedCallback(ResourceLocation typeId) {
		ToolType type = get(typeId);
		if (type != null) {
			return type.tierAddedCallbacks();
		}
		return pendingCallbacks.computeIfAbsent(typeId, _ -> new CallbackContainer<>());
	}

	@Override
	public ToolTier defaultShearsTier() {
		return DEFAULT_SHEARS_TIER.get();
	}

	public static synchronized Map<ResourceLocation, ? extends ToolType> registeredTypes() {
		return TOOL_TYPES;
	}

	public static synchronized void apply() {
		clear();
		ToolTypeRegistryImpl registry = new ToolTypeRegistryImpl();
		for (Consumer<ToolTypeRegistry> plugin : WailaClientRegistration.instance().harvestPlugins) {
			try {
				plugin.accept(registry);
			} catch (Throwable t) {
				Jade.LOGGER.error("Failed to apply harvest plugin {}", plugin, t);
			}
		}
		registry.pendingMap.entrySet().removeIf(entry -> entry.getValue().tiers().isEmpty());
		TOOL_TYPES = ImmutableMap.copyOf(registry.pendingMap);
	}

	public static synchronized void clear() {
		// 1.12.2: no List.of in the Java 8 runtime API.
		DEFAULT_SHEARS_TIER.get().replaceExtraBlocks(Collections.singletonList(Blocks.TRIPWIRE));
		TOOL_TYPES = ImmutableMap.of();
	}
}
