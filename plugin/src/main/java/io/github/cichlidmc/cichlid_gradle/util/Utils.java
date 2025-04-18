package io.github.cichlidmc.cichlid_gradle.util;

import java.util.function.Supplier;

public final class Utils {
	public static <T> T make(Supplier<T> supplier) {
		return supplier.get();
	}
}
