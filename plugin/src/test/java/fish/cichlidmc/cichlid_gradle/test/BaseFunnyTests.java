package fish.cichlidmc.cichlid_gradle.test;

import fish.cichlidmc.cichlid_gradle.util.hash.Base58Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.BaseFunnyEncoding;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class BaseFunnyTests {
	public static final String BASE58_ALPHABET = new String(Base58Encoding.DEFAULT_ALPHABET);
	public static final String FUNNY_ALPHABET = new String(BaseFunnyEncoding.ALPHABET);

	// https://appdevtools.com/base58-encoder-decoder
	private static final List<TestEntry> tests = List.of(
			new TestEntry("testtesttest", "3CQ2BAJ4H2ijzrj2T"),
			new TestEntry("aaaaaaaaaaaaaaaaaa", "53GMrrqVi3jzo9T3zLMBr8zsa"),
			new TestEntry("asdf", "3VUZMf")
	);

	@Test
	public void run() {
		for (TestEntry test : tests) {
			Assertions.assertEquals(test.base58, Encoding.BASE58.encode(test.data));
			Assertions.assertEquals(translate(test.base58), Encoding.BASE_FUNNY.encode(test.data));
		}
	}

	private static String translate(String base58) {
		char[] output = new char[base58.length()];
		for (int i = 0; i < base58.length(); i++) {
			char c = base58.charAt(i);
			int index = BASE58_ALPHABET.indexOf(c);
			output[i] = FUNNY_ALPHABET.charAt(index);
		}
		return new String(output);
	}

	private record TestEntry(byte[] data, String base58) {
		private TestEntry(String data, String base58) {
			this(data.getBytes(StandardCharsets.UTF_8), base58);
		}
	}
}
