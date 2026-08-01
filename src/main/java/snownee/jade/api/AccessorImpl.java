package snownee.jade.api;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.network.PacketBuffer;

import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.RayTraceResult;

/**
 * Base implementation for Jade accessors.
 *
 * @param <T> hit result type handled by this accessor
 */
public abstract class AccessorImpl<T extends RayTraceResult> implements Accessor<T> {

	private final World level;
	private final EntityPlayer player;
	private final Supplier<T> hit;
	private final boolean serverConnected;
	private final boolean showDetails;
	protected ItemStack serversideRep = ItemStack.EMPTY;
	private NBTTagCompound serverData;
	protected boolean verify;
	private @Nullable PacketBuffer buffer;

	/**
	 * Creates a new accessor implementation.
	 *
	 * @param level current level
	 * @param player current player
	 * @param serverData synchronized server data, or {@code null}
	 * @param hit supplier for the target hit result
	 * @param serverConnected whether the dedicated server has Jade installed
	 * @param showDetails whether detailed target data should be shown
	 */
	public AccessorImpl(
			World level,
			EntityPlayer player,
			@Nullable NBTTagCompound serverData,
			Supplier<T> hit,
			boolean serverConnected,
			boolean showDetails) {
		this.level = Objects.requireNonNull(level);
		this.player = Objects.requireNonNull(player);
		this.hit = Objects.requireNonNull(hit);
		this.serverConnected = serverConnected;
		this.showDetails = showDetails;
		setServerData(serverData);
	}

	@Override
	public World getLevel() {
		return level;
	}

	@Override
	public EntityPlayer getPlayer() {
		return player;
	}

	@Override
	public final NBTTagCompound getServerData() {
		return serverData;
	}

	/**
	 * Do not call this
	 */
	@SuppressWarnings("DeprecatedIsStillUsed")
	@Deprecated
	@Override
	public final void setServerData(@Nullable NBTTagCompound serverData) {
		this.serverData = serverData == null ? new NBTTagCompound() : serverData;
	}

	private PacketBuffer buffer() {
		if (buffer == null) {
			buffer = new PacketBuffer(Unpooled.buffer());
		}
		buffer.clear();
		return buffer;
	}

	@Override
	public <D> Optional<D> decodeFromNbt(DataCodec<D> codec, NBTBase tag) {
		try {
			PacketBuffer buffer = buffer();
			buffer.writeBytes(((NBTTagByteArray) tag).getByteArray());
			D decoded = codec.decode(buffer);
			return Optional.of(decoded);
		} catch (Exception e) {
			return Optional.empty();
		} finally {
			if (buffer != null) {
				buffer.clear();
			}
		}
	}

	@Override
	public <D> NBTBase encodeAsNbt(DataCodec<D> codec, D value) {
		PacketBuffer buffer = buffer();
		codec.encode(buffer, value);
		NBTTagByteArray tag = new NBTTagByteArray(ArrayUtils.subarray(buffer.array(), 0, buffer.readableBytes()));
		buffer.clear();
		return tag;
	}

	@Override
	public T getHitResult() {
		return hit.get();
	}

	/**
	 * Returns true if dedicated server has Jade installed.
	 */
	@Override
	public boolean isServerConnected() {
		return serverConnected;
	}

	@Override
	public boolean showDetails() {
		return showDetails;
	}

	@Override
	public abstract ItemStack getPickedResult();

	/**
	 * Marks this accessor as needing verification.
	 */
	public void requireVerification() {
		verify = true;
	}

	@Override
	public boolean shouldVerifyData() {
		return verify;
	}

	@Override
	public float tickRate() {
		return 20.0F;
	}

	@Override
	public ItemStack getServersideRep() {
		return serversideRep;
	}

	/**
	 * Sets the server-side representation shown for this accessor.
	 *
	 * @param serversideRep replacement item stack
	 */
	public void setServersideRep(ItemStack serversideRep) {
		this.serversideRep = serversideRep;
	}
}
