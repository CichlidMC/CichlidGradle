package fish.cichlidmc.cichlid_gradle.util.hash;

/**
 * This is a custom encoding that is case-insensitive and filesystem-safe.
 * <p>
 * This might already be a thing with a name, but I couldn't find anything.
 * <p>
 * It just happens to conveniently use 58 characters, and base58 already exists,
 * so this class just defers to {@link Base58Encoding} with the custom alphabet.
 */
public class BaseFunnyEncoding {
	public static final char[] ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz`~!@#$%^&()-_=+[]{};',".toCharArray();

	static String encode(byte[] bytes) {
		return Base58Encoding.encode(bytes, ALPHABET);
	}
}
