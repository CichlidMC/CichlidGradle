package io.github.cichlidmc.cichlid_gradle.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.gradle.api.provider.Property;

public class HierarchicalListImpl<T> implements HierarchicalList<T> {
	private final List<Predicate<T>> removals = new ArrayList<>();
	private final List<T> values = new ArrayList<>();

	private HierarchicalListImpl<T> parent;

	@Override
	public void add(T value) {
		this.values.add(value);
	}

	@Override
	public void remove(Predicate<T> test) {
		this.values.removeIf(test);
		this.removals.add(test);
	}

	public void setParent(HierarchicalListImpl<T> parent) {
		this.parent = parent;
	}

	public List<T> resolve() {
		Set<HierarchicalListImpl<T>> visited = new HashSet<>();
		visited.add(this);
		List<T> list = new ArrayList<>();
		this.resolve(list, visited);
		return list;
	}

	private void resolve(List<T> list, Set<HierarchicalListImpl<T>> visited) {
		if (this.parent != null) {
			if (!visited.add(parent)) {
				// duplicate
				throw new IllegalStateException("Dependency chain in lists");
			}
			this.parent.resolve(list, visited);
		}
		this.removals.forEach(list::removeIf);
		list.addAll(this.values);
	}

	public static <T> List<T> resolve(Property<HierarchicalList<T>> property) {
		return ((HierarchicalListImpl<T>) property.get()).resolve();
	}

	public static <T> void setParent(Property<HierarchicalList<T>> child, Property<HierarchicalList<T>> parent) {
		((HierarchicalListImpl<T>) child.get()).setParent(((HierarchicalListImpl<T>) parent.get()));
	}
}
