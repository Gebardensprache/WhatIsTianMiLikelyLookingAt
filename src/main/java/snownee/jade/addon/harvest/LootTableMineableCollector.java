package snownee.jade.addon.harvest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootEntryTable;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import snownee.jade.Jade;
import snownee.jade.util.CommonProxy;

public class LootTableMineableCollector {
	private static List<Block> shearableBlocks = Collections.emptyList();
	private final World world;
	private final ItemStack toolItem;

	public LootTableMineableCollector(World world, ItemStack toolItem) {
		this.world = world;
		this.toolItem = toolItem;
	}

	public static List<Block> execute(World world, ItemStack toolItem) {
		Stopwatch stopwatch = null;
		if (CommonProxy.isDevEnv()) {
			stopwatch = Stopwatch.createStarted();
		}
		LootTableMineableCollector collector = new LootTableMineableCollector(world, toolItem);
		ImmutableList.Builder<Block> list = ImmutableList.builder();
		// 1.12.2: blocks do not retain a direct loot-table id; use their registry id as the
		// conventional blocks/<path> table key and let absent tables resolve to EMPTY_LOOT_TABLE.
		for (Block block : ForgeRegistries.BLOCKS) {
			ResourceLocation blockId = block.getRegistryName();
			if (blockId == null) {
				continue;
			}
			ResourceLocation lootId = new ResourceLocation(blockId.getNamespace(), "blocks/" + blockId.getPath());
			LootTable lootTable = world.getLootTableManager().getLootTableFromLocation(lootId);
			if (collector.doLootTable(lootTable)) {
				list.add(block);
			}
		}
		if (stopwatch != null) {
			Jade.LOGGER.info("LootTableMineableCollector took {}", stopwatch.stop());
		}
		list.add(Blocks.TRIPWIRE);
		return list.build();
	}

	private boolean doLootTable(@Nullable LootTable lootTable) {
		if (lootTable == null || lootTable == LootTable.EMPTY_LOOT_TABLE) {
			return false;
		}
		for (LootPool pool : lootTable.pools) {
			if (doLootPool(pool)) {
				return true;
			}
		}
		return false;
	}

	private boolean doLootPool(LootPool lootPool) {
		for (LootEntry entry : lootPool.lootEntries) {
			if (doLootPoolEntry(entry)) {
				return true;
			}
		}
		return false;
	}

	private boolean doLootPoolEntry(LootEntry entry) {
		if (entry instanceof LootEntryTable tableEntry) {
			return CommonProxy.isCorrectConditions(Arrays.asList(tableEntry.conditions), toolItem) &&
					doLootTable(world.getLootTableManager().getLootTableFromLocation(tableEntry.table));
		}
		if (entry instanceof LootEntryItem) {
			return CommonProxy.isCorrectConditions(Arrays.asList(entry.conditions), toolItem);
		}
		// Only actual item entries, directly or through a qualifying table reference, are evidence of a drop.
		return false;
	}

	public static void onTagsUpdated(@Nullable MinecraftServer server) {
		onTagsUpdated(server, false);
	}

	public static void onTagsUpdated(@Nullable MinecraftServer server, boolean client) {
		// 1.12.2: tag reload has no modern registry lookup. The server's overworld owns the loot manager.
		if (server == null) {
			return;
		}
		try {
			shearableBlocks = LootTableMineableCollector.execute(server.getWorld(0), new ItemStack(Items.SHEARS));
		} catch (Throwable e) {
			Jade.LOGGER.error("Failed to collect shearable blocks", e);
			shearableBlocks = Collections.emptyList();
		}
	}

	public static List<Block> getShearableBlocks() {
		return shearableBlocks;
	}
}
