package snownee.jade.shim.top.testmod;

import java.util.function.Function;

import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.event.FMLInterModComms;

/**
 * Test-mod fixture: registers a TOP provider through the shim's function-IMC seam, mirroring
 * the Jade root's {@code snownee.jade.testmod.TopTestIntegration} (same provider contract).
 */
public class TopTestIntegration {

	public static void register() {
		FMLInterModComms.sendFunctionMessage("theoneprobe", "getTheOneProbe",
				"snownee.jade.shim.top.testmod.TopTestIntegration$GetTheOneProbe");
	}

	public static class GetTheOneProbe implements Function<ITheOneProbe, Void> {
		@Override
		public Void apply(ITheOneProbe probe) {
			probe.registerProvider(new FurnaceProvider());
			return null;
		}
	}

	public static class FurnaceProvider implements IProbeInfoProvider {

		@Override
		public String getID() {
			return "jadetopshimtestmod:furnace";
		}

		@Override
		public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player,
				World world, IBlockState blockState, IProbeHitData data) {
			if (blockState.getBlock() == Blocks.FURNACE || blockState.getBlock() == Blocks.LIT_FURNACE) {
				probeInfo.horizontal()
						.item(new ItemStack(Items.COAL))
						.text("TOP Test: Furnace")
						.progress(42, 100);
			}
		}
	}
}
