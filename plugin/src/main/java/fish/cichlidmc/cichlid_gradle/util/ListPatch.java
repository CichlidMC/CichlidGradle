package fish.cichlidmc.cichlid_gradle.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class ListPatch<T> {
	private final List<Predicate<T>> removals;
	private final List<T> values;

	public ListPatch() {
		this.removals = new ArrayList<>();
		this.values = new ArrayList<>();
	}

	public void add(T value) {
		this.values.add(value);
	}

	public void addAll(Collection<T> values) {
		this.values.addAll(values);
	}

	public void remove(T value) {
		this.removals.add(entry -> entry.equals(value));
	}

	public void removeAll(Predicate<T> test) {
		this.removals.add(test);
	}

	public List<T> apply(List<T> list) {
		List<T> copy = new ArrayList<>(list);
		// apply removals first
		this.removals.forEach(copy::removeIf);
		// add new entries
		copy.addAll(this.values);
		return copy;
	}
}
