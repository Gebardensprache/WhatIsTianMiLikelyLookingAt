package snownee.jade.api.callback;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Collections2;

/**
 * Priority-ordered callback storage.
 *
 * @param <T> callback type
 */
public class CallbackContainer<T> {
	/**
	 * 1.12.2: modern uses fastutil's {@code IntReferencePair} /
	 * {@code IntReferenceImmutablePair} here; the 1.12.2-bundled fastutil (7.1.0)
	 * does not ship those 8.2+ types, so an equivalent priority-carrying record is
	 * used instead.
	 */
	private record CallbackEntry<T>(int priority, T callback) {
	}

	private final SortedSet<CallbackEntry<T>> callbacks = new TreeSet<>(
			Comparator.<CallbackEntry<T>>comparingInt(CallbackEntry::priority)
					.thenComparingInt(p -> System.identityHashCode(p.callback())));
	private final LoadingCache<Boolean, Collection<T>> callbacksView = CacheBuilder.newBuilder().build(new CacheLoader<>() {
		@Override
		public Collection<T> load(Boolean key) {
			return Collections2.transform(callbacks, CallbackEntry::callback);
		}
	});

	/**
	 * Adds a callback with default priority.
	 *
	 * @param callback callback to add
	 */
	public void add(T callback) {
		add(0, callback);
	}

	/**
	 * Adds a callback with the given priority.
	 *
	 * @param priority callback priority
	 * @param callback callback to add
	 */
	public void add(int priority, T callback) {
		Objects.requireNonNull(callback);
		callbacks.add(new CallbackEntry<>(priority, callback));
		callbacksView.invalidateAll();
	}

	/**
	 * Returns the current callback collection.
	 *
	 * @return callbacks in priority order
	 */
	public Collection<T> callbacks() {
		return callbacksView.getUnchecked(Boolean.TRUE);
	}

	/**
	 * Invokes every callback in priority order.
	 *
	 * @param consumer consumer to run
	 */
	public void call(Consumer<T> consumer) {
		for (T callback : callbacks()) {
			consumer.accept(callback);
		}
	}
}
