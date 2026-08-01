package snownee.jade.addon.vanilla;

import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import snownee.jade.api.DataCodec;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.util.ClientProxy;

public class AnimalOwnerProvider implements StreamServerDataProvider<EntityAccessor, String> {
	public static final AnimalOwnerProvider INSTANCE = new AnimalOwnerProvider();

	@Override
	@Nullable
	public String streamData(EntityAccessor accessor) {
		WorldServer level = (WorldServer) accessor.getLevel();
		UUID uuid = getOwnerUUID(accessor.getEntity());
		Entity entity = uuid == null ? null : level.getEntityFromUuid(uuid);
		if (entity != null) {
			// 1.12.2: owner display-name components are sent as plain text.
			return entity.getDisplayName().getFormattedText();
		}
		PlayerProfileCache cache = level.getMinecraftServer().getPlayerProfileCache();
		GameProfile profile = uuid == null ? null : cache.getProfileByUUID(uuid);
		return profile == null ? null : profile.getName();
	}

	@Override
	public DataCodec<String> streamCodec() {
		return new DataCodec<>() {
			@Override
			public String decode(PacketBuffer buf) {
				return buf.readString(32767);
			}

			@Override
			public void encode(PacketBuffer buf, String value) {
				buf.writeString(value);
			}
		};
	}

	@Nullable
	public static UUID getOwnerUUID(Entity entity) {
		return entity instanceof EntityTameable ? ((EntityTameable) entity).getOwnerId() : null;
	}

	@Override
	public boolean shouldRequestData(EntityAccessor accessor) {
		Entity entity = accessor.getEntity();
		if (!(entity instanceof EntityTameable)) {
			return false;
		}
		UUID ownerUUID = getOwnerUUID(entity);
		return ownerUUID == null || ClientProxy.shouldFetchFromServer(ownerUUID);
	}

	@Override
	public ResourceLocation getUid() {
		return JadeIds.MC_ANIMAL_OWNER;
	}

	public static class Client implements IEntityComponentProvider {
		public static final Client INSTANCE = new Client();

		@Override
		public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
			String name = AnimalOwnerProvider.INSTANCE.decodeFromData(accessor).orElse(null);
			if (name == null) {
				UUID uuid = getOwnerUUID(accessor.getEntity());
				name = ClientProxy.lookupPlayerName(uuid);
				if (name == null) {
					return;
				}
			}
			tooltip.add(new TextComponentTranslation("jade.owner", new TextComponentString(name)));
		}

		@Override
		public ResourceLocation getUid() {
			return JadeIds.MC_ANIMAL_OWNER;
		}
	}
}
