package fish.cichlidmc.cichlid_gradle.util.hash;

import fish.cichlidmc.cichlid_gradle.util.TransformingIterable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public record HashAlgorithm(String name) {
	public static final HashAlgorithm MD5 = new HashAlgorithm("MD5");
	public static final HashAlgorithm SHA1 = new HashAlgorithm("SHA-1");
	public static final HashAlgorithm SHA256 = new HashAlgorithm("SHA-256");
	public static final HashAlgorithm SHA512 = new HashAlgorithm("SHA-512");

	public HashAlgorithm(String name) {
		this.name = name;
		// make sure the algorithm exists
		this.digest();
	}

	public MessageDigest digest() {
		try {
			return MessageDigest.getInstance(this.name);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] hashFiles(Iterable<File> files) throws IOException {
		return this.hash(new TransformingIterable<>(files, File::toPath));
	}

	public byte[] hash(Iterable<Path> files) throws IOException {
		MessageDigest digest = this.digest();

		for (Path file : files) {
			try (DigestInputStream stream = new DigestInputStream(Files.newInputStream(file), digest)) {
				// let the implementation read bytes in whatever way is most optimal
				stream.transferTo(OutputStream.nullOutputStream());
			}
		}

		return digest.digest();
	}

	public byte[] hash(Path file) throws IOException {
		return this.hash(List.of(file));
	}

	public byte[] hash(byte[] bytes) {
		return this.digest().digest(bytes);
	}
}
