package snownee.jade.api.view;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import snownee.jade.api.DataCodec;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.ui.Element;
import snownee.jade.api.ui.JadeUI;
import snownee.jade.api.ui.NarratableComponent;
import snownee.jade.util.FluidTextHelper;

/**
 * Client-side representation of a fluid storage view.
 */
public class FluidView {

	public static final ITextComponent EMPTY_FLUID = new TextComponentTranslation("jade.fluid.empty");

	/**
	 * Overlay element rendered for the fluid.
	 */
	public Element overlay;
	/**
	 * Current amount text.
	 */
	public ITextComponent current;
	/**
	 * Maximum amount text.
	 */
	public ITextComponent max;
	/**
	 * Fill ratio.
	 */
	public float ratio;
	/**
	 * Optional fluid name.
	 */
	@Nullable
	public ITextComponent fluidName;
	/**
	 * Optional override text.
	 */
	@Nullable
	public ITextComponent overrideText;

	/**
	 * Creates a fluid view.
	 *
	 * @param overlay overlay element
	 * @param current current amount text
	 * @param max maximum amount text
	 */
	public FluidView(Element overlay, ITextComponent current, ITextComponent max) {
		this.overlay = Objects.requireNonNull(overlay);
		this.current = Objects.requireNonNull(current);
		this.max = Objects.requireNonNull(max);
	}

	/**
	 * Builds a default fluid view from serialized data.
	 *
	 * @param data serialized fluid storage data
	 * @return fluid view, or {@code null} if the data is not renderable
	 */
	@Nullable
	public static FluidView readDefault(Data data) {
		if (data.capacity <= 0 || data.fluids.size() > 1) {
			return null;
		}
		JadeFluidObject fluidObject = data.fluids.isEmpty() ? JadeFluidObject.empty() : data.fluids.get(0);
		long amount = fluidObject.getAmount();
		ITextComponent current = FluidTextHelper.getMillibuckets(amount, true);
		ITextComponent max = FluidTextHelper.getMillibuckets(data.capacity, true);
		FluidView view = new FluidView(JadeUI.fluid(fluidObject), current, max);
		view.fluidName = fluidObject.getDisplayName();
		view.ratio = (float) ((double) amount / data.capacity);
		if (fluidObject.isEmpty()) {
			view.overrideText = NarratableComponent.translatable(
					"jade.fluid",
					EMPTY_FLUID,
					NarratableComponent.attach(new TextComponentString(view.max.getUnformattedComponentText()), view.max));
		}
		return view;
	}

	/**
	 * Serialized fluid storage data.
	 */
	public record Data(List<JadeFluidObject> fluids, long capacity) {
		public static final DataCodec<Data> STREAM_CODEC = new DataCodec<>() {
			@Override
			public Data decode(PacketBuffer buf) {
				int size = buf.readVarInt();
				List<JadeFluidObject> fluids = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					fluids.add(JadeFluidObject.STREAM_CODEC.decode(buf));
				}
				return new Data(fluids, buf.readLong());
			}

			@Override
			public void encode(PacketBuffer buf, Data value) {
				buf.writeVarInt(value.fluids.size());
				for (JadeFluidObject fluid : value.fluids) {
					JadeFluidObject.STREAM_CODEC.encode(buf, fluid);
				}
				buf.writeLong(value.capacity);
			}
		};

		/**
		 * Creates a single-fluid payload.
		 *
		 * @param fluid fluid stack
		 * @param capacity storage capacity
		 */
		public Data(JadeFluidObject fluid, long capacity) {
			this(Arrays.asList(fluid), capacity);
		}
	}

}
