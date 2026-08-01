package snownee.jade.test;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.DataCodec;
import snownee.jade.api.StreamServerDataProvider;

public class ExampleDataProvider implements StreamServerDataProvider<BlockAccessor, Integer> {
	public static final ExampleDataProvider INSTANCE = new ExampleDataProvider();

	@Override
	public @Nullable Integer streamData(BlockAccessor accessor) {
		// 1.12.2: TileEntityFurnace hides litTime in the field-id protocol (getField(0)).
		return accessor.<TileEntityFurnace>typedBlockEntity().getField(0);
	}

	@Override
	public DataCodec<Integer> streamCodec() {
		return new DataCodec<>() {
			@Override
			public Integer decode(PacketBuffer buf) {
				return buf.readVarInt();
			}

			@Override
			public void encode(PacketBuffer buf, Integer value) {
				buf.writeVarInt(value);
			}
		};
	}

	@Override
	public ResourceLocation getUid() {
		return ExamplePlugin.UID_TEST_FUEL;
	}
}
