package fish.cichlidmc.cichlid_gradle.extension.def;

import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;

public record DefHash(byte[] bytes, String shortString, String longString) {
	public static final int SHORT_LENGTH = 6;

	public static DefHash of(byte[] bytes) {
		return new DefHash(
				bytes,
				Encoding.BASE36.encode(bytes).substring(0, SHORT_LENGTH),
				Encoding.BASE_FUNNY.encode(bytes)
		);
	}
}
