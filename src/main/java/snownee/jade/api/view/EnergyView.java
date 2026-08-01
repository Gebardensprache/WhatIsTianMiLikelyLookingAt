package snownee.jade.api.view;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import snownee.jade.api.DataCodec;
import snownee.jade.api.ui.IDisplayHelper;

/**
 * Client-side representation of a stored energy value.
 */
public class EnergyView {

	/**
	 * Current value formatted for display.
	 */
	public String current;
	/**
	 * Maximum value formatted for display.
	 */
	public String max;
	/**
	 * Current fill ratio.
	 */
	public float ratio;
	/**
	 * Optional override text.
	 */
	public @Nullable ITextComponent overrideText;

	/**
	 * Creates an energy view from formatted strings.
	 *
	 * @param current current value text
	 * @param max maximum value text
	 */
	public EnergyView(String current, String max) {
		this.current = Objects.requireNonNull(current);
		this.max = Objects.requireNonNull(max);
	}

	/**
	 * Builds an energy view from raw numeric data.
	 *
	 * @param data serialized energy data
	 * @param unit display unit, such as {@code FE}
	 * @return energy view, or {@code null} if capacity is not positive
	 */
	@Nullable
	public static EnergyView read(Data data, String unit) {
		if (data.capacity <= 0) {
			return null;
		}
		String current = IDisplayHelper.get().humanReadableNumber(data.current, unit, false);
		String max = IDisplayHelper.get().humanReadableNumber(data.capacity, unit, false);
		EnergyView energyView = new EnergyView(current, max);
		energyView.ratio = (float) data.current / data.capacity;
		return energyView;
	}

	/**
	 * Serialized energy data.
	 */
	public record Data(long current, long capacity) {
		public static final DataCodec<Data> STREAM_CODEC = new DataCodec<>() {
			@Override
			public Data decode(PacketBuffer buf) {
				return new Data(buf.readLong(), buf.readLong());
			}

			@Override
			public void encode(PacketBuffer buf, Data value) {
				buf.writeLong(value.current);
				buf.writeLong(value.capacity);
			}
		};
	}

}
