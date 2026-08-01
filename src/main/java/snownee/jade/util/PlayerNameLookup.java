package snownee.jade.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.management.PlayerProfileCache;
import com.mojang.authlib.GameProfile;

public class PlayerNameLookup {

	public static final String DUMMY_NAME = "???";

	private static final Set<UUID> FETCHING = ConcurrentHashMap.newKeySet();
	private static final ConcurrentHashMap<UUID, String> FETCHED = new ConcurrentHashMap<>();

	public static boolean isFetching(UUID uuid) {
		return FETCHING.contains(uuid);
	}

	public static boolean isFetched(UUID uuid) {
		return FETCHED.containsKey(uuid);
	}

	@Nullable
	public static String get(@Nullable UUID uuid, PlayerProfileCache profileCache) {
		if (uuid == null) {
			return null;
		}
		if (FETCHED.containsKey(uuid)) {
			return FETCHED.get(uuid);
		}
		GameProfile profile = profileCache.getProfileByUUID(uuid);
		if (profile != null && profile.getName() != null) {
			FETCHED.put(uuid, profile.getName());
			return profile.getName();
		}
		if (!FETCHING.add(uuid)) {
			return null;
		}
		CompletableFuture.runAsync(
				() -> {
					// 1.12.2: no async profile resolver; just try the cache and mark as fetched
					GameProfile p = profileCache.getProfileByUUID(uuid);
					if (p != null && p.getName() != null) {
						FETCHED.put(uuid, p.getName());
					} else {
						FETCHED.put(uuid, DUMMY_NAME);
					}
					FETCHING.remove(uuid);
				},
				command -> new Thread(command).start()
		);
		return null;
	}
}
