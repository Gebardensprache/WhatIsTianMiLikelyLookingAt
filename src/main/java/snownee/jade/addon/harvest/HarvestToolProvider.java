package snownee.jade.addon.harvest;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import snownee.jade.Jade;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.TooltipPosition;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.harvest.ToolResult;
import snownee.jade.api.harvest.ToolType;
import snownee.jade.api.theme.IThemeHelper;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.util.ClientProxy;
import snownee.jade.util.CommonProxy;

public class HarvestToolProvider implements IBlockComponentProvider {
	public static final HarvestToolProvider INSTANCE = new HarvestToolProvider();

	private static final ITextComponent CHECK = new TextComponentString("✔");
	private static final ITextComponent X = new TextComponentString("✕");
	// 1.12.2: guava 21 has no CacheBuilder.expireAfterAccess(Duration); use the
	// (long, TimeUnit) overload (5 minutes).
	private final Cache<IBlockState, ImmutableList<ItemStack>> resultCache = CacheBuilder.newBuilder()
			.expireAfterAccess(5, TimeUnit.MINUTES)
			.build();

	static {
		// 1.12.2: tags do not reload; the common proxy fires this after server loading instead.
		CommonProxy.registerTagsUpdatedListener((server, client) -> apply());
	}

	public static ImmutableList<ItemStack> getTool(IBlockState state, World level, BlockPos pos) {
		ImmutableList.Builder<ItemStack> tools = ImmutableList.builder();
		for (ToolType handler : ToolTypeRegistryImpl.registeredTypes().values()) {
			ToolResult result = handler.test(state, level, pos);
			if (result.isSuccess()) {
				tools.add(result.displayStack());
			}
		}
		return tools.build();
	}

	private static void apply() {
		ToolTypeRegistryImpl.apply();
		INSTANCE.resultCache.invalidateAll();
	}

	@Override
	public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
		EntityPlayer player = accessor.getPlayer();
		if (!config.get(JadeIds.MC_HARVEST_TOOL_CREATIVE) && (player.isCreative() || player.isSpectator())) {
			return;
		}
		World level = accessor.getLevel();
		BlockPos pos = accessor.getPosition();
		GameType gameType = ClientProxy.getGameMode();
		if (gameType == GameType.ADVENTURE && !player.isAllowEdit()) {
			// 1.12.2: Player#blockActionRestricted is represented by allowEdit capabilities.
			return;
		}
		IBlockState state = accessor.getBlockState();
		try {
			if (state.getPlayerRelativeBlockHardness(player, level, pos) <= 0) {
				if (!accessor.isServersideContent() && config.get(JadeIds.MC_SHOW_UNBREAKABLE)) {
					ITextComponent text = IThemeHelper.get().failure(new TextComponentTranslation("jade.harvest_tool.unbreakable"));
					tooltip.add(JadeUI.text(text).narration(""));
				}
				return;
			}
		} catch (Exception ignored) {
			return;
		}

		boolean newLine = config.get(JadeIds.MC_HARVEST_TOOL_NEW_LINE);
		List<Element> elements = getText(accessor, config);
		if (elements.isEmpty()) {
			return;
		}
		elements.forEach(e -> e.narration(""));
		if (newLine) {
			tooltip.add(elements);
		} else {
			tooltip.append(0, elements);
		}
	}

	public List<Element> getText(BlockAccessor accessor, IPluginConfig config) {
		IBlockState state = accessor.getBlockState();
		boolean needsTool = !state.getMaterial().isToolNotRequired();
		if (!needsTool && !config.get(JadeIds.MC_EFFECTIVE_TOOL)) {
			return Collections.emptyList();
		}
		List<ItemStack> tools = Collections.emptyList();
		try {
			tools = resultCache.get(state, () -> getTool(state, accessor.getLevel(), accessor.getPosition()));
		} catch (ExecutionException e) {
			Jade.LOGGER.error("Failed to get harvest tool", e);
		}
		if (tools.isEmpty()) {
			return Collections.emptyList();
		}

		int offsetY = -3;
		boolean newLine = config.get(JadeIds.MC_HARVEST_TOOL_NEW_LINE);
		List<Element> elements = Lists.newArrayList();
		for (ItemStack tool : tools) {
			elements.add(JadeUI.item(tool, 0.75f).offset(-1, offsetY).size(10, 0).narration(""));
		}

		if (!elements.isEmpty()) {
			// 1.12.2: no List#addFirst in the Java 8 collection API.
			elements.add(0, JadeUI.spacer(newLine ? -2 : 5, newLine ? 10 : 0).flexGrow(1000));
			EntityPlayer player = accessor.getPlayer();
			boolean canHarvest = CommonProxy.isCorrectToolForDrops(state, player, accessor.getLevel(), accessor.getPosition());
			if (needsTool || !canHarvest) {
				IThemeHelper t = IThemeHelper.get();
				ITextComponent text = canHarvest ? t.success(CHECK) : t.danger(X);
				elements.add(JadeUI.text(text)
						.scale(0.75F)
						.size(0, 0)
						.offset(-3, 6 + offsetY));
			}
		}

		return elements;
	}

	public void invalidateCache() {
		resultCache.invalidateAll();
	}

	public void setShearableBlocks(Collection<Block> blocks) {
		ToolTypeRegistryImpl.DEFAULT_SHEARS_TIER.get().replaceExtraBlocks(blocks);
		invalidateCache();
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_HARVEST_TOOL;
	}

	@Override
	public int getDefaultPriority() {
		return TooltipPosition.TAIL - 2000;
	}
}
