package snownee.jade.test;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@EventBusSubscriber(modid = "jade")
public class Test {

	public static TestBlock BLOCK;

	@SubscribeEvent
	public static void registerBlocks(Register<Block> event) {
		event.getRegistry().register((BLOCK = new TestBlock()).setRegistryName("test"));
		// 1.12.2: there is no tile-entity registry event; tile entities are keyed by class
		// via GameRegistry, so the modern registerTileTypes handler is folded into block
		// registration. The "test" key mirrors the modern block entity type registration.
		GameRegistry.registerTileEntity(TestBlockEntity.class, "test");
	}

	@SubscribeEvent
	public static void registerItems(Register<Item> event) {
		event.getRegistry().register(new ItemBlock(BLOCK).setRegistryName("test"));
	}

}
