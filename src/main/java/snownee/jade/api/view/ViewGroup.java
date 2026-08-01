package snownee.jade.api.view;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import snownee.jade.api.DataCodec;

/**
 * Logical group of views that should be rendered together.
 *
 * @param <T> contained view type
 */
public class ViewGroup<T> {
	/**
	 * Returns a codec for a single view group.
	 *
	 * @param viewCodec codec for individual views
	 * @param <T> view type
	 * @return view-group codec
	 */
	public static <T> DataCodec<ViewGroup<T>> codec(DataCodec<T> viewCodec) {
		return new DataCodec<>() {
			@Override
			public ViewGroup<T> decode(PacketBuffer buf) {
				int size = buf.readVarInt();
				List<T> views = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					views.add(viewCodec.decode(buf));
				}
				String id = buf.readBoolean() ? buf.readString(32767) : null;
				NBTTagCompound extraData = buf.readBoolean() ? DataCodec.readTag(buf) : null;
				return new ViewGroup<>(views, Optional.ofNullable(id), Optional.ofNullable(extraData));
			}

			@Override
			public void encode(PacketBuffer buf, ViewGroup<T> value) {
				buf.writeVarInt(value.views.size());
				for (T view : value.views) {
					viewCodec.encode(buf, view);
				}
				if (value.id != null) {
					buf.writeBoolean(true);
					buf.writeString(value.id);
				} else {
					buf.writeBoolean(false);
				}
				if (value.extraData != null) {
					buf.writeBoolean(true);
					buf.writeCompoundTag(value.extraData);
				} else {
					buf.writeBoolean(false);
				}
			}
		};
	}

	/**
	 * Returns a codec for a named list of view groups.
	 *
	 * @param viewCodec codec for individual views
	 * @param <T> view type
	 * @return named list codec
	 */
	public static <T> DataCodec<Map.Entry<ResourceLocation, List<ViewGroup<T>>>> listCodec(DataCodec<T> viewCodec) {
		DataCodec<ViewGroup<T>> groupCodec = codec(viewCodec);
		return new DataCodec<>() {
			@Override
			public Map.Entry<ResourceLocation, List<ViewGroup<T>>> decode(PacketBuffer buf) {
				ResourceLocation key = new ResourceLocation(buf.readString(32767));
				int size = buf.readVarInt();
				List<ViewGroup<T>> groups = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					groups.add(groupCodec.decode(buf));
				}
				return new AbstractMap.SimpleEntry<>(key, groups);
			}

			@Override
			public void encode(PacketBuffer buf, Map.Entry<ResourceLocation, List<ViewGroup<T>>> value) {
				buf.writeString(value.getKey().toString());
				buf.writeVarInt(value.getValue().size());
				for (ViewGroup<T> group : value.getValue()) {
					groupCodec.encode(buf, group);
				}
			}
		};
	}

	/**
	 * Writes this view group's data to a PacketBuffer.
	 *
	 * @param buf the buffer to write to
	 * @param viewCodec codec for individual views
	 */
	public void write(PacketBuffer buf, DataCodec<T> viewCodec) {
		codec(viewCodec).encode(buf, this);
	}

	/**
	 * Reads a view group from a PacketBuffer.
	 *
	 * @param buf the buffer to read from
	 * @param viewCodec codec for individual views
	 * @param <T> view type
	 * @return the decoded view group
	 */
	public static <T> ViewGroup<T> read(PacketBuffer buf, DataCodec<T> viewCodec) {
		return codec(viewCodec).decode(buf);
	}

	/**
	 * Views in this group.
	 */
	public List<T> views;
	/**
	 * Optional group identifier.
	 */
	@Nullable
	public String id;
	/**
	 * Optional extra rendering data.
	 */
	@Nullable
	protected NBTTagCompound extraData;

	/**
	 * Creates a group with no explicit id or extra data.
	 *
	 * @param views contained views
	 */
	public ViewGroup(List<T> views) {
		this(views, Optional.empty(), Optional.empty());
	}

	/**
	 * Creates a group with optional id and extra data.
	 *
	 * @param views contained views
	 * @param id optional group id
	 * @param extraData optional extra data
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public ViewGroup(List<T> views, Optional<String> id, Optional<NBTTagCompound> extraData) {
		this.views = views;
		this.id = id.orElse(null);
		this.extraData = extraData.orElse(null);
	}

	/**
	 * Returns the mutable extra data tag.
	 *
	 * @return extra data tag
	 */
	public NBTTagCompound getExtraData() {
		if (extraData == null) {
			extraData = new NBTTagCompound();
		}
		return extraData;
	}

	/**
	 * Stores the render progress in the extra data tag.
	 *
	 * @param progress progress value in the {@code 0..1} range
	 */
	public void setProgress(float progress) {
		getExtraData().setFloat("Progress", progress);
	}
}
