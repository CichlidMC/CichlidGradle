package io.github.cichlidmc.cichlid_gradle.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public record Hashes(String sha1, String sha256, String sha512, String md5) {
	public static Hashes of(Path file) throws IOException {
		MessageDigest sha1 = createDigest("SHA-1");
		MessageDigest sha256 = createDigest("SHA-256");
		MessageDigest sha512 = createDigest("SHA-512");
		MessageDigest md5 = createDigest("MD5");

		byte[] data = Files.readAllBytes(file);

		return new Hashes(
				format(sha1.digest(data)), format(sha256.digest(data)),
				format(sha512.digest(data)), format(md5.digest(data))
		);
	}

	private static MessageDigest createDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private static String format(byte[] bytes) {
		Formatter formatter = new Formatter();
		for (byte b : bytes) {
			formatter.format("%02x", b);
		}
		return formatter.toString();
	}
}
