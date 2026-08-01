package snownee.jade.impl.lookup;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Minimal 1.12.2 replacement for modern MC's {@code IdMapper}. Maps objects to
 * sequential integer IDs for network serialization.
 *
 * @param <T> mapped object type
 */
public class IdMapper<T> implements Iterable<T> {

	public static final int DEFAULT = -1;

	private final Map<T, Integer> toId = Maps.newIdentityHashMap();
	private final List<T> byId;
	private int nextId;

	public IdMapper(int initialCapacity) {
		byId = Lists.newArrayListWithExpectedSize(initialCapacity);
	}

	public void add(T object) {
		addMapping(object, nextId);
	}

	public void addMapping(T object, int id) {
		toId.put(object, id);
		while (byId.size() <= id) {
			byId.add(null);
		}
		byId.set(id, object);
		nextId = Math.max(nextId, id + 1);
	}

	public int getId(T object) {
		return toId.getOrDefault(object, DEFAULT);
	}

	public int getIdOrThrow(T object) {
		Integer id = toId.get(object);
		if (id == null) {
			throw new IllegalArgumentException("Object not in IdMapper: " + object);
		}
		return id;
	}

	public @Nullable T byId(int id) {
		return id >= 0 && id < byId.size() ? byId.get(id) : null;
	}

	@Override
	public Iterator<T> iterator() {
		return byId.stream().filter(java.util.Objects::nonNull).iterator();
	}
}
