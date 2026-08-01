package snownee.jade.impl;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Suppliers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.RayTraceResult;
import snownee.jade.api.AccessorImpl;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EmptyAccessor;

public class EmptyAccessorImpl extends AccessorImpl<RayTraceResult> implements EmptyAccessor {

	private EmptyAccessorImpl(Builder builder) {
		super(
				Objects.requireNonNull(builder.level),
				Objects.requireNonNull(builder.player),
				builder.serverData,
				Suppliers.ofInstance(Objects.requireNonNull(builder.hit)),
				builder.connected,
				builder.showDetails);
	}

	@Nullable
	@Override
	public Object getTarget() {
		return null;
	}

	@Override
	public boolean verifyData(NBTTagCompound data) {
		return true;
	}

	@Override
	public ItemStack getPickedResult() {
		return ItemStack.EMPTY;
	}

	public static class Builder implements EmptyAccessor.Builder {

		private @Nullable World level;
		private @Nullable EntityPlayer player;
		private @Nullable NBTTagCompound serverData;
		private boolean connected;
		private boolean showDetails;
		private @Nullable RayTraceResult hit;
		private boolean verify;

		@Override
		public Builder level(World level) {
			this.level = level;
			return this;
		}

		@Override
		public Builder player(EntityPlayer player) {
			this.player = player;
			return this;
		}

		@Override
		public Builder serverData(@Nullable NBTTagCompound serverData) {
			this.serverData = serverData;
			return this;
		}

		@Override
		public Builder serverConnected(boolean connected) {
			this.connected = connected;
			return this;
		}

		@Override
		public Builder showDetails(boolean showDetails) {
			this.showDetails = showDetails;
			return this;
		}

		@Override
		public Builder hit(RayTraceResult hit) {
			this.hit = hit;
			return this;
		}

		@Override
		public Builder from(EmptyAccessor accessor) {
			level = accessor.getLevel();
			player = accessor.getPlayer();
			serverData = accessor.getServerData().copy();
			connected = accessor.isServerConnected();
			showDetails = accessor.showDetails();
			hit = accessor.getHitResult();
			verify = accessor.shouldVerifyData();
			return this;
		}

		@Override
		public Builder requireVerification(boolean verify) {
			this.verify = verify;
			return this;
		}

		@Override
		public EmptyAccessor build() {
			EmptyAccessorImpl accessor = new EmptyAccessorImpl(this);
			if (verify) {
				accessor.requireVerification();
			}
			return accessor;
		}
	}

	public record SyncData(boolean showDetails, RayTraceResult hit, NBTTagCompound data) {
		public static final DataCodec<SyncData> STREAM_CODEC = new DataCodec<>() {
			@Override
			public SyncData decode(PacketBuffer buf) {
				boolean showDetails = buf.readBoolean();
				RayTraceResult hit = BlockAccessorImpl.SyncData.readBlockHitResult(buf);
				NBTTagCompound data = DataCodec.readTag(buf);
				return new SyncData(showDetails, hit, data);
			}

			@Override
			public void encode(PacketBuffer buf, SyncData value) {
				buf.writeBoolean(value.showDetails);
				BlockAccessorImpl.SyncData.writeBlockHitResult(buf, value.hit);
				buf.writeCompoundTag(value.data);
			}
		};

		public SyncData(EmptyAccessor accessor) {
			this(accessor.showDetails(), accessor.getHitResult(), accessor.getServerData());
		}

		public EmptyAccessor unpack(EntityPlayerMP player) {
			return new Builder()
					.level(player.getEntityWorld())
					.player(player)
					.showDetails(showDetails)
					.hit(hit)
					.serverData(data)
					.build();
		}
	}
}
