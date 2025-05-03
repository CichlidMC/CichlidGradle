package fish.cichlidmc.cichlid_gradle.util;

import java.lang.reflect.Field;
import java.util.function.Supplier;

public final class Utils {
	public static <T> T make(Supplier<T> supplier) {
		return supplier.get();
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Field field, Object object) throws IllegalAccessException {
		return (T) field.get(object);
	}
}
