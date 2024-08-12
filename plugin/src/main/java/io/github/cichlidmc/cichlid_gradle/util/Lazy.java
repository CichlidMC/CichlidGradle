package io.github.cichlidmc.cichlid_gradle.util;

import java.util.function.Supplier;

public class Lazy<T> implements Supplier<T> {
	private Supplier<T> wrapped;
	private T value;

	public Lazy(Supplier<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public T get() {
		if (this.wrapped != null) {
			this.value = this.wrapped.get();
			this.wrapped = null;
		}
		return this.value;
	}
}
