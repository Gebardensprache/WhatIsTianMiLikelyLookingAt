package snownee.jade.shim.hwyla.testmod;

import java.util.List;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import mcp.mobius.waila.api.IWailaRegistrar;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.block.BlockWorkbench;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Test-mod fixture: a HWYLA plugin registered through the shim's ASM-scan seam, mirroring the
 * Jade root's {@code snownee.jade.testmod.HwylaTestPlugin} (same provider contract).
 */
@WailaPlugin(TestMod.MODID)
public class HwylaTestPlugin implements mcp.mobius.waila.api.IWailaPlugin {

	@Override
	public void register(IWailaRegistrar registrar) {
		registrar.registerHeadProvider(new CraftingTableProvider(), BlockWorkbench.class);
		registrar.registerBodyProvider(new CraftingTableProvider(), BlockWorkbench.class);
		registrar.registerTailProvider(new CraftingTableProvider(), BlockWorkbench.class);
		registrar.registerStackProvider(new CraftingTableProvider(), BlockWorkbench.class);
		registrar.registerNBTProvider(new CraftingTableProvider(), BlockWorkbench.class);
	}

	public static class CraftingTableProvider implements IWailaDataProvider {

		@Override
		public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
			return new ItemStack(Items.NAME_TAG);
		}

		@Override
		public List<String> getWailaHead(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			currenttip.add("HWYLA Test: Crafting Table Head");
			return currenttip;
		}

		@Override
		public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			currenttip.add("HWYLA Test: Body line 1");
			int counter = accessor.getNBTData().getInteger("hwylaCounter");
			currenttip.add("HWYLA NBT counter: " + counter);
			return currenttip;
		}

		@Override
		public List<String> getWailaTail(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
			currenttip.add("HWYLA Test: Tail");
			return currenttip;
		}

		@Override
		public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, BlockPos pos) {
			tag.setInteger("hwylaCounter", tag.getInteger("hwylaCounter") + 1);
			return tag;
		}
	}
}
