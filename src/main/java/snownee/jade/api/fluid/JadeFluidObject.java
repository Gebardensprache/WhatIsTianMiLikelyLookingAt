package snownee.jade.api.fluid;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import snownee.jade.api.DataCodec;
import snownee.jade.util.CommonProxy;

/**
 * Fluid stack representation used by Jade's client and network codecs.
 */
public class JadeFluidObject {
	/**
	 * Codec for serialized fluid objects. (1.12.2: stubbed to a simple fluid-name/amount record.)
	 */
	public static final Codec<JadeFluidObject> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Codec.STRING.fieldOf("type").forGetter(o -> o.fluid.getName()),
					Codec.LONG.fieldOf("amount").forGetter(JadeFluidObject::getAmount))
			.apply(instance, JadeFluidObject::of));

	/**
	 * Network codec for fluid objects. (1.12.2: transmits fluid registry name + amount.)
	 */
	public static final DataCodec<JadeFluidObject> STREAM_CODEC = new DataCodec<>() {
		@Override
		public JadeFluidObject decode(PacketBuffer buf) {
			return of(buf.readString(32767), buf.readLong());
		}

		@Override
		public void encode(PacketBuffer buf, JadeFluidObject o) {
			buf.writeString(o.fluid.getName());
			buf.writeLong(o.amount);
		}
	};

	/**
	 * Returns the amount represented by one bucket.
	 *
	 * @return bucket volume
	 */
	public static long bucketVolume() {
		return CommonProxy.bucketVolume();
	}

	/**
	 * Returns the amount represented by one block.
	 *
	 * @return block volume
	 */
	public static long blockVolume() {
		return CommonProxy.blockVolume();
	}

	private static final JadeFluidObject EMPTY = of(FluidRegistry.WATER, 0);

	/**
	 * Creates an empty fluid object.
	 *
	 * @return empty fluid object
	 */
	public static JadeFluidObject empty() {
		return EMPTY;
	}

	public static JadeFluidObject of(String name, long amount) {
		Fluid fluid = FluidRegistry.getFluid(name);
		if (fluid == null) {
			fluid = FluidRegistry.WATER;
		}
		return of(fluid, amount);
	}

	/**
	 * Creates a fluid object with the given name, amount and tag.
	 *
	 * @param name fluid registry name
	 * @param amount amount in millibuckets
	 * @param tag optional fluid stack tag
	 * @return fluid object
	 */
	public static JadeFluidObject of(String name, long amount, @Nullable NBTTagCompound tag) {
		JadeFluidObject object = of(name, amount);
		object.tag = tag;
		return object;
	}

	/**
	 * Creates a full block-volume fluid object.
	 *
	 * @param fluid fluid type
	 * @return fluid object
	 */
	public static JadeFluidObject of(Fluid fluid) {
		return of(fluid, blockVolume());
	}

	/**
	 * Creates a fluid object with the given amount.
	 *
	 * @param fluid fluid type
	 * @param amount amount in millibuckets
	 * @return fluid object
	 */
	public static JadeFluidObject of(Fluid fluid, long amount) {
		return new JadeFluidObject(fluid, amount);
	}

	private final Fluid fluid;
	private final long amount;
	@Nullable
	private NBTTagCompound tag;

	/**
	 * Creates a fluid object.
	 *
	 * @param fluid fluid type
	 * @param amount amount in millibuckets
	 */
	private JadeFluidObject(Fluid fluid, long amount) {
		this.fluid = Objects.requireNonNull(fluid);
		this.amount = amount;
	}

	public Fluid getFluid() {
		return fluid;
	}

	/**
	 * Returns the stored amount.
	 *
	 * @return amount in millibuckets
	 */
	public long getAmount() {
		return amount;
	}

	/**
	 * Returns the attached NBT tag, if any.
	 *
	 * @return fluid stack tag, or {@code null}
	 */
	@Nullable
	public NBTTagCompound getTag() {
		return tag;
	}

	/**
	 * Returns whether this object represents no fluid.
	 *
	 * @return {@code true} if empty
	 */
	public boolean isEmpty() {
		return amount == 0;
	}

	public boolean is(Fluid other) {
		return fluid == other;
	}

	/**
	 * Returns the display name for this fluid object.
	 *
	 * @return fluid name
	 */
	public ITextComponent getDisplayName() {
		// 1.12.2: CommonProxy.getFluidName returns a plain String; wrap it in a component
		return new TextComponentString(CommonProxy.getFluidName(this));
	}

	public static boolean isSameFluidSameComponents(JadeFluidObject first, JadeFluidObject second) {
		return first.fluid == second.fluid;
	}
}
