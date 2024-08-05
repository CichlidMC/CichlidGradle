package io.github.cichlidmc.cichlid_gradle.util;

import java.util.function.Predicate;

/**
 * A list that may inherit elements from a parent.
 * This relationship is live, changes to parents reflect in their children.
 * Evaluated lazily.
 */
public interface HierarchicalList<T> {
	void add(T value);
	void remove(Predicate<T> test);

	default void remove(T value) {
		this.remove(t -> t.equals(value));
	}

	default void addAll(Iterable<T> values) {
		values.forEach(this::add);
	}
}
