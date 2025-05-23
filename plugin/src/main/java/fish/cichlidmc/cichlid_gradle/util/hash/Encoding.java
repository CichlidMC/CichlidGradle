package fish.cichlidmc.cichlid_gradle.util.hash;

import java.math.BigInteger;
import java.util.Formatter;

@FunctionalInterface
public interface Encoding {
	Encoding HEX = bytes -> {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	};

	Encoding BASE36 = bytes -> {
		String hex = HEX.encode(bytes);
		return new BigInteger(hex, 16).toString(36);
	};

	Encoding BASE58 = Base58Encoding::encode;
	Encoding BASE_FUNNY = BaseFunnyEncoding::encode;

	String encode(byte[] bytes);
}
