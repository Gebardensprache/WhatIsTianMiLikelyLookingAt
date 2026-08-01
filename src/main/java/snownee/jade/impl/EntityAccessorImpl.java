package snownee.jade.impl;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.google.common.base.Suppliers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.RayTraceResult;
import snownee.jade.Jade;
import snownee.jade.api.AccessorImpl;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.network.RequestEntityPacket;
import snownee.jade.network.ServerPayloadContext;
import snownee.jade.util.CommonProxy;
import snownee.jade.util.WailaExceptionHandler;

/**
 * Class to get information of entity target and context.
 */
public class EntityAccessorImpl extends AccessorImpl<RayTraceResult> implements EntityAccessor {

	private final Supplier<Entity> entity;

	public EntityAccessorImpl(Builder builder) {
		super(
				Objects.requireNonNull(builder.level),
				Objects.requireNonNull(builder.player),
				builder.serverData,
				Objects.requireNonNull(builder.hit),
				builder.connected,
				builder.showDetails);
		entity = Objects.requireNonNull(builder.entity);
	}

	public static void handleRequest(RequestEntityPacket message, ServerPayloadContext context, Consumer<NBTTagCompound> responseSender) {
		EntityPlayerMP player = context.player();
		context.execute(() -> {
			EntityAccessor accessor = message.data().unpack(player);
			if (accessor == null) {
				return;
			}

			Entity entity = accessor.getEntity();
			NBTTagCompound tag = accessor.getServerData();
			tag.setInteger("EntityId", entity.getEntityId());

			if (Jade.isOutOfReach(player, entity.getPosition(), player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue())) {
				responseSender.accept(tag);
				return;
			}
			List<IServerDataProvider<EntityAccessor>> providers = WailaCommonRegistration.instance().entityDataProvidersOf(entity);
			for (IServerDataProvider<EntityAccessor> provider : providers) {
				if (!message.dataProviders().contains(provider)) {
					continue;
				}
				try {
					provider.appendServerData(tag, accessor);
				} catch (Exception e) {
					WailaExceptionHandler.handleErr(e, provider, null);
				}
			}

			responseSender.accept(tag);
		});
	}

	@Override
	public Entity getEntity() {
		return CommonProxy.wrapPartEntityParent(getRawEntity());
	}

	@Override
	public Entity getRawEntity() {
		return entity.get();
	}

	@Override
	public ItemStack getPickedResult() {
		if (isServersideContent()) {
			return getServersideRep();
		}
		return CommonProxy.getEntityPickedResult(entity.get(), getPlayer(), getHitResult());
	}

	@Override
	public Object getTarget() {
		return getEntity();
	}

	@Override
	public boolean verifyData(NBTTagCompound data) {
		if (!verify) {
			return true;
		}
		return data.getInteger("EntityId") == getEntity().getEntityId();
	}

	public static class Builder implements EntityAccessor.Builder {

		public boolean showDetails;
		private @Nullable World level;
		private @Nullable EntityPlayer player;
		private @Nullable NBTTagCompound serverData;
		private boolean connected;
		private @Nullable Supplier<RayTraceResult> hit;
		private @Nullable Supplier<Entity> entity;
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
		public Builder hit(Supplier<RayTraceResult> hit) {
			this.hit = hit;
			return this;
		}

		@Override
		public Builder entity(Supplier<Entity> entity) {
			this.entity = entity;
			return this;
		}

		@Override
		public Builder from(EntityAccessor accessor) {
			level = accessor.getLevel();
			player = accessor.getPlayer();
			serverData = accessor.getServerData().copy();
			connected = accessor.isServerConnected();
			showDetails = accessor.showDetails();
			hit = accessor::getHitResult;
			entity = accessor::getEntity;
			verify = accessor.shouldVerifyData();
			return this;
		}

		@Override
		public EntityAccessor.Builder requireVerification(boolean verify) {
			this.verify = verify;
			return this;
		}

		@Override
		public EntityAccessor build() {
			EntityAccessorImpl accessor = new EntityAccessorImpl(this);
			if (verify) {
				accessor.requireVerification();
			}
			return accessor;
		}
	}

	public record SyncData(boolean showDetails, int id, int partIndex, Vec3d hitVec, NBTTagCompound data) {
		public static final DataCodec<SyncData> STREAM_CODEC = new DataCodec<>() {
			@Override
			public SyncData decode(PacketBuffer buf) {
				boolean showDetails = buf.readBoolean();
				int id = buf.readVarInt();
				int partIndex = buf.readVarInt();
				Vec3d hitVec = new Vec3d(buf.readFloat(), buf.readFloat(), buf.readFloat());
				NBTTagCompound data = DataCodec.readTag(buf);
				return new SyncData(showDetails, id, partIndex, hitVec, data);
			}

			@Override
			public void encode(PacketBuffer buf, SyncData value) {
				buf.writeBoolean(value.showDetails);
				buf.writeVarInt(value.id);
				buf.writeVarInt(value.partIndex);
				buf.writeFloat((float) value.hitVec.x);
				buf.writeFloat((float) value.hitVec.y);
				buf.writeFloat((float) value.hitVec.z);
				buf.writeCompoundTag(value.data);
			}
		};

		public SyncData(EntityAccessor accessor) {
			this(
					accessor.showDetails(),
					accessor.getEntity().getEntityId(),
					CommonProxy.getPartEntityIndex(accessor.getRawEntity()),
					accessor.getHitResult().hitVec,
					accessor.getServerData());
		}

		@Nullable
		public EntityAccessor unpack(EntityPlayerMP player) {
			Entity entity = CommonProxy.getPartEntity(player.getEntityWorld().getEntityByID(id), partIndex);
			if (entity == null) {
				return null;
			}
			return new EntityAccessorImpl.Builder()
					.level(player.getEntityWorld())
					.player(player)
					.showDetails(showDetails)
					.entity(() -> entity)
					.hit(Suppliers.memoize(() -> new RayTraceResult(entity, hitVec)))
					.serverData(data)
					.build();
		}
	}
}
