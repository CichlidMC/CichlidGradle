package fish.cichlidmc.cichlid_gradle.util;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

public final class Utils {
	public static <T> T make(Supplier<T> supplier) {
		return supplier.get();
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(Field field, Object object) throws IllegalAccessException {
		return (T) field.get(object);
	}

	public static <T> T getOnly(Iterable<T> iterable) {
		Iterator<T> iterator = iterable.iterator();
		T value = iterator.next();
		if (iterator.hasNext()) {
			throw new IllegalStateException("Multiple values present: " + iterable);
		}

		return value;
	}

	public static String until(String s, char c) {
		int i = s.indexOf(c);
		return i == -1 ? s : s.substring(0, i);
	}

	/**
	 * Try to split a String on the last occurrence of the given character.
	 * @return null if the character is not present
	 */
	@Nullable
	public static Pair<String, String> splitLast(String s, char c) {
		int i = s.lastIndexOf(c);
		if (i == -1) {
			return null;
		}

		String first = s.substring(0, i);
		String second = s.substring(i);
		return new Pair<>(first, second);
	}

	public static <A, B, X extends Throwable> Collection<B> map(Collection<A> collection, ThrowingFunction<A, B, X> function) throws X {
		Collection<B> result = new ArrayList<>();
		for (A a : collection) {
			result.add(function.apply(a));
		}
		return result;
	}

	public static byte[] readClassLoaderResource(@Nullable ClassLoader loader, String path) {
		ClassLoader toUse = loader != null ? loader : Utils.class.getClassLoader();
		URL url = toUse.getResource(path);
		if (url == null)
			return null;

		try (InputStream stream = url.openStream()) {
			return stream.readAllBytes();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public interface ThrowingFunction<A, B, X extends Throwable> {
		B apply(A a) throws X;
	}
}
