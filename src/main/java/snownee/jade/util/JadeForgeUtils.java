package snownee.jade.util;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.math.LongMath;
import com.mojang.datafixers.util.Pair;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import snownee.jade.addon.universal.ItemIterator;
import snownee.jade.api.Accessor;
import snownee.jade.api.fluid.JadeFluidObject;
import snownee.jade.api.view.FluidView;
import snownee.jade.api.view.ViewGroup;

public class JadeForgeUtils {

	private JadeForgeUtils() {
	}

	public static JadeFluidObject fromFluidStack(FluidStack fs) {
		return JadeFluidObject.of(fs.getFluid().getName(), fs.amount, fs.tag);
	}

	public static List<ViewGroup<FluidView.Data>> fromFluidHandler(IFluidHandler storage) {
		int tanks = storage.getTankProperties().length;
		if (tanks == 0) {
			return List.of();
		}
		FluidCollectingResult result = fromFluidHandlerStream(storage);
		if (result.tanks == 0) {
			return List.of();
		}
		List<Pair<JadeFluidObject, Long>> list = Lists.newArrayList();
		int maxTanks = result.emptyTanks == 0 ? 5 : 4;
		if (result.tanks - result.emptyTanks <= maxTanks) {
			list.addAll(result.stream.toList());
		} else {
			result.stream.takeWhile(tag -> list.size() <= maxTanks).forEach(Pair1 -> {
				for (Pair<JadeFluidObject, Long> Pair2 : list) {
					if (JadeFluidObject.isSameFluidSameComponents(Pair1.getFirst(), Pair2.getFirst())) {
						return;
					}
				}
				list.add(Pair1);
			});
		}
		int remaining = result.tanks - result.emptyTanks - list.size();
		if (result.emptyTanks > 0) {
			list.add(new Pair<>(JadeFluidObject.empty(), result.emptyCapacity));
		}
		ViewGroup<FluidView.Data> group = new ViewGroup<>(list.stream()
				.map(Pair -> new FluidView.Data(Pair.getFirst(), Pair.getSecond()))
				.toList());
		if (remaining > 0) {
			group.getExtraData().setInteger("+", remaining);
		}
		return List.of(group);
	}

	public static FluidCollectingResult fromFluidHandlerStream(IFluidHandler fluidHandler) {
		FluidCollectingResult result = new FluidCollectingResult();
		IFluidTankProperties[] properties = fluidHandler.getTankProperties();
		for (IFluidTankProperties property : properties) {
			if (property.getCapacity() > 0) {
				result.tanks++;
				FluidStack fs = property.getContents();
				if (fs == null || fs.amount <= 0) {
					result.emptyTanks++;
				}
			}
		}
		if (result.tanks == 0) {
			result.stream = Stream.empty();
		} else {
			result.stream = Stream.of(properties).map(property -> {
				long capacity = property.getCapacity();
				if (capacity <= 0) {
					return null;
				}
				FluidStack fs = property.getContents();
				if (fs == null || fs.amount <= 0) {
					result.emptyCapacity = LongMath.saturatedAdd(result.emptyCapacity, capacity);
					return null;
				}
				return new Pair<>(fromFluidStack(fs), capacity);
			}).filter(Objects::nonNull);
		}
		return result;
	}

	public static class FluidCollectingResult {
		public Stream<Pair<JadeFluidObject, Long>> stream = Stream.empty();
		public long emptyCapacity;
		public int tanks;
		public int emptyTanks;
	}

	public static ItemIterator<? extends IItemHandler> fromItemHandler(
			IItemHandler storage,
			int fromIndex) {
		return fromItemHandler(storage, fromIndex, CommonProxy::findItemHandler);
	}

	public static ItemIterator<? extends IItemHandler> fromItemHandler(
			IItemHandler storage,
			int fromIndex,
			Function<Accessor<?>, @Nullable IItemHandler> containerFinder) {
		return new ItemIterator.SlottedItemIterator<>(containerFinder, fromIndex) {
			@Override
			protected int getSlotCount(IItemHandler container) {
				return container.getSlots();
			}

			@Override
			protected ItemStack getItemInSlot(IItemHandler container, int slot) {
				return container.getStackInSlot(slot);
			}
		};
	}
}
